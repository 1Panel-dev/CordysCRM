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
    <CrmDataSource
      v-model:value="value"
      :rows="props.fieldConfig.dataSourceSelectedRows"
      :multiple="fieldConfig.multiple"
      :data-source-type="props.fieldConfig.dataSourceType"
    />
  </n-form-item>
</template>

<script setup lang="ts">
  import { NFormItem } from 'naive-ui';

  import CrmDataSource from '@/components/business/crm-data-source-select/index.vue';

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
