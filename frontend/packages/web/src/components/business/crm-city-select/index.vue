<template>
  <n-cascader
    v-model:value="innerValue"
    :options="workingCityOptions"
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

  import { pcTextArr, regionData } from 'element-china-area-data';

  export type DataItem<T = Record<string, any>> = CascaderOption & T;

  const props = withDefaults(
    defineProps<{
      placeholder?: string;
      checkStrategy?: 'all' | 'child' | 'parent';
      range?: 'PCD' | 'PC' | 'detail';
    }>(),
    {
      range: 'PCD',
    }
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

  const workingCityOptions = computed<DataItem[]>(() => {
    if (props.range === 'PC') {
      return pcTextArr as DataItem[];
    }
    return regionData as DataItem[];
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
