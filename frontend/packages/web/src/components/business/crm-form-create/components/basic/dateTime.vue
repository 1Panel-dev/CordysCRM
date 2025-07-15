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
    <n-date-picker
      v-model:value="value"
      :type="props.fieldConfig.dateType"
      :placeholder="props.fieldConfig.placeholder"
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
    needInitDetail?: boolean; // 判断是否编辑情况
  }>();
  const emit = defineEmits<{
    (e: 'change', value: null | number | (string | number)[]): void;
  }>();

  const value = defineModel<null | number | [number, number]>('value', {
    default: null,
  });

  watch(
    () => props.fieldConfig.defaultValue,
    (val) => {
      if (!props.needInitDetail) {
        value.value = val || value.value;
        emit('change', value.value);
      }
    },
    {
      immediate: true,
    }
  );

  watch(
    () => props.fieldConfig.dateDefaultType,
    (val) => {
      if (val === 'current') {
        value.value = new Date().getTime();
        emit('change', value.value);
      }
    },
    {
      immediate: true,
    }
  );
</script>

<style lang="less" scoped></style>
