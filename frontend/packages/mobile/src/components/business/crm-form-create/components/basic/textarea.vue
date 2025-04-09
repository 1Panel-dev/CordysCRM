<template>
  <van-field
    v-model="value"
    type="textarea"
    :label="props.fieldConfig.showLabel ? props.fieldConfig.name : ''"
    :name="props.fieldConfig.id"
    :rules="props.fieldConfig.rules as FieldRule[]"
    :maxlength="1000"
    :placeholder="props.fieldConfig.placeholder"
    :disabled="props.fieldConfig.editable === false"
    clearable
    @update:model-value="($event) => emit('change', $event)"
  >
  </van-field>
</template>

<script setup lang="ts">
  import { FieldRule } from 'vant';

  import { FormCreateField } from '@cordys/web/src/components/business/crm-form-create/types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    path: string;
  }>();
  const emit = defineEmits<{
    (e: 'change', value: (string | number)[]): void;
  }>();

  const value = defineModel<string>('value', {
    default: '',
  });

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
