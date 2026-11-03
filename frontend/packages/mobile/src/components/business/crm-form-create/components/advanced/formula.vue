<template>
  <van-field
    :model-value="displayValue"
    :label="fieldConfig.showLabel ? fieldConfig.name : ''"
    :name="fieldConfig.id"
    readonly
  />
</template>

<script setup lang="ts">
  import { formatFormulaResultValue } from '@lib/shared/method/formCreate';

  import { executeFormFormula } from '@cordys/web/src/components/business/crm-formula/formula-runtime/formula-executor';
  import { getFormulaResultFormatOptions } from '@cordys/web/src/components/business/crm-formula/utils';
  import type { FormCreateField } from '@cordys/web/src/components/business/crm-form-create/types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    path: string;
    formDetail?: Record<string, any>;
    needInitDetail?: boolean;
  }>();

  const emit = defineEmits<{
    (event: 'change', value: any): void;
  }>();

  const value = defineModel<any>('value', {
    default: '',
  });

  const formulaFormContext = inject(
    'formFieldsProvider',
    ref({
      fields: [],
      formulaDataSource: {},
      evaluationNow: null,
    })
  );

  const fieldList = computed(() => formulaFormContext.value.fields);
  const formulaDataSource = computed(() => formulaFormContext.value.formulaDataSource);
  const evaluationNow = computed(() => formulaFormContext.value.evaluationNow);
  const displayValue = computed(() => formatFormulaResultValue(value.value, props.fieldConfig, '-'));

  function updateValue() {
    const result = executeFormFormula({
      formula: props.fieldConfig.formula,
      path: props.path,
      formDetail: props.formDetail,
      fields: fieldList.value,
      formulaDataSource: formulaDataSource.value,
      needInitDetail: props.needInitDetail,
      evaluationNow: evaluationNow.value,
      ...getFormulaResultFormatOptions(props.fieldConfig),
    });

    if (!result) return;

    const next = result.normalizedResult;
    if (Object.is(next, value.value)) return;
    value.value = next;
    emit('change', next);
  }

  watch(
    () => props.fieldConfig.defaultValue,
    (defaultValue) => {
      if (props.needInitDetail) return;
      if (defaultValue !== undefined && defaultValue !== null) {
        value.value = defaultValue;
      }
    },
    { immediate: true }
  );

  watch(() => props.formDetail, updateValue, { deep: true, immediate: true });
  watch(() => formulaDataSource.value, updateValue, { deep: true });
</script>
