<template>
  <n-form-item :label="props.fieldConfig.name" :show-label="props.fieldConfig.showLabel">
    <div
      v-if="props.fieldConfig.description"
      class="crm-form-create-item-desc"
      v-html="props.fieldConfig.description"
    ></div>
    <n-divider v-if="props.isSubTableField && !props.isSubTableRender" class="!my-0" />
    <n-input
      v-model:value="value"
      :default-value="props.fieldConfig.defaultValue"
      :placeholder="props.fieldConfig.placeholder"
      disabled
    />
  </n-form-item>
</template>

<script setup lang="ts">
  import { NDivider, NFormItem, NInput } from 'naive-ui';

  import { FormCreateField } from '../../types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    isSubTableField?: boolean; // 是否是子表字段
    isSubTableRender?: boolean; // 是否是子表渲染
  }>();

  const value = defineModel<string>('value', {
    default: '',
  });
</script>

<style lang="less" scoped></style>
