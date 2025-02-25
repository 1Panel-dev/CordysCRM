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
    <n-radio-group
      v-model:value="value"
      :default-value="props.fieldConfig.defaultValue"
      :disabled="props.fieldConfig.editable === false"
      name="radiogroup"
    >
      <n-space :item-class="props.fieldConfig.direction === 'horizontal' ? '' : 'w-full'">
        <n-radio v-for="item in props.fieldConfig.options" :key="item.value" :value="item.value">
          {{ item.label }}
        </n-radio>
      </n-space>
    </n-radio-group>
  </n-form-item>
</template>

<script setup lang="ts">
  import { NFormItem, NRadio, NRadioGroup, NSpace } from 'naive-ui';

  import { FormCreateField } from '../../types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    path: string;
  }>();

  const value = defineModel<string>('value', {
    default: '',
  });

  watch(
    () => props.fieldConfig.defaultValue,
    (val) => {
      value.value = val;
    }
  );
</script>

<style lang="less" scoped></style>
