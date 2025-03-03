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
    <n-date-picker
      v-model:value="value"
      :type="props.fieldConfig.datetype"
      :placeholder="props.fieldConfig.placeholder"
      :default-value="props.fieldConfig.defaultValue"
      :disabled="props.fieldConfig.editable === false"
      class="w-full"
      @update-value="($event) => emit('change', $event)"
    >
    </n-date-picker>
  </n-form-item>
</template>

<script setup lang="ts">
  import { NDatePicker, NFormItem } from 'naive-ui';

  import { FormCreateField } from '../../types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    path: string;
  }>();
  const emit = defineEmits<{
    (e: 'change', value: (string | number)[]): void;
  }>();

  const value = defineModel<number>('value', {
    default: null,
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
