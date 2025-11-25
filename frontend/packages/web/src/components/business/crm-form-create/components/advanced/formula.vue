<template>
  <inputNumber
    v-model:value="value"
    path="fieldValue"
    :field-config="fieldConfig"
    :is-sub-table-field="props.isSubTableField"
    :is-sub-table-render="props.isSubTableRender"
    @change="handleChange"
  />
</template>

<script setup lang="ts">
  import inputNumber from '../basic/inputNumber.vue';

  import { FormCreateField } from '../../types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    path: string;
    needInitDetail?: boolean; // 判断是否编辑情况
    isSubTableField?: boolean; // 是否是子表字段
    isSubTableRender?: boolean; // 是否是子表渲染
  }>();

  const emit = defineEmits<{
    (e: 'change', value: number | null): void;
  }>();

  const value = defineModel<number | null>('value', {
    default: null,
  });

  watch(
    () => props.fieldConfig.formula,
    (val) => {
      if (!props.needInitDetail) {
        // TODO:  根据表单计算公式formula金额
        // value.value = val || value.value;
        emit('change', value.value);
      }
    },
    {
      immediate: true,
    }
  );

  function handleChange(val: number | null) {
    emit('change', val);
  }
</script>

<style lang="less" scoped></style>
