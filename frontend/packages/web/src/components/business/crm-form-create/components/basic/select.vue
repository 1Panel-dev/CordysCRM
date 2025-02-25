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
    <n-select
      v-model:value="value"
      :default-value="props.fieldConfig.defaultValue"
      :disabled="props.fieldConfig.editable === false"
      :options="props.fieldConfig.options"
      :multiple="props.fieldConfig.multiple"
      :placeholder="props.fieldConfig.placeholder"
      clearable
    />
  </n-form-item>
</template>

<script setup lang="ts">
  import { NFormItem, NSelect } from 'naive-ui';

  import { FormCreateField } from '../../types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    path: string;
  }>();

  const value = defineModel<(string | number)[]>('value', {
    default: [],
  });

  watch(
    () => props.fieldConfig.defaultValue,
    (val) => {
      value.value = val;
    }
  );
</script>

<style lang="less" scoped></style>
