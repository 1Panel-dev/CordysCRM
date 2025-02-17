<template>
  <n-cascader
    v-model:value="innerValue"
    :placeholder="t('common.pleaseSelect')"
    :options="workingCityOptions"
    clearable
    v-bind="{
      filterable: true,
      showPath: true,
      checkStrategy: 'child',
      ...$attrs,
    }"
    @update:value="handleChange"
  />
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { CascaderOption, NCascader } from 'naive-ui';

  import { useI18n } from '@/hooks/useI18n';

  import { regionData } from 'element-china-area-data';

  const { t } = useI18n();

  export type DataItem<T = Record<string, any>> = CascaderOption & T;

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

  const workingCityOptions = ref<DataItem[]>(regionData as DataItem[]);

  function handleChange(
    value: string | number | Array<string | number> | null,
    option: CascaderOption | Array<CascaderOption | null> | null,
    pathValues: Array<CascaderOption | null> | Array<CascaderOption[] | null> | null
  ) {
    emit('change', value, option, pathValues);
  }
</script>

<style scoped></style>
