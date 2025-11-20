<template>
  <inputNumber v-model:value="value" path="fieldValue" :field-config="fieldConfig" @change="handleChange" />
</template>

<script setup lang="ts">
  import { FormCreateField } from '../../types';

  import inputNumber from '../basic/inputNumber.vue';
  const props = defineProps<{
    fieldConfig: FormCreateField;
    path: string;
    needInitDetail?: boolean; // 判断是否编辑情况
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
