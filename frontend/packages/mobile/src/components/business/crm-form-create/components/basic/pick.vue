<template>
  <van-field
    v-model:value="value"
    :label="props.fieldConfig.showLabel ? props.fieldConfig.name : ''"
    :name="props.fieldConfig.id"
    :rule="props.fieldConfig.rules"
    is-link
    readonly
    :placeholder="props.fieldConfig.placeholder"
    :disabled="props.fieldConfig.editable === false"
    clearable
    @click="showPicker = true"
    @update:model-value="($event) => emit('change', $event)"
  >
  </van-field>
  <van-popup v-model:show="showPicker" destroy-on-close round position="bottom">
    <van-picker
      :model-value="pickerValue"
      :columns="
        props.fieldConfig.options?.map((item) => ({
          ...item,
          text: item.name,
          value: item.id,
        }))
      "
      @cancel="showPicker = false"
      @confirm="onConfirm"
    />
  </van-popup>
</template>

<script setup lang="ts">
  import { FormCreateField } from '@cordys/web/src/components/business/crm-form-create/types';
  import { Numeric } from 'vant/es/utils';

  const props = defineProps<{
    fieldConfig: FormCreateField;
  }>();
  const emit = defineEmits<{
    (e: 'change', value: string): void;
  }>();

  const value = defineModel<string>('value', {
    default: '',
  });

  const showPicker = ref(false);
  const pickerValue = ref<Numeric[]>([]);

  watch(
    () => props.fieldConfig.defaultValue,
    (val) => {
      value.value = val;
    },
    {
      immediate: true,
    }
  );

  function onConfirm({ selectedValues, selectedOptions }: any) {
    showPicker.value = false;
    pickerValue.value = selectedValues;
    value.value = selectedOptions[0].text;
  }
</script>

<style lang="less" scoped></style>
