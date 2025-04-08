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
    <van-picker-group title="选择日期" :tabs="['选择日期', '选择时间']" next-step-text="下一步" @confirm="onConfirm">
      <van-date-picker v-model="currentDate" />
      <van-time-picker v-model="currentTime" />
    </van-picker-group>
  </van-popup>
</template>

<script setup lang="ts">
  import dayjs from 'dayjs';

  import { FormCreateField } from '@cordys/web/src/components/business/crm-form-create/types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
  }>();
  const emit = defineEmits<{
    (e: 'change', value: number): void;
  }>();

  const value = defineModel<number>('value', {
    default: null,
  });

  const showPicker = ref(false);
  const currentDate = ref<string[]>([]);
  const currentTime = ref<string[]>([]);

  watch(
    () => props.fieldConfig.defaultValue,
    (val) => {
      value.value = val;
      if (val) {
        const date = dayjs(val);
        currentDate.value = date.format('YYYY-MM-DD').split('-');
        currentTime.value = date.format('HH:mm:ss').split(':');
      } else {
        currentDate.value = [];
        currentTime.value = [];
      }
    },
    {
      immediate: true,
    }
  );

  function onConfirm() {
    showPicker.value = false;
    value.value = dayjs(`${currentDate.value.join('-')} ${currentTime.value.join('-')}`).unix() * 1000;
    emit('change', value.value);
  }
</script>

<style lang="less" scoped></style>
