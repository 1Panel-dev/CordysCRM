<template>
  <CrmDataSource
    v-model:value="value"
    v-model:selected-rows="selectedRows"
    :data-source-type="props.fieldConfig.dataSourceType"
    :label="props.fieldConfig.showLabel ? props.fieldConfig.name : ''"
    :name="props.fieldConfig.id"
    :rules="props.fieldConfig.rules as FieldRule[]"
    :placeholder="props.fieldConfig.placeholder"
    :disabled="props.fieldConfig.editable === false"
    @change="($event) => emit('change', $event)"
  >
  </CrmDataSource>
</template>

<script setup lang="ts">
  import { FieldRule } from 'vant';

  import CrmDataSource from '@/components/business/crm-datasource/index.vue';

  import { FormCreateField } from '@cordys/web/src/components/business/crm-form-create/types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
  }>();
  const emit = defineEmits<{
    (e: 'change', value: string | string[]): void;
  }>();

  const value = defineModel<string | string[]>('value', {
    default: '',
  });
  const selectedRows = ref<Record<string, any>[]>(props.fieldConfig.initialOptions || []);

  watch(
    () => props.fieldConfig.defaultValue,
    (val) => {
      value.value = val;
    },
    {
      immediate: true,
    }
  );
</script>

<style lang="less" scoped></style>
