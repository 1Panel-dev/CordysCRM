<template>
  <n-scrollbar class="max-h-[60vh]">
    <div class="crm-form-design-formula-header">
      <div class="ph-prefix">{{ props.fieldConfig.name }} = </div>
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
        @input="handleEditorInput"
      >
      </div>
      <div class="diagnose-result mb-[16px] mt-[4px] flex flex-wrap items-center">
        <div v-for="(item, index) of formulaDiagnostics" class="flex flex-wrap items-center text-[var(--error-red)]">
          <template v-if="item.functionName"> {{ item.functionName }}： </template>
          <div class="mr-[8px]">{{ item.message }}</div>
          <template v-if="formulaDiagnostics.length > 1 && index !== formulaDiagnostics.length - 1"> ；</template>
        </div>
      </div>
      <div class="field-wrapper">
        <div class="field-item">
          <div class="field-item-title"> {{ t('common.formFields') }} </div>
          <CrmSearchInput
            v-model:value="keyword"
            class="field-search-input !w-[calc(100%+2px)]"
            :placeholder="t('crmFormDesign.formulaByNameSearchPlaceholder')"
          />
          <div class="field-item-content">
            <!-- <n-scrollbar class="max-h-[60%]"> -->
            <CrmCollapse
              v-for="ele of allFieldList"
              :default-expand="true"
              :name-key="ele.type"
              class="field-item-collapse"
            >
              <template #header>
                <div class="px-[4px] text-[14px] font-medium text-[var(--text-n2)]">
                  {{ t(ele.name) }}（{{ ele.children.length }}）
                </div>
              </template>
              <template v-if="ele.children.length > 0">
                <div
                  v-for="item of ele.children"
                  class="flex h-[32px] items-center justify-between rounded px-[4px] hover:bg-[var(--text-n9)]"
                  @mousedown.prevent="insertField(item)"
                >
                  <n-tooltip trigger="hover" :delay="300" placement="top-start">
                    <template #trigger>
                      <div class="one-line-text flex-1 cursor-pointer">
                        {{ item.name }}
                      </div>
                    </template>
                    {{ item.name }}
                  </n-tooltip>

                  <CrmTag :type="colorThemeMap[ele.type]?.type || 'default'" class="flex-shrink-0" theme="light">
                    {{ colorThemeMap[ele.type]?.label }}
                  </CrmTag>
                </div>
              </template>
              <div v-else class="px-[4px] text-[var(--text-n4)]">{{ t('common.noData') }}</div>
              <n-divider v-if="ele.children.length !== 0" class="!mb-0 !mt-[16px]" />
            </CrmCollapse>
            <!-- </n-scrollbar> -->
          </div>
        </div>
        <div class="field-item">
          <div class="field-item-title"> {{ t('crmFormDesign.formulaFunction') }} </div>
          <div class="field-item-content">
            <div v-for="fun of allFunctionSource" class="field-fun-item" @mousedown.prevent="insertField(fun)">
              <div :class="`${activeFun?.name === fun.name ? `text-[${FUN_COLOR}]` : 'text-[var(--text-n1)]'}`">
                {{ fun.name }}
              </div>
              <div class="text-[12px] text-[var(--text-n4)]">{{ fun.description }}</div>
            </div>
          </div>
        </div>
        <div class="field-item">
          <div class="field-item-title"> {{ t('crmFormDesign.formulaUsage') }} </div>
          <div class="field-item-content">
            <div>
              <div class="field-fun-item-name">{{ activeFun?.name }}</div>
              <div class="text-[12px] text-[var(--text-n4)]">{{ activeFun?.description }}</div>
            </div>
            <div v-if="activeFun">
              <div class="text-[var(--text-n1)]">{{ t('crmFormDesign.formulaUsageMethods') }}</div>
              <div v-if="activeFun?.name === 'DAYS'">
                <div class="flex items-center">
                  <div :class="`text-[${FUN_COLOR}]`">DAYS</div>(<div :class="`text-[${DATE_TIME_COLOR}]`">
                    {{ t('crmFormDesign.formulaExampleEndTime') }} </div
                  >, <div :class="`text-[${DATE_TIME_COLOR}]`"> {{ t('crmFormDesign.formulaExampleStartTime') }} </div>)
                </div>
              </div>
              <div v-if="activeFun?.name === 'SUM'">
                <div class="flex items-center">
                  <!-- TODO 文案不确定 -->
                  <div :class="`text-[${FUN_COLOR}]`">SUM</div>(<div :class="`text-[${INPUT_NUMBER_COLOR}]`"> 值1 </div
                  >, <div :class="`text-[${INPUT_NUMBER_COLOR}]`">值2</div>, ... )
                </div>
              </div>
            </div>
            <div v-else class="text-[12px] text-[var(--text-n4)]">
              {{ t('crmFormDesign.formulaSelectUsageTip') }}
            </div>
          </div>
        </div>
      </div>
      <div v-if="isEmpty" class="formula-placeholder flex flex-col items-start">
        <div class="text-[var(--text-n6)]">{{ t('crmFormDesign.formulaPlaceholder') }}</div>

        <div class="flex items-center">
          <div :class="`text-[${FUN_COLOR}]`">SUM</div>(<div :class="`text-[${ARRAY_COLOR}]`">订阅产品.金额</div>) +
          <div :class="`text-[${FUN_COLOR}]`">SUM</div>(<div :class="`text-[${ARRAY_COLOR}]`">授权产品.金额</div>) +
          <div :class="`text-[${FUN_COLOR}]`">SUM</div>(<div :class="`text-[${ARRAY_COLOR}]`">续费产品.金额</div>)
        </div>
      </div>
    </div>
  </n-scrollbar>
</template>

<script setup lang="ts">
  import { NButton, NDivider, NScrollbar, NTooltip } from 'naive-ui';

  import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmCollapse from '@/components/pure/crm-collapse/index.vue';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import { FormCreateField } from '@/components/business/crm-form-create/types';

  import { allFunctionSource, ARRAY_COLOR, DATE_TIME_COLOR, FUN_COLOR, INPUT_NUMBER_COLOR } from './config';
  import diagnoseFormula from './diagnose/diagnose';
  import parseTokensToAST from './parser';
  import { renderTokensToEditor } from './parseSource/renderTokens';
  import { serializeFormulaFromAst, tokenizeFromSource } from './parseSource/serializeFormulaFromAst';
  import tokenizeFromEditor from './tokenizer';
  import { FormulaDiagnostic } from './types';
  import { applyDiagnosticsHighlight, safeParseFormula } from './utils';

  const { t } = useI18n();

  export type FormulaFormCreateField = FormCreateField & {
    parentId?: string;
    parentName?: string;
    inSubTable?: boolean;
  };

  const props = defineProps<{
    fieldConfig: FormCreateField;
    formFields: FormCreateField[];
    isSubTableField?: boolean;
  }>();

  const emit = defineEmits<{
    (e: 'save', astVal: string): void;
  }>();

  const formulaDiagnostics = ref<FormulaDiagnostic[]>([]);
  const keyword = ref('');
  const activeFun = ref<FormCreateField | null>(null);

  const colorThemeMap: Record<string, any> = {
    [FieldTypeEnum.INPUT_NUMBER]: {
      type: 'info',
      label: t('crmFormDesign.inputNumber'),
    },
    [FieldTypeEnum.DATE_TIME]: {
      type: 'success',
      label: t('crmFormDesign.dateTime'),
    },
    ARRAY: {
      type: 'warning',
      label: t('common.array'),
    },
  };

  function resolveFieldId(e: FormCreateField) {
    const id = e.id.split('_').pop() ?? '';
    if (e.resourceFieldId) {
      return id;
    }
    return e.businessKey || id;
  }

  const allowFormulaType = [FieldTypeEnum.INPUT_NUMBER, FieldTypeEnum.DATE_TIME];

  function pushByType(field: FormCreateField, numberGroup: FormCreateField[], dateGroup: FormCreateField[]) {
    if (field.type === FieldTypeEnum.INPUT_NUMBER) {
      numberGroup.push({
        ...field,
        id: field.numberFormat === 'percent' ? `(${resolveFieldId(field)} / 100)` : resolveFieldId(field),
      });
    }

    if (field.type === FieldTypeEnum.DATE_TIME) {
      dateGroup.push({
        ...field,
        id: resolveFieldId(field),
      });
    }
  }

  function flatAllFields(fields: FormCreateField[]) {
    const result: (FormCreateField & { parentId?: string; parentName?: string; inSubTable?: boolean })[] = [];
    console.log(fields, 'fields');

    fields.forEach((field) => {
      if (field.subFields) {
        field.subFields.forEach((sub) => {
          result.push({
            ...sub,
            parentId: field.id,
            id: resolveFieldId(sub),
            parentName: field.name,
            inSubTable: true,
          });
        });
      } else {
        result.push({
          ...field,
          inSubTable: false,
        });
      }
    });

    return result;
  }

  function isSameSubTableNumberField(field: FormulaFormCreateField) {
    const currentField = flatAllFields(props.formFields).find((e) => e.id === props.fieldConfig.id);
    return field.inSubTable && field.parentId === currentField?.parentId && field.type === FieldTypeEnum.INPUT_NUMBER;
  }

  function classifyFields(allFields: FormulaFormCreateField[]) {
    const numberGroup: FormulaFormCreateField[] = [];
    const dateGroup: FormulaFormCreateField[] = [];
    const arrayGroup: FormulaFormCreateField[] = [];

    allFields.forEach((field) => {
      // 当前在子表内
      if (props.isSubTableField) {
        if (field.inSubTable && [FieldTypeEnum.INPUT_NUMBER].includes(field.type) && isSameSubTableNumberField(field)) {
          pushByType(field, numberGroup, dateGroup);
        }
        return;
      }

      // 不在子表内
      if (!field.inSubTable) {
        pushByType(field, numberGroup, dateGroup);
      }
      if (field.inSubTable && [...allowFormulaType, FieldTypeEnum.FORMULA].includes(field.type)) {
        // 生成路径字段
        arrayGroup.push({
          ...field,
          id: `${field.parentId}.${resolveFieldId(field)}`,
          name: `${field.parentName}.${field.name}`,
        });
      }
    });

    return { numberGroup, dateGroup, arrayGroup };
  }

  const allFieldListSource = computed(() => {
    const { numberGroup, dateGroup, arrayGroup } = classifyFields(flatAllFields(props.formFields));
    console.log({ numberGroup, dateGroup, arrayGroup }, 'xinxinwu 🏷');

    return [
      {
        name: t('crmFormDesign.inputNumber'),
        type: FieldTypeEnum.INPUT_NUMBER,
        children: numberGroup,
      },
      {
        name: t('crmFormDesign.dateTime'),
        type: FieldTypeEnum.DATE_TIME,
        children: dateGroup,
      },
      {
        name: t('common.array'),
        type: 'ARRAY',
        children: arrayGroup,
      },
    ];
  });

  const allFieldList = computed(() => {
    const key = keyword.value.trim();
    if (!key) {
      return allFieldListSource.value;
    }
    return allFieldListSource.value.map((group) => ({
      ...group,
      children: group.children.filter((child) => child.name?.includes(key)),
    }));
  });

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

  function ensureCursor(edt: HTMLElement) {
    if (!cursorRange.value || !edt.contains(cursorRange.value.startContainer)) {
      const range = document.createRange();
      range.selectNodeContents(edt);
      range.collapse(false);

      const sel = window.getSelection();
      sel?.removeAllRanges();
      sel?.addRange(range);

      cursorRange.value = range;
    }
  }

  function validateCurrentFormula() {
    if (!editor.value) return;

    const tokens = tokenizeFromEditor(editor.value);
    const ast = parseTokensToAST(tokens);

    formulaDiagnostics.value = diagnoseFormula(tokens, ast);
    // TODO 高亮先不做
    // applyDiagnosticsHighlight(editor.value, diagnostics);
  }

  type FormulaNodeMeta = {
    text: string;
    color?: string;
    isFunction: boolean;
  };

  function getFormulaNodeMeta(item: FormCreateField & { isFunction?: boolean }): FormulaNodeMeta {
    if (item.isFunction) {
      return {
        text: `${item.name}`,
        color: FUN_COLOR,
        isFunction: true,
      };
    }

    // —— 数组字段（二级路径）
    if (item.id?.includes('.')) {
      return {
        text: item.name,
        color: ARRAY_COLOR,
        isFunction: false,
      };
    }

    // —— 普通字段
    switch (item.type) {
      case FieldTypeEnum.INPUT_NUMBER:
        return {
          text: item.name,
          color: INPUT_NUMBER_COLOR,
          isFunction: false,
        };

      case FieldTypeEnum.DATE_TIME:
        return {
          text: item.name,
          color: DATE_TIME_COLOR,
          isFunction: false,
        };

      default:
        return {
          text: item.name,
          isFunction: false,
        };
    }
  }

  // 插入函数节点
  function insertFunction(item: FormCreateField) {
    if (!editor.value) return;

    ensureCursor(editor.value);
    handleFocus();
    const range = cursorRange.value!;
    range.deleteContents();

    /** 函数名 */
    const fnNode = document.createElement('span');
    fnNode.className = 'formula-fn';
    fnNode.style.color = FUN_COLOR;
    fnNode.contentEditable = 'false';
    fnNode.dataset.nodeType = 'function';
    fnNode.dataset.fnName = item.id;
    fnNode.textContent = item.name;

    /** 左括号 */
    const leftParen = document.createTextNode('(');

    /** 参数区（真正可编辑） */
    const argsNode = document.createElement('span');
    argsNode.className = 'formula-args';
    argsNode.appendChild(document.createTextNode('\u200B'));

    /** 右括号 */
    const rightParen = document.createTextNode(')');

    range.insertNode(rightParen);
    range.insertNode(argsNode);
    range.insertNode(leftParen);
    range.insertNode(fnNode);

    /** 光标放入参数区 */
    const caretRange = document.createRange();
    const text = argsNode.firstChild!;
    caretRange.setStart(text, 1);
    caretRange.setEnd(text, 1);

    const sel = window.getSelection();
    sel?.removeAllRanges();
    sel?.addRange(caretRange);

    cursorRange.value = caretRange;

    nextTick(() => {
      validateCurrentFormula();
    });
  }

  // 插入对应的字段
  function insertField(item: FormCreateField & { isFunction?: boolean }) {
    if (item.isFunction) {
      activeFun.value = item;
    }

    if (!editor.value) return;

    if (item.isFunction) {
      insertFunction(item);
      return;
    }

    if (!editor.value) return;
    ensureCursor(editor.value);
    handleFocus();

    const range = cursorRange.value!;
    range.deleteContents();

    const meta = getFormulaNodeMeta(item);

    const wrapper = document.createElement('span');
    if (meta.color) {
      wrapper.style.color = meta.color;
    }

    wrapper.className = 'formula-tag-wrapper';
    wrapper.contentEditable = 'false';
    wrapper.dataset.value = item.id;
    wrapper.dataset.nodeType = 'field';
    wrapper.dataset.fieldType = item.type;
    wrapper.textContent = item.name;

    range.insertNode(wrapper);

    const space = document.createTextNode('\u200B');
    wrapper.after(space);

    const newRange = document.createRange();
    newRange.setStart(space, 1);
    newRange.setEnd(space, 1);

    const sel = window.getSelection();
    sel?.removeAllRanges();
    sel?.addRange(newRange);

    cursorRange.value = newRange;

    nextTick(() => {
      validateCurrentFormula();
    });
  }

  function handleEditorInput() {
    validateCurrentFormula();
  }

  function getCalculateFormula(): string | undefined {
    if (!editor.value) return;
    const tokens = tokenizeFromEditor(editor.value);
    const ast = parseTokensToAST(tokens);

    const fieldMap: Record<string, string> = {};
    flatAllFields(props.formFields).forEach((item) => {
      fieldMap[item.id] = item.name;
    });

    const saveResult = serializeFormulaFromAst(ast, fieldMap);
    const result = JSON.stringify(saveResult);
    return result;
  }

  function handleClearFormulaField() {
    if (!editor.value) return;
    editor.value.innerHTML = '';
    ensureCursor(editor.value);
    isEmpty.value = true;
    formulaDiagnostics.value = [];
  }

  function initFormula() {
    nextTick(async () => {
      if (editor.value) {
        const { source, fields } = safeParseFormula(props.fieldConfig.formula ?? '');
        const fieldMap: Record<string, FormulaFormCreateField> = {};
        const { numberGroup, dateGroup, arrayGroup } = classifyFields(flatAllFields(props.formFields));
        [...numberGroup, ...dateGroup, ...arrayGroup].forEach((item) => {
          if (fields.includes(item.id)) {
            fieldMap[item.id] = item;
          }
        });
        const tokens = tokenizeFromSource(source, fieldMap);

        renderTokensToEditor(editor.value, tokens);
        await nextTick();
        validateCurrentFormula();
        if (source) {
          isEmpty.value = false;
        }
      }
    });
  }

  watch(
    () => props.fieldConfig.formula,
    () => {
      initFormula();
    }
  );

  onMounted(() => {
    initFormula();
  });

  defineExpose({
    getCalculateFormula,
    handleClearFormulaField,
    editor,
  });
</script>

<style scoped lang="less">
  .crm-form-design-formula-wrapper {
    position: relative;
    display: flex;
    min-height: 40px;
    border-radius: 6px;
    color: var(--text-n1);
    @apply flex flex-col;
    .crm-form-design-formula-editor {
      padding: 16px 16px 8px;
      min-height: 100px;
      max-height: 800px;
      border: 0.5px solid var(--text-n7);
      border-radius: 0 0 4px 4px;
      outline: none;
      flex: 1;
      gap: 8px;
      .crm-scroll-bar();
      @apply: h-full w-full overflow-y-auto flex items-center;
      &:focus {
        border-color: var(--primary-8);
        outline: none;
      }
    }
    .formula-placeholder {
      position: absolute;
      top: 0;
      left: 0;
      padding: 16px 16px 0;
      color: var(--text-n1);
      gap: 8px;
      pointer-events: none; /* 不阻止输入 */
      @apply flex;
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
  .field-wrapper {
    @apply flex;

    min-width: 250px;
    border: 1px solid var(--text-n7);
    border-radius: 4px;
    background: var(--text-n9);
    .field-item {
      @apply flex w-1/3  flex-1  flex-col;

      border-radius: 4px;
      .field-item-title {
        padding: 4px 8px;
        @apply font-semibold;

        border-right: 1px solid var(--text-n7);
        color: var(--text-n1);
      }
      &:nth-of-type(2) .field-item-title {
        border-bottom: 1px solid var(--text-n7);
      }
      &:last-child .field-item-title {
        border-right: none;
        border-bottom: 1px solid var(--text-n7);
      }
      .field-item-content {
        padding: 12px;
        @apply flex flex-1 flex-col;

        border-right: 1px solid var(--text-n7);
        background: var(--text-n10);
        .field-fun-item {
          padding: 0 4px;
          @apply cursor-pointer rounded;
          &:hover {
            background: var(--text-n9);
          }
          &.active {
            background: var(--text-n9);
          }
        }
      }
      &:last-child .field-item-content {
        border-right: none;
        background: none;
      }
    }
  }
  .field-item-collapse {
    margin-bottom: 16px;
    padding: 0;
    .n-collapse-item {
      .n-collapse-item__content-wrapper {
        .n-collapse-item__content-inner {
          padding-top: 8px !important;
        }
      }
    }
  }
  :deep(.n-collapse-item__content-inner) {
    padding-top: 0 !important;
  }
  .field-search-input {
    margin-left: -1px;
    max-width: calc(100% + 1px);
    .n-input-wrapper {
      width: calc(100% + 1px);
    }

    --n-border-radius: 0 !important;
  }
  .formula-args {
    min-width: 4px;
  }
  .formula-fn::selection,
  .formula-args::selection {
    background: rgb(64 158 255 / 20%);
  }
</style>
