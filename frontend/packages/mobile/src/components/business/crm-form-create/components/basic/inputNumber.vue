<template>
  <van-field
    v-model="value"
    :label="props.fieldConfig.showLabel ? props.fieldConfig.name : ''"
    :name="props.fieldConfig.id"
    :rules="props.fieldConfig.rules as FieldRule[]"
    type="number"
    :placeholder="props.fieldConfig.placeholder || t('common.pleaseInput')"
    :disabled="props.fieldConfig.editable === false"
    :max="1000000000"
    :min="props.fieldConfig.min"
    clearable
    :formatter="format"
    format-trigger="onBlur"
    @update:model-value="(val) => emit('change', val)"
  >
    <template v-if="props.fieldConfig.numberFormat === 'percent'" #right-icon> % </template>
  </van-field>
</template>

<script setup lang="ts">
  import { FieldRule } from 'vant';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import { FormCreateField } from '@cordys/web/src/components/business/crm-form-create/types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
  }>();

  const emit = defineEmits<{
    (e: 'change', value: number): void;
  }>();

  const { t } = useI18n();

  const value = defineModel<any>('value', {
    default: null,
  });

  watch(
    () => props.fieldConfig.defaultValue,
    (val) => {
      value.value = val || value.value;
    },
    {
      immediate: true,
    }
  );

  // TODO lmy 千分位没效果
  // 失焦时格式化显示（千分位 + 精度）
  function format(val: string): string {
    if (!val) return '';

    const num = Number(val.replace(/,/g, ''));
    if (Number.isNaN(num)) return '';

    const precision = props.fieldConfig.precision ?? 0;

    if (props.fieldConfig.numberFormat === 'number' && props.fieldConfig.showThousandsSeparator) {
      // 带千分位 + 小数位
      return num.toLocaleString('en-US', {
        minimumFractionDigits: precision,
        maximumFractionDigits: precision,
      });
    }
    // 仅处理小数位
    return num.toFixed(precision);
  }
</script>
