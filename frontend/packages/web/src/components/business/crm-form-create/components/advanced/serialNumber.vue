<template>
  <n-form-item
    :label="props.fieldConfig.name"
    :label-placement="props.isSubTableField || props.isSubTableRender ? 'top' : props.formConfig?.labelPos"
    :show-label="!props.isSubTableRender"
  >
    <template #label>
      <div v-if="props.fieldConfig.showLabel" class="flex h-[22px] items-center gap-[4px] whitespace-nowrap">
        <div class="one-line-text">{{ props.fieldConfig.name }}</div>
        <CrmIcon v-if="props.fieldConfig.resourceFieldId" type="iconicon_correlation" />
      </div>
      <div v-else class="h-[22px]"></div>
    </template>
    <div
      v-if="props.fieldConfig.description"
      class="crm-form-create-item-desc"
      v-html="props.fieldConfig.description"
    ></div>
    <n-divider v-if="props.isSubTableField && !props.isSubTableRender" class="!my-0" />
    <n-tooltip trigger="hover" placement="top" :disabled="props.fieldConfig.prefixType !== 'formula'">
      <template #trigger>
        <n-input
          v-model:value="displayValue"
          :default-value="props.fieldConfig.defaultValue"
          :placeholder="props.fieldConfig.placeholder"
          disabled
        />
      </template>
      {{ formulaTooltip }}
    </n-tooltip>
  </n-form-item>
</template>

<script setup lang="ts">
  import { NDivider, NFormItem, NInput, NTooltip } from 'naive-ui';
  import { debounce } from 'lodash-es';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { FormConfig } from '@lib/shared/models/system/module';

  import { executeFormFormula } from '@/components/business/crm-formula/formula-runtime/formula-executor';
  import { getFormulaDisplayInfo } from '@/components/business/crm-formula/formula-runtime/formula-executor/formula-display';

  import { FormCreateField } from '../../types';

  const { t } = useI18n();

  const props = defineProps<{
    fieldConfig: FormCreateField;
    formConfig?: FormConfig;
    isSubTableField?: boolean; // 是否是子表字段
    isSubTableRender?: boolean; // 是否是子表渲染
    path: string;
    formDetail?: Record<string, any>;
    needInitDetail?: boolean;
  }>();

  const value = defineModel<string>('value', {
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

  const fieldList = computed(() => formulaFormContext?.value.fields);
  const formulaDataSource = computed(() => formulaFormContext?.value.formulaDataSource);
  const evaluationNow = computed(() => formulaFormContext?.value.evaluationNow);

  const updateValue = debounce(() => {
    const { formula } = props.fieldConfig;

    const result = executeFormFormula({
      formula,
      path: props.path,
      formDetail: props.formDetail,
      fields: fieldList.value ?? [],
      formulaDataSource: formulaDataSource.value,
      needInitDetail: props.needInitDetail,
      evaluationNow: evaluationNow.value,
      decimalPlaces: 2,
      warn: (msg: string) => {
        // eslint-disable-next-line no-console
        console.warn(msg);
      },
    });

    if (!result) {
      return;
    }

    const next = result.normalizedResult;

    if (Object.is(next, value.value)) return;
    const serialNumberPlaceholder = `\${${props.fieldConfig.name}}`;
    const resultStr = props.fieldConfig.prefixType === 'formula' ? `${next}${serialNumberPlaceholder}` : next;
    value.value = resultStr;
  }, 10);

  const displayValue = computed(() => (props.needInitDetail ? value.value : ''));

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

  watch(
    () => props.formDetail,
    () => {
      updateValue.flush?.();
      if (props.fieldConfig.prefixType === 'formula' && !props.needInitDetail) {
        updateValue();
      }
    },
    { deep: true }
  );
</script>

<style lang="less" scoped></style>
