<template>
  <n-cascader
    v-model:value="innerValue"
    :options="industryOptions"
    clearable
    v-bind="{
      filterable: true,
      showPath: true,
      checkStrategy: props.checkStrategy || 'child',
      ...$attrs,
    }"
    max-tag-count="responsive"
    :placeholder="props.placeholder"
    @update:value="handleChange"
  />
</template>

<script setup lang="ts">
  import { CascaderOption, NCascader } from 'naive-ui';

  import { industryOptions } from './config';

  export type DataItem<T = Record<string, any>> = CascaderOption & T;

  const props = withDefaults(
    defineProps<{
      placeholder?: string;
      checkStrategy?: 'all' | 'child' | 'parent';
    }>(),
    {}
  );

  const emit = defineEmits<{
    (
      e: 'change',
      value: string | number | Array<string | number> | null,
      option: CascaderOption | Array<CascaderOption | null> | null,
      pathValues: Array<CascaderOption | null> | Array<CascaderOption[] | null> | null
    ): void;
  }>();

  const innerValue = defineModel<string | number | Array<string | number> | null>('value', {
    required: true,
    default: null,
  });

  function handleChange(
    value: string | number | Array<string | number> | null,
    option: CascaderOption | Array<CascaderOption | null> | null,
    pathValues: Array<CascaderOption | null> | Array<CascaderOption[] | null> | null
  ) {
    emit('change', value, option, pathValues);
  }
</script>

<style scoped></style>
