<template>
  <n-form-item
    :label="props.fieldConfig.name"
    :path="props.path"
    :rule="props.fieldConfig.rules"
    :required="props.fieldConfig.rules.some((rule) => rule.key === 'required')"
    :label-placement="props.isSubTableField || props.isSubTableRender ? 'top' : props.formConfig?.labelPos"
    :show-label="!props.isSubTableRender"
  >
    <template #label>
      <div
        v-if="props.fieldConfig.showLabel"
        class="flex h-[22px] w-full items-center gap-[4px] overflow-hidden whitespace-nowrap"
      >
        <div class="one-line-text">{{ props.fieldConfig.name }}</div>
        <CrmIcon v-if="props.fieldConfig.resourceFieldId" type="iconicon_correlation" />
      </div>
      <div v-else class="h-[22px]"></div>
    </template>
    <div
      v-if="props.fieldConfig.description && !props.isSubTableRender"
      class="crm-form-create-item-desc"
      v-html="props.fieldConfig.description"
    ></div>
    <n-divider v-if="props.isSubTableField && !props.isSubTableRender" class="!my-0" />

    <n-tooltip
      trigger="hover"
      placement="top"
      :disabled="props.fieldConfig.defaultValueType !== 'formula' || props.isSubTableRender"
    >
      <template #trigger>
        <n-input
          v-model:value="value"
          :maxlength="255"
          :placeholder="props.fieldConfig.placeholder"
          :disabled="
            props.fieldConfig.editable === false ||
            !!props.fieldConfig.resourceFieldId ||
            props.fieldConfig.defaultValueType === 'formula'
          "
          clearable
          @update-value="($event) => emit('change', $event)"
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
    path: string;
    needInitDetail?: boolean; // 判断是否编辑情况
    isSubTableField?: boolean; // 是否是子表字段
    isSubTableRender?: boolean; // 是否是子表渲染
    formDetail?: Record<string, any>;
  }>();
  const emit = defineEmits<{
    (e: 'change', value: string): void;
  }>();

  const value = defineModel<string>('value', {
    default: '',
  });

  watch(
    () => props.fieldConfig.defaultValue,
    (val) => {
      if (!props.needInitDetail) {
        value.value = val !== undefined && !value.value ? val : value.value;
        emit('change', value.value);
      }
    },
    {
      immediate: true,
    }
  );

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
    value.value = next;
  }, 100);

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
      if (props.fieldConfig.defaultValueType === 'formula') {
        updateValue();
      }
    },
    { deep: true }
  );
</script>

<style lang="less" scoped></style>
