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
    <n-checkbox-group
      v-model:value="value"
      :default-value="props.fieldConfig.defaultValue"
      :disabled="props.fieldConfig.editable === false"
      @update-value="($event) => emit('change', $event)"
    >
      <n-space :item-class="props.fieldConfig.direction === 'horizontal' ? '' : 'w-full'">
        <n-checkbox
          v-for="item in props.fieldConfig.options"
          :key="item.value"
          :value="item.value"
          :label="item.label"
        />
      </n-space>
    </n-checkbox-group>
  </n-form-item>
</template>

<script setup lang="ts">
  import { NCheckbox, NCheckboxGroup, NFormItem, NSpace } from 'naive-ui';

  import { FormCreateField } from '../../types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    path: string;
  }>();
  const emit = defineEmits<{
    (e: 'change', value: (string | number)[]): void;
  }>();

  const value = defineModel<(string | number)[]>('value', {
    default: [],
  });

  watch(
    () => props.fieldConfig.defaultValue,
    (val) => {
      value.value = val || value.value;
    },
    {
      immediate: true,
    }
  );
</script>

<style lang="less" scoped></style>
