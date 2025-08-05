<template>
  <CrmDataSource
    :id="props.fieldConfig.id"
    v-model:value="value"
    v-model:selected-rows="selectedRows"
    :data-source-type="props.fieldConfig.dataSourceType"
    :label="props.fieldConfig.showLabel ? props.fieldConfig.name : ''"
    :rules="props.fieldConfig.rules as FieldRule[]"
    :placeholder="props.fieldConfig.placeholder || t('common.pleaseSelect')"
    :disabled="!props.fieldConfig.editable"
    :multiple="props.fieldConfig.type === FieldTypeEnum.DATA_SOURCE_MULTIPLE"
    @change="($event) => emit('change', $event)"
  >
  </CrmDataSource>
</template>

<script setup lang="ts">
  import { FieldRule } from 'vant';

  import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmDataSource from '@/components/business/crm-datasource/index.vue';

  import { FormCreateField } from '@cordys/web/src/components/business/crm-form-create/types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
  }>();
  const emit = defineEmits<{
    (e: 'change', value: string | string[]): void;
  }>();

  const { t } = useI18n();

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

  watch(
    () => props.fieldConfig.initialOptions,
    (val) => {
      selectedRows.value = val as Record<string, any>[];
    },
    {
      immediate: true,
    }
  );
</script>

<style lang="less" scoped></style>
