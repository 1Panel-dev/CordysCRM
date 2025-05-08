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
    <n-select
      v-model:value="value"
      :disabled="props.fieldConfig.editable === false"
      :options="props.fieldConfig.options"
      :multiple="props.fieldConfig.type === FieldTypeEnum.SELECT_MULTIPLE"
      :placeholder="props.fieldConfig.placeholder"
      clearable
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
    default: '',
  });

  watch(
    () => props.fieldConfig.defaultValue,
    (val) => {
      value.value = val || (props.fieldConfig.type === FieldTypeEnum.SELECT_MULTIPLE ? [] : '');
      emit('change', value.value);
    },
    {
      immediate: true,
    }
  );

  watch(
    () => value.value,
    (val) => {
      emit('change', val);
    }
  );
</script>

<style lang="less" scoped></style>
