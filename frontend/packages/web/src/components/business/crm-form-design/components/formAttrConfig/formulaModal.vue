<template>
  <CrmModal
    v-model:show="visible"
    :title="t('crmFormDesign.formulaSetting')"
    :positive-text="t('common.save')"
    :maskClosable="false"
    footer
    :width="800"
    @confirm="saveCalculateFormula"
    @cancel="handleCancel"
  >
    <CrmFormulaEditor
      ref="crmFormulaEditorRef"
      :field-config="fieldConfig"
      :form-fields="formFields"
      :is-sub-table-field="isSubTableField"
    />
  </CrmModal>
</template>

<script setup lang="ts">
  import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import { FormCreateField } from '@/components/business/crm-form-create/types';
  import CrmFormulaEditor from '@/components/business/crm-formula-editor/index.vue';

  const { t } = useI18n();
  const visible = defineModel<boolean>('visible', { required: true });

  const props = defineProps<{
    fieldConfig: FormCreateField;
    formFields: FormCreateField[];
    isSubTableField?: boolean;
  }>();

  const emit = defineEmits<{
    (e: 'save', result?: string): void;
  }>();

  const crmFormulaEditorRef = ref<InstanceType<typeof CrmFormulaEditor>>();
  function resolveFieldId(e: FormCreateField) {
    const isSub = props.isSubTableField;
    if (e.resourceFieldId || !isSub) {
      return e.id;
    }
    return e.businessKey || e.id;
  }

  const calTagList = computed<FormCreateField[]>(() =>
    props.formFields
      .filter((e) => e.type === FieldTypeEnum.INPUT_NUMBER)
      .map((e) => {
        const fieldId = resolveFieldId(e);
        let id = fieldId;
        if (e.resourceFieldId) {
          [, id] = id.split('_ref_'); // 数据源显示字段 id 是拼接_ref_的
        }
        if (e.numberFormat === 'percent') {
          id = `(${id} / 100)`;
        }
        return {
          ...e,
          id,
        };
      })
  );

  const cursorRange = ref<Range | null>(null);
  const isEmpty = ref(true);
  const editor = ref<HTMLElement | null>(null);

  function handleInput() {
    const text = editor.value?.innerText.trim() ?? '';
    isEmpty.value = text.length === 0;
  }

  function handleFocus() {
    isEmpty.value = false;
  }

  function handleBlur() {
    handleInput();
  }

  function saveCursor() {
    const selection = window.getSelection();
    if (selection && selection.rangeCount > 0) {
      cursorRange.value = selection.getRangeAt(0);
    }
  }

  function placeCursorAtEnd(el: HTMLElement) {
    el.focus();
    const range = document.createRange();
    range.selectNodeContents(el);
    range.collapse(false);

    const sel = window.getSelection();
    sel?.removeAllRanges();
    sel?.addRange(range);

    cursorRange.value = range;
  }

  function insertField(item: { name: string; id: string }) {
    if (!editor.value) return;

    // 如果光标不存在 或 光标不在 editor 内 → 强制定位到最后
    if (!cursorRange.value || !editor.value.contains(cursorRange.value.startContainer)) {
      placeCursorAtEnd(editor.value);
    }

    handleFocus();

    // 创建包裹节点
    const wrapper = document.createElement('span');
    wrapper.className = 'formula-tag-wrapper';
    wrapper.contentEditable = 'false';
    wrapper.setAttribute('data-value', item.id);
    wrapper.style.display = 'inline-block';

    const tagApp = createApp({
      render() {
        return h(
          CrmTag,
          {
            type: 'primary',
            theme: 'light',
            size: 'small',
            class: 'mx-[4px] mb-[4px]',
            tooltipDisabled: true,
          },
          { default: () => item.name }
        );
      },
    });

    tagApp.mount(wrapper);

    // 插入标签
    const range = cursorRange.value;
    range?.insertNode(wrapper);

    // 插入零宽字符，确保光标正常移动
    const space = document.createTextNode('\u200B');
    wrapper.after(space);

    // 重新设定光标到标签后
    const newRange = document.createRange();
    newRange.setStart(space, 1);
    newRange.setEnd(space, 1);

    cursorRange.value = newRange;

    const sel = window.getSelection();
    sel?.removeAllRanges();
    sel?.addRange(newRange);
  }

  // 解析公式编辑器内容为 AST 结构
  function parseFormula(editorEl: HTMLElement): Array<any> {
    const result: any[] = [];

    editorEl.childNodes.forEach((node: any) => {
      // field 节点
      if (node.nodeType === 1 && node.classList.contains('formula-tag-wrapper')) {
        const el = node as HTMLElement;
        const value = el.dataset.value ?? el.getAttribute('data-value') ?? '';
        const name = el.textContent?.trim() ?? '';
        result.push({
          type: 'field',
          field: value,
          name: name.trim(),
        });
      }

      // 文本节点
      if (node.nodeType === 3) {
        const text = node.textContent;
        if (text.trim() !== '') {
          result.push({
            type: 'text',
            value: text,
          });
        }
      }
    });

    return result;
  }

  function astToFormulaString(ast: any[]): string {
    return ast
      .map((item) => {
        if (item.type === 'field') return `\${${item.field}}`;
        return item.value;
      })
      .join('');
  }

  // 将公式字符串解析为 AST 结构化公式
  function formulaStringToAst(str: string) {
    const ast = [];
    let currentIndex = 0;
    while (currentIndex < str.length) {
      if (str[currentIndex] === '$' && str[currentIndex + 1] === '{') {
        const endIndex = str.indexOf('}', currentIndex + 2);
        const key = str.slice(currentIndex + 2, endIndex);
        ast.push({ type: 'field', field: key });
        currentIndex = endIndex + 1;
      } else {
        let textEndIndex = currentIndex;
        while (textEndIndex < str.length && !(str[textEndIndex] === '$' && str[textEndIndex + 1] === '{'))
          textEndIndex++;
        ast.push({ type: 'text', value: str.slice(currentIndex, textEndIndex) });
        currentIndex = textEndIndex;
      }
    }
    return ast;
  }

  // 将 AST结构化公式 渲染回公式编辑器
  function renderFormulaToEditor(ast: Array<any>, editorEl: HTMLElement, fieldMap: Record<string, string>) {
    editorEl.innerHTML = '';
    ast.forEach((item) => {
      if (item.type === 'text') {
        editorEl.appendChild(document.createTextNode(item.value));
      } else if (item.type === 'field') {
        const wrapper = document.createElement('span');
        wrapper.className = 'formula-tag-wrapper';
        wrapper.contentEditable = 'false';
        wrapper.setAttribute('data-value', item.field);
        wrapper.style.display = 'inline-block';
        const tagApp = createApp({
          render() {
            return h(
              CrmTag,
              {
                type: 'primary',
                theme: 'light',
                size: 'small',
                class: 'mx-[4px] mb-[4px]',
              },
              {
                default: () => fieldMap[item.field] ?? item.field,
              }
            );
          },
        });

        tagApp.mount(wrapper);
        editorEl.appendChild(wrapper);
        // 添加零宽空格确保光标正确定位
        editorEl.appendChild(document.createTextNode('\u200B'));
      }
    });
  }

  function astToTooltipString(ast: any[], fieldMap: Record<string, string>): string {
    return ast
      .map((item) => {
        if (item.type === 'field') return fieldMap[item.field] ?? item.field;
        return item.value;
      })
      .join('');
  }

  function saveCalculateFormula() {
    const result = crmFormulaEditorRef.value?.getCalculateFormula();
    emit('save', result);
  }

  function handleCancel() {
    visible.value = false;
  }
</script>

<style scoped lang="less"></style>
