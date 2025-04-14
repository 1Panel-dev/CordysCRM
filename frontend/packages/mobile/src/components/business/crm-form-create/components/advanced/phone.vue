<template>
  <van-field
    v-model="value"
    type="tel"
    :disabled="props.fieldConfig.editable === false"
    :maxlength="11"
    :label="props.fieldConfig.showLabel ? props.fieldConfig.name : ''"
    :path="props.fieldConfig.id"
    :rules="props.fieldConfig.rules as FieldRule[]"
    :placeholder="props.fieldConfig.placeholder || t('common.pleaseInput')"
    @update:model-value="($event) => emit('change', $event)"
  />
</template>

<script setup lang="ts">
  import { FieldRule } from 'vant';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import { FormCreateField } from '@cordys/web/src/components/business/crm-form-create/types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
  }>();
  const emit = defineEmits<{
    (e: 'change', value: string): void;
  }>();

  const { t } = useI18n();

  const value = defineModel<string>('value', {
    default: '',
  });
</script>

<style lang="less" scoped></style>
