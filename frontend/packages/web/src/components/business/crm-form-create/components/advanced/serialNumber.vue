<template>
  <n-form-item :label="props.fieldConfig.name" :show-label="props.fieldConfig.showLabel">
    <div
      v-if="props.fieldConfig.description"
      class="crm-form-create-item-desc"
      v-html="props.fieldConfig.description"
    ></div>
    <n-input
      v-model:value="value"
      :default-value="props.fieldConfig.defaultValue"
      :placeholder="props.fieldConfig.placeholder"
      disabled
    />
  </n-form-item>
</template>

<script setup lang="ts">
  import { NFormItem, NInput } from 'naive-ui';

  import { FormCreateField } from '../../types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
  }>();

  const value = defineModel<string>('value', {
    default: '',
  });
</script>

<style lang="less" scoped></style>
