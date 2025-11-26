<template>
  <CrmModal
    v-model:show="visible"
    :title="t('crmFormDesign.formulaSetting')"
    :positive-text="t('common.confirm')"
    :maskClosable="false"
    footer
    @confirm="saveCalculateFormula"
    @cancel="handleCancel"
  >
    <n-scrollbar class="max-h-[60vh]">
      <div class="crm-form-design-formula-header">
        <div class="ph-prefix">{{ t('crmFormDesign.formulaEquals') }} </div>
        <n-button type="primary" text @click="handleClearFormulaField">
          {{ t('common.clear') }}
        </n-button>
      </div>
      <div class="crm-form-design-formula-wrapper">
        <div
          ref="editor"
          class="crm-form-design-formula-editor"
          contenteditable="true"
          @click="saveCursor"
          @keyup="saveCursor"
          @focus="handleFocus"
          @blur="handleBlur"
        >
        </div>
        <div class="font-medium text-[var(--text-n1)]">{{ t('crmFormDesign.currentForm') }}</div>
        <div class="flex flex-wrap items-center gap-[8px]">
          <CrmTag
            v-for="item in calTagList"
            type="primary"
            class="cursor-pointer"
            theme="light"
            @mousedown.prevent="insertField(item)"
          >
            {{ item.name }}
          </CrmTag>
        </div>
        <div v-if="isEmpty" class="formula-placeholder">
          <div class="text-[var(--text-n6)]">{{ t('crmFormDesign.formulaPlaceholder') }}</div>
          <CrmTag size="small" tooltipDisabled> {{ t('crmFormDesign.formulaPricing') }} </CrmTag>
          <span class="text-[var(--text-n6)]">*</span>
          <CrmTag size="small" tooltipDisabled> {{ t('crmFormDesign.formulaDiscount') }} </CrmTag>
        </div>
      </div>
    </n-scrollbar>
  </CrmModal>
</template>

<script setup lang="ts">
  import { NButton, NScrollbar } from 'naive-ui';

  import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import { FormCreateField } from '@/components/business/crm-form-create/types';

  const { t } = useI18n();
  const visible = defineModel<boolean>('visible', { required: true });

  const props = defineProps<{
    fieldConfig: FormCreateField;
    formFields: FormCreateField[];
  }>();

  const emit = defineEmits<{
    (e: 'save', astVal: string): void;
  }>();

  const calTagList = computed<FormCreateField[]>(() =>
    props.formFields.filter((e) => e.type === FieldTypeEnum.INPUT_NUMBER)
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

  function saveCalculateFormula() {
    if (!editor.value) return;
    const ast = parseFormula(editor.value);
    const astValue: string = astToFormulaString(ast);
    emit('save', astValue);
  }

  function handleCancel() {
    visible.value = false;
  }

  function handleClearFormulaField() {
    if (!editor.value) return;
    editor.value.innerHTML = '';
    placeCursorAtEnd(editor.value);
    isEmpty.value = true;
  }

  watch(
    () => visible.value,
    (val) => {
      if (val && props.fieldConfig.formula) {
        nextTick(() => {
          if (editor.value) {
            const ast = formulaStringToAst(props.fieldConfig.formula ?? '');
            if (props.fieldConfig.formula) {
              isEmpty.value = false;
            }
            const fieldMap: Record<string, string> = {};
            calTagList.value.forEach((item) => {
              fieldMap[item.id] = item.name;
            });
            renderFormulaToEditor(ast, editor.value, fieldMap);
          }
        });
      }
    }
  );
</script>

<style scoped lang="less">
  .crm-form-design-formula-wrapper {
    position: relative;
    display: flex;
    min-height: 40px;
    border-radius: 6px;
    color: var(--text-n1);
    gap: 16px;
    @apply flex flex-col;
    .crm-form-design-formula-editor {
      padding: 16px;
      min-height: 72px;
      border: 0.5px solid var(--text-n7);
      border-radius: 0 0 4px 4px;
      outline: none;
      flex: 1;
      gap: 8px;
      @apply: flex items-center;
      &:focus {
        border-color: var(--primary-8);
        outline: none;
      }
    }
    .formula-placeholder {
      position: absolute;
      top: 0;
      left: 0;
      display: flex;
      align-items: center;
      padding: 16px 16px 0;
      color: var(--text-n1);
      gap: 8px;
      pointer-events: none; /* 不阻止输入 */
      @apply flex items-center;
    }
  }
  .crm-form-design-formula-header {
    padding: 0 16px;
    height: 32px;
    line-height: 32px;
    border: 0.5px solid var(--text-n7);
    border-bottom: none;
    border-radius: 4px 4px 0 0;
    background: var(--text-n9);
    @apply flex items-center justify-between;
    .ph-prefix {
      margin-right: 4px;
      font-weight: 600;
      color: var(--text-n1);
    }
  }
</style>
