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
      :default-value="props.fieldConfig.defaultValue"
      :disabled="props.fieldConfig.editable === false"
      :options="props.fieldConfig.options"
      :multiple="props.fieldConfig.type === FieldTypeEnum.SELECT_MULTIPLE"
      :placeholder="props.fieldConfig.placeholder"
      clearable
      @update-value="emit('change', $event)"
    />
  </n-form-item>
</template>

<script setup lang="ts">
  import { NFormItem, NSelect } from 'naive-ui';

  import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';

  import { FormCreateField } from '../../types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    path: string;
  }>();
  const emit = defineEmits<{
    (e: 'change', value: string | number | (string | number)[]): void;
  }>();

  const value = defineModel<string | number | (string | number)[]>('value', {
    default: [],
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
