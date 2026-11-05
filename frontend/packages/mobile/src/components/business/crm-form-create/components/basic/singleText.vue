<template>
  <van-field
    v-model="value"
    :label="props.fieldConfig.showLabel ? props.fieldConfig.name : ''"
    :name="props.fieldConfig.id"
    :rules="props.fieldConfig.rules as FieldRule[]"
    :maxlength="255"
    :placeholder="props.fieldConfig.placeholder"
    :disabled="!props.fieldConfig.editable || !!props.fieldConfig.resourceFieldId || isFormula"
    :clearable="!isFormula"
    @update:model-value="($event) => emit('change', $event)"
  >
    <template #right-icon>
      <van-icon
        v-if="isFormula && !props.fieldConfig.resourceFieldId"
        name="info-o"
        @click.stop="showFormulaConfig = true"
      />
    </template>
  </van-field>
  <van-popup v-model:show="showFormulaConfig" position="bottom" round closeable>
    <div class="formula-config-content">
      <div class="formula-config-title">{{ props.fieldConfig.name }}</div>
      <div class="formula-config-text">{{ formulaTooltip }}</div>
    </div>
  </van-popup>
</template>

<script setup lang="ts">
  import { FieldRule } from 'vant';
  import { debounce } from 'lodash-es';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import { FormCreateField } from '@cordys/web/src/components/business/crm-form-create/types';
  import { executeFormFormula } from '@cordys/web/src/components/business/crm-formula/formula-runtime/formula-executor';
  import { safeParseFormula } from '@cordys/web/src/components/business/crm-formula-editor/utils';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    needInitDetail?: boolean; // 判断是否编辑情况
    path?: string;
    formDetail?: Record<string, any>;
  }>();
  const emit = defineEmits<{
    (e: 'change', value: string): void;
  }>();

  const { t } = useI18n();

  const value = defineModel<string>('value', {
    default: '',
  });
  const isFormula = computed(
    () => props.fieldConfig.defaultValueType === 'formula' && !props.fieldConfig.resourceFieldId
  );
  const showFormulaConfig = ref(false);

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
  const formulaTooltip = computed(() => {
    const formula = safeParseFormula(props.fieldConfig.formula);
    return formula.display || formula.source || t('crmFormDesign.formulaTooltip');
  });

  const updateFormulaValue = debounce(() => {
    if (!isFormula.value) return;
    const result = executeFormFormula({
      formula: props.fieldConfig.formula,
      path: props.path || props.fieldConfig.id,
      formDetail: props.formDetail,
      fields: fieldList.value,
      formulaDataSource: formulaDataSource.value,
      needInitDetail: props.needInitDetail,
      evaluationNow: evaluationNow.value,
      decimalPlaces: 2,
    });
    if (!result) return;

    const next = String(result.normalizedResult);
    if (Object.is(next, value.value)) return;
    value.value = next;
    emit('change', next);
  }, 100);

  watch(
    () => props.fieldConfig.defaultValue,
    (val) => {
      if (!props.needInitDetail) {
        value.value = val !== undefined ? val : value.value;
        emit('change', value.value);
      }
    },
    {
      immediate: true,
    }
  );

  watch(() => props.formDetail, updateFormulaValue, { deep: true, immediate: true });
  watch(() => formulaDataSource.value, updateFormulaValue, { deep: true });

  onBeforeUnmount(() => updateFormulaValue.cancel());
</script>

<style lang="less" scoped>
  .formula-config-content {
    padding: 24px 20px 32px;
  }
  .formula-config-title {
    margin-bottom: 12px;
    font-weight: 600;
    color: var(--text-n1);
  }
  .formula-config-text {
    color: var(--text-n3);
    line-height: 1.6;
    overflow-wrap: anywhere;
  }
</style>
