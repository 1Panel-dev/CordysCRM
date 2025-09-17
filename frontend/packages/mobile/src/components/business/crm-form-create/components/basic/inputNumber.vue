<template>
  <van-field
    v-model="displayValue"
    :label="props.fieldConfig.showLabel ? props.fieldConfig.name : ''"
    :name="props.fieldConfig.id"
    :rules="props.fieldConfig.rules as FieldRule[]"
    type="text"
    :placeholder="props.fieldConfig.placeholder || t('common.pleaseInput')"
    :disabled="props.fieldConfig.editable === false"
    clearable
    @blur="onBlur"
    @focus="onFocus"
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
    (e: 'change', value?: number | null): void;
  }>();

  const { t } = useI18n();

  const value = defineModel<number | null>('value', {
    default: null,
  });

  const displayValue = ref('');

  watch(
    () => props.fieldConfig.defaultValue,
    (val) => {
      value.value = val || value.value;
      displayValue.value = val || value.value;
    },
    {
      immediate: true,
    }
  );

  // 失去焦点时：清理非法字符 → 限制小数点 → min/max → 精度 → 千分位
  function onBlur() {
    if (!displayValue.value) {
      value.value = null;
      emit('change', null);
      return;
    }

    // 只保留数字和小数点
    let clean = displayValue.value.replace(/[^\d.]/g, '');

    // 限制只能有一个小数点
    const dotIndex = clean.indexOf('.');
    if (dotIndex !== -1) {
      clean = clean.substring(0, dotIndex + 1) + clean.substring(dotIndex + 1).replace(/\./g, '');
    }

    let num = Number(clean);
    if (Number.isNaN(num)) {
      value.value = null;
      displayValue.value = '';
      emit('change', null);
      return;
    }

    // 限制 min/max
    if (props.fieldConfig.min != null && num < props.fieldConfig.min) {
      num = props.fieldConfig.min;
    }
    if (props.fieldConfig.max != null && num > props.fieldConfig.max) {
      num = props.fieldConfig.max;
    }

    if (!props.fieldConfig.max && num > 1000000000) {
      num = 1000000000;
    }

    const precision = props.fieldConfig.precision ?? 0;
    num = Number(num.toFixed(precision));

    // 更新真实值
    value.value = num;
    emit('change', num);

    // 更新显示值（千分位/小数位）
    if (props.fieldConfig.numberFormat === 'number' && props.fieldConfig.showThousandsSeparator) {
      displayValue.value = num.toLocaleString('en-US', {
        minimumFractionDigits: precision,
        maximumFractionDigits: precision,
      });
    } else {
      displayValue.value = String(num);
    }
  }

  // 聚焦时：去掉千分位，方便编辑
  function onFocus() {
    if (!displayValue.value) return;
    displayValue.value = displayValue.value.replace(/,/g, '');
  }
</script>
