<template>
  <van-field
    v-model="value"
    :label="props.fieldConfig.showLabel ? props.fieldConfig.name : ''"
    :name="props.fieldConfig.id"
    :placeholder="props.fieldConfig.placeholder"
    disabled
  >
  </van-field>
</template>

<script setup lang="ts">
  import { FormCreateField } from '@cordys/web/src/components/business/crm-form-create/types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
  }>();

  const value = defineModel<string>('value', {
    default: '',
  });
</script>
