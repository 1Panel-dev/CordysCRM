<template>
  <n-tooltip trigger="hover" placement="top">
    <template #trigger>
      <inputNumber
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

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { FormConfig } from '@lib/shared/models/system/module';

  import basicComponents from '@/components/business/crm-form-create/components/basic/index';

  import { safeParseFormula } from '../crm-formula-editor/utils';
  import evaluateIR from './formula-runtime';
  import type { FormCreateField } from '@cordys/web/src/components/business/crm-form-create/types';

  const { t } = useI18n();
  const { inputNumber } = basicComponents;

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
    (e: 'change', value: number | null): void;
  }>();

  const value = defineModel<number | null>('value', {
    default: 0,
  });

  const fieldList = inject('formFieldsProvider', ref([]));

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

  // 根据公式实时计算
  const updateValue = debounce(() => {
    const { formula } = props.fieldConfig;
    const { ir } = safeParseFormula(formula ?? '');

    if (!ir) {
      value.value = 0;
      return;
    }
    const contextMatch = props.path.match(/^([^[]+)\[(\d+)\]\./);

    const context = contextMatch
      ? {
          tableKey: contextMatch[1],
          rowIndex: Number(contextMatch[2]),
        }
      : undefined;

    const result = evaluateIR(ir, {
      context,
      getScalarFieldValue,
      getTableColumnValues,
      warn: (msg: string) => {
        // eslint-disable-next-line no-console
        console.warn(msg);
      },
    });

    const next = result != null && typeof result === 'number' ? Number(result.toFixed(2)) : 0;
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

  function handleChange(val: number | null) {
    emit('change', val);
  }
</script>

<style lang="less" scoped></style>
