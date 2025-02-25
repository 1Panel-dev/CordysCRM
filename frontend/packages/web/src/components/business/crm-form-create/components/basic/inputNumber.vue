<template>
  <n-form-item
    :label="props.fieldConfig.name"
    :show-label="props.fieldConfig.showLabel"
    :path="props.path"
    :rule="props.fieldConfig.rules"
  >
    <div
      v-if="props.fieldConfig.description"
      class="crm-form-create-item-desc"
      v-html="props.fieldConfig.description"
    ></div>
    <n-input-number
      v-model:value="value"
      :max="props.fieldConfig.max"
      :min="props.fieldConfig.min"
      :placeholder="props.fieldConfig.placeholder"
      :default-value="props.fieldConfig.defaultValue"
      :disabled="props.fieldConfig.editable === false"
      :parse="parse"
      :format="format"
      :show-button="false"
      button-placement="both"
      clearable
      class="w-full"
    >
      <template v-if="props.fieldConfig.numberFormat === 'percent'" #suffix> % </template>
    </n-input-number>
  </n-form-item>
</template>

<script setup lang="ts">
  import { NFormItem, NInputNumber } from 'naive-ui';

  import { FormCreateField } from '../../types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    path: string;
  }>();

  const value = defineModel<number>('value', {
    default: null,
  });

  watch(
    () => props.fieldConfig.defaultValue,
    (val) => {
      value.value = val;
    }
  );

  function parse(val: string) {
    const nums = val.replace(/,/g, '').trim();
    if (!props.fieldConfig.showThousandsSeparator || /^\d+(\.(\d+)?)?$/.test(nums)) {
      return Number(nums);
    }
    return nums === '' ? null : Number.NaN;
  }

  function format(val: number | null) {
    if (val === null) return '';
    return val.toLocaleString('en-US');
  }
</script>

<style lang="less" scoped></style>
