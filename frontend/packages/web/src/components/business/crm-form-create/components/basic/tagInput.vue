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
      filterable
      multiple
      tag
      :placeholder="props.fieldConfig.placeholder || t('common.tagsInputPlaceholder')"
      :show-arrow="false"
      :show="false"
      :default-value="props.fieldConfig.defaultValue"
      :disabled="props.fieldConfig.editable === false"
      clearable
    />
  </n-form-item>
</template>

<script setup lang="ts">
  import { NFormItem, NSelect } from 'naive-ui';

  import { useI18n } from '@/hooks/useI18n';

  import { FormCreateField } from '../../types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    path: string;
  }>();

  const { t } = useI18n();

  const value = defineModel<string>('value', {
    default: '',
  });

  watch(
    () => props.fieldConfig.defaultValue,
    (val) => {
      value.value = val;
    }
  );
</script>

<style lang="less" scoped></style>
