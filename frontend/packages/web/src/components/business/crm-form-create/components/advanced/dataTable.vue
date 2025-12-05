<template>
  <n-form-item :label="props.fieldConfig.name" :show-label="props.fieldConfig.showLabel">
    <div
      v-if="props.fieldConfig.description"
      class="crm-form-create-item-desc"
      v-html="props.fieldConfig.description"
    ></div>
    <CrmSubTable
      v-model:value="value"
      :sub-fields="fieldConfig.subFields || []"
      :need-init-detail="props.needInitDetail"
      :parent-id="fieldConfig.id"
      :readonly="false"
      :form-detail="props.formDetail"
      :fixed-column="props.fieldConfig.fixedColumn"
      :sumColumns="props.fieldConfig.sumColumns"
      :disabled="props.fieldConfig.editable === false || !!props.fieldConfig.resourceFieldId"
    />
  </n-form-item>
</template>

<script setup lang="ts">
  import { NFormItem } from 'naive-ui';

  import CrmSubTable from '@/components/business/crm-sub-table/index.vue';

  import { FormCreateField } from '../../types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    path: string;
    formDetail?: Record<string, any>;
    needInitDetail?: boolean; // 判断是否编辑情况
  }>();
  const emit = defineEmits<{
    (e: 'change', value: Record<string, any>[]): void;
  }>();

  const value = defineModel<Record<string, any>[]>('value', {
    default: [],
  });

  watch(
    () => value.value,
    (newVal) => {
      emit('change', newVal);
    }
  );
</script>

<style lang="less" scoped></style>
