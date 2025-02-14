<template>
  <n-form-item :label="props.fieldConfig.name" :path="props.path" :rule="props.fieldConfig.rules">
    <n-input v-model="value" :maxlength="255" />
  </n-form-item>
</template>

<script setup lang="ts">
  import { NFormItem, NInput } from 'naive-ui';

  import { FormCreateField } from '../types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    path: string;
  }>();

  const value = defineModel<string>('value', {
    default: '',
  });
</script>

<style lang="less" scoped></style>
