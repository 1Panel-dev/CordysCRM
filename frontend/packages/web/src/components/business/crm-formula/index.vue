<template>
  <n-tooltip trigger="hover" placement="top">
    <template #trigger>
      <component
        :is="currentComponent"
        v-model:value="value"
        :path="props.path"
        :field-config="runtimeFieldConfig"
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

  import { executeFormFormula } from './formula-runtime/formula-executor';
  import { getFormulaDisplayInfo } from './formula-runtime/formula-executor/formula-display';
  import { getFormulaResultFormatOptions } from './utils';
  import type { FormCreateField, FormCreateFieldRule } from '@cordys/web/src/components/business/crm-form-create/types';

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
    default: '',
  });

  const formulaResultFormat = computed(() => props.fieldConfig.formulaResultFormat ?? 'text');

  const currentComponent = computed(() => {
    return formulaResultFormat.value === 'number' && typeof value.value === 'number' ? inputNumber : singleText;
  });

  const runtimeFieldConfig = computed(() => {
    const valueType: FormCreateFieldRule['type'] = typeof value.value === 'number' ? 'number' : 'string';
    return {
      ...props.fieldConfig,
      rules: props.fieldConfig.rules.map((rule) => (rule.type ? { ...rule, type: valueType } : rule)),
    };
  });

  function getEmptyValue() {
    return formulaResultFormat.value === 'number' ? 0 : '';
  }

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

  const formulaDisplayInfo = computed(() =>
    getFormulaDisplayInfo({
      formula: props.fieldConfig.formula,
      fields: fieldList.value ?? [],
      invalidText: t('crmFormDesign.formulaFieldChanged'),
      emptyText: t('crmFormDesign.formulaTooltip'),
      isSubTableRender: props.isSubTableRender,
    })
  );

  const formulaTooltip = computed(() => formulaDisplayInfo.value.tooltip);

  // 根据公式实时计算 todo 等待优化
  const updateValue = debounce(() => {
    const { formula } = props.fieldConfig;
    const resultFormatOptions = getFormulaResultFormatOptions(props.fieldConfig);
    const result = executeFormFormula({
      formula,
      path: props.path,
      formDetail: props.formDetail,
      fields: fieldList.value ?? [],
      formulaDataSource: formulaDataSource.value,
      needInitDetail: props.needInitDetail,
      evaluationNow: evaluationNow.value,
      ...resultFormatOptions,
      warn: (msg: string) => {
        // eslint-disable-next-line no-console
        console.warn(msg);
      },
    });

    if (!result) {
      return;
    }

    const next =
      formulaResultFormat.value === 'number' && typeof result.normalizedResult === 'number'
        ? result.normalizedResult
        : String(result.normalizedResult);

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
        value.value = getEmptyValue();
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
      if (val == null) value.value = getEmptyValue();
    },
    { immediate: true }
  );

  function handleChange(val: any) {
    emit('change', val);
  }
</script>

<style lang="less" scoped></style>
