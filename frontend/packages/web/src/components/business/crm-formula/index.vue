<template>
  <n-tooltip trigger="hover" placement="top">
    <template #trigger>
      <component
        :is="currentComponent"
        v-model:value="value"
        :path="props.path"
        :field-config="fieldConfig"
        :form-config="props.formConfig"
        :is-sub-table-field="props.isSubTableField"
        :is-sub-table-render="props.isSubTableRender"
        :need-init-detail="needInitDetail"
        @change="handleChange"
      />
    </template>
    {{ formulaTooltip }}
  </n-tooltip>
</template>

<script setup lang="ts">
  import { NTooltip } from 'naive-ui';
  import { debounce } from 'lodash-es';

  import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { FormConfig } from '@lib/shared/models/system/module';

  import basicComponents from '@/components/business/crm-form-create/components/basic/index';

  import { safeParseFormula } from '../crm-formula-editor/utils';
  import evaluateIR from './formula-runtime';
  import { FieldTypeMap, ValueType } from './formula-runtime/types';
  import { getFormulaDataSourceDisplayValue, hydrateIRNumberType } from './utils';
  import type { FormCreateField } from '@cordys/web/src/components/business/crm-form-create/types';

  const { t } = useI18n();
  const { inputNumber, singleText } = basicComponents;

  const props = defineProps<{
    fieldConfig: FormCreateField;
    formConfig?: FormConfig;
    path: string;
    formDetail?: Record<string, any>;
    needInitDetail?: boolean; // 判断是否编辑情况
    isSubTableField?: boolean; // 是否是子表字段
    isSubTableRender?: boolean; // 是否是子表渲染
  }>();

  const emit = defineEmits<{
    (e: 'change', value: any): void;
  }>();

  const value = defineModel<any>('value', {
    default: 0,
  });

  const currentComponent = computed(() => {
    switch (typeof value.value) {
      case 'string':
        return singleText;
      case 'number':
        return inputNumber;
      default:
        return inputNumber;
    }
  });

  const formulaFormContext = inject(
    'formFieldsProvider',
    ref({
      fields: [],
      formulaDataSource: {},
      evaluationNow: null,
    })
  );

  const fieldList = computed(() => formulaFormContext?.value.fields);
  const formulaDataSource = computed(() => formulaFormContext?.value.formulaDataSource);
  const evaluationNow = computed(() => formulaFormContext?.value.evaluationNow);

  function resolveFieldId(e: FormCreateField, inSubTable?: boolean) {
    if (e.resourceFieldId) {
      return e.id;
    }

    return inSubTable ? e.businessKey || e.id : e.id;
  }

  function flatAllFields(fields: FormCreateField[]) {
    const result: (FormCreateField & { parentId?: string; parentName?: string; inSubTable?: boolean })[] = [];
    fields?.forEach((field) => {
      if (field.subFields) {
        field.subFields.forEach((sub) => {
          result.push({
            ...sub,
            name: `${field.name}.${sub.name}`,
            parentId: field.id,
            id: props.isSubTableRender ? resolveFieldId(sub, true) : `${field.id}.${resolveFieldId(sub, true)}`,
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

  const allFieldIds = computed(() => {
    const fields = fieldList.value ?? [];
    return flatAllFields(fields).map((e) => e.id);
  });
  const isFormulaInvalid = computed(() => {
    const { fields } = safeParseFormula(props.fieldConfig.formula ?? '');
    const savedFields = fields?.map((e: any) => e.fieldId);

    return savedFields.some((fieldId: string) => !allFieldIds.value.includes(fieldId));
  });

  const formulaTooltip = computed(() => {
    const { display } = safeParseFormula(props.fieldConfig.formula ?? '');
    if (display) {
      return isFormulaInvalid.value ? t('crmFormDesign.formulaFieldChanged') : display;
    }
    return t('crmFormDesign.formulaTooltip');
  });

  function normalizeFormulaResult(
    result: any,
    options?: {
      decimalPlaces?: number;
      expectedType?: ValueType;
    }
  ): any {
    const decimalPlaces = options?.decimalPlaces ?? 2;
    const expectedType = options?.expectedType;

    if (result == null) {
      return '';
    }

    // 根据 expectedType 决定行为
    if (expectedType === 'string') {
      return String(result);
    }

    if (expectedType === 'number') {
      const num = Number(result);
      if (Number.isNaN(num)) return 0;
      return Number(num.toFixed(decimalPlaces));
    }

    // 自动推断模式（当前默认行为）
    switch (typeof result) {
      case 'number':
        return Number(result.toFixed(decimalPlaces));

      case 'string':
        return result;

      case 'boolean':
        return result ? 'TRUE' : 'FALSE';

      default:
        return String(result);
    }
  }

  function getScalarFieldValue(
    fieldId: string,
    context?: {
      tableKey?: string;
      rowIndex?: number;
    }
  ) {
    // 子表公式：只取当前行
    if (context?.tableKey && context.rowIndex != null) {
      const row = props.formDetail?.[context.tableKey]?.[context.rowIndex];
      return row?.[fieldId];
    }

    // 主表字段
    return props.formDetail?.[fieldId];
  }

  function getTableColumnValues(path: string): any[] {
    const [tableKey, fieldKey] = path.split('.');
    const rows = props.formDetail?.[tableKey];
    if (!Array.isArray(rows)) return [];
    return rows.map((row) => row?.[fieldKey]);
  }

  function getValueType(field: FormCreateField): ValueType {
    switch (field.type) {
      case FieldTypeEnum.INPUT_NUMBER:
        return 'number';
      case FieldTypeEnum.DATE_TIME:
        return 'date';
      case FieldTypeEnum.INPUT:
      case FieldTypeEnum.DATA_SOURCE:
      case FieldTypeEnum.DATA_SOURCE_MULTIPLE:
        return 'string';
      // todo 预留
      case FieldTypeEnum.RADIO:
      case FieldTypeEnum.CHECKBOX:
        return 'boolean';
      default:
        return 'unknown';
    }
  }

  function buildFieldTypeMap(fields: FormCreateField[]) {
    const map: FieldTypeMap = {};

    flatAllFields(fields).forEach((field) => {
      map[field.id] = {
        valueType: getValueType(field),
        ...(field.type === FieldTypeEnum.INPUT_NUMBER
          ? {
              numberType: field.numberFormat === 'percent' ? 'percent' : 'number', // 仅当 valueType=number
            }
          : {}),
      };
    });

    return map;
  }

  function resolveFieldRuntimeValue(fieldId: string, rawValue: any) {
    return getFormulaDataSourceDisplayValue(formulaDataSource.value, fieldId, rawValue);
  }

  // 根据公式实时计算
  const updateValue = debounce(() => {
    const { formula } = props.fieldConfig;
    const { ir } = safeParseFormula(formula ?? '');

    if (!ir) {
      return;
    }
    const fields = fieldList.value ?? [];
    const fieldTypeMap = buildFieldTypeMap(fields);
    hydrateIRNumberType(ir, fieldTypeMap);

    const contextMatch = props.path.match(/^([^[]+)\[(\d+)\]\./);

    const context = contextMatch
      ? {
          tableKey: contextMatch[1],
          rowIndex: Number(contextMatch[2]),
        }
      : undefined;

    const result = evaluateIR(ir, {
      evaluationNow: evaluationNow.value,
      context,
      getScalarFieldValue,
      getTableColumnValues,
      getFieldMeta: (fieldId: string) => {
        return fieldTypeMap[fieldId];
      },
      resolveFieldRuntimeValue,
      warn: (msg: string) => {
        // eslint-disable-next-line no-console
        console.warn(msg);
      },
    });

    const next = normalizeFormulaResult(result, {
      decimalPlaces: 2,
    });

    // 如果值未变，不需要更新
    if (Object.is(next, value.value)) return;
    value.value = next;
    emit('change', next);
  }, 100);

  watch(
    () => props.fieldConfig.defaultValue,
    (val) => {
      if (props.needInitDetail) return;
      if (val !== undefined && val !== null) {
        value.value = val;
      } else if (value.value == null) {
        value.value = 0;
      }
    },
    {
      immediate: true,
    }
  );

  watch(
    () => props.formDetail,
    () => {
      updateValue.flush?.();
      updateValue();
    },
    { deep: true }
  );

  watch(
    value,
    (val) => {
      if (val == null) value.value = 0;
    },
    { immediate: true }
  );

  function handleChange(val: any) {
    emit('change', val);
  }
</script>

<style lang="less" scoped></style>
