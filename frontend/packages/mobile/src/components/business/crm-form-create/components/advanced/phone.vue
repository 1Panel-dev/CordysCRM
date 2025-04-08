<template>
  <van-field
    v-model:value="value"
    type="tel"
    :disabled="props.fieldConfig.editable === false"
    :maxlength="11"
    :label="props.fieldConfig.showLabel ? props.fieldConfig.name : ''"
    :path="props.fieldConfig.id"
    :rule="props.fieldConfig.rules"
    :placeholder="props.fieldConfig.placeholder"
    @update:model-value="($event) => emit('change', $event)"
  />
</template>

<script setup lang="ts">
  import { FormCreateField } from '@cordys/web/src/components/business/crm-form-create/types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
  }>();
  const emit = defineEmits<{
    (e: 'change', value: string): void;
  }>();

  const value = defineModel<string>('value', {
    default: '',
  });
</script>

<style lang="less" scoped></style>
