<template>
  <CrmFormula
    v-model:value="value"
    :path="path"
    :field-config="fieldConfig"
    :form-config="formConfig"
    :form-detail="formDetail"
    :need-init-detail="needInitDetail"
    :is-sub-table-field="isSubTableField"
    :is-sub-table-render="isSubTableRender"
    @change="handleChange"
  />
</template>

<script setup lang="ts">
  import type { FormConfig } from '@lib/shared/models/system/module';

  import CrmFormula from '@/components/business/crm-formula/index.vue';

  import type { FormCreateField } from '@cordys/web/src/components/business/crm-form-create/types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    formConfig?: FormConfig;
    path: string;
    formDetail?: Record<string, any>;
    needInitDetail?: boolean; // 判断是否编辑情况
    isSubTableField?: boolean; // 是否是子表字段
    isSubTableRender?: boolean; // 是否是子表渲染
  }>();

  const emit = defineEmits<{
    (e: 'change', value: number | null): void;
  }>();

  const value = defineModel<number | null>('value', {
    default: 0,
  });

  function handleChange(val: number | null) {
    emit('change', val);
  }
</script>

<style lang="less" scoped></style>
