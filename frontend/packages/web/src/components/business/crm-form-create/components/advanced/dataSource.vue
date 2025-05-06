<template>
  <n-form-item
    :label="props.fieldConfig.name"
    :show-label="props.fieldConfig.showLabel"
    :path="props.path"
    :rule="props.fieldConfig.rules"
    :required="props.fieldConfig.rules.some((rule) => rule.key === 'required')"
  >
    <div
      v-if="props.fieldConfig.description"
      class="crm-form-create-item-desc"
      v-html="props.fieldConfig.description"
    ></div>
    <CrmDataSource
      v-model:value="value"
      :rows="props.fieldConfig.initialOptions"
      :multiple="fieldConfig.type === FieldTypeEnum.DATA_SOURCE_MULTIPLE"
      :data-source-type="props.fieldConfig.dataSourceType || FieldDataSourceTypeEnum.CUSTOMER"
      :disabled="props.fieldConfig.editable === false"
      @change="($event) => emit('change', $event)"
    />
  </n-form-item>
</template>

<script setup lang="ts">
  import { NFormItem } from 'naive-ui';

  import { FieldDataSourceTypeEnum, FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';

  import CrmDataSource from '@/components/business/crm-data-source-select/index.vue';

  import { FormCreateField } from '../../types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    path: string;
  }>();
  const emit = defineEmits<{
    (e: 'change', value: (string | number)[]): void;
  }>();

  const value = defineModel<(string | number)[]>('value', {
    default: [],
  });

  watch(
    () => props.fieldConfig.defaultValue,
    (val) => {
      value.value = val || value.value || [];
    },
    {
      immediate: true,
    }
  );
</script>

<style lang="less" scoped></style>
