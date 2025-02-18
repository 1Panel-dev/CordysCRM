<template>
  <n-form-item
    :label="props.fieldConfig.name"
    :show-label="props.fieldConfig.showLabel"
    :path="props.path"
    :rule="props.fieldConfig.rules"
  >
    <div v-if="props.fieldConfig.description" class="n-form-item-desc" v-html="props.fieldConfig.description"></div>
    <n-select
      v-model:value="value"
      :default-value="props.fieldConfig.defaultValue"
      :disabled="props.fieldConfig.editable === false"
      :options="props.fieldConfig.options?.map((e, i) => ({ ...e, label: t(e.label, { i: i + 1 }) }))"
      :multiple="props.fieldConfig.type === FieldTypeEnum.SELECT_MULTIPLE"
      :placeholder="props.fieldConfig.placeholder"
      clearable
    />
  </n-form-item>
</template>

<script setup lang="ts">
  import { NFormItem, NSelect } from 'naive-ui';

  import { FieldTypeEnum } from '@/components/business/crm-form-create/enum';

  import { useI18n } from '@/hooks/useI18n';

  import { FormCreateField } from '../../types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    path: string;
  }>();

  const { t } = useI18n();

  const value = defineModel<(string | number)[]>('value', {
    default: [],
  });
</script>

<style lang="less" scoped></style>
