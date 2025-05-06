<template>
  <van-field
    v-model="fieldValue"
    :label="props.fieldConfig.showLabel ? props.fieldConfig.name : ''"
    :name="props.fieldConfig.id"
    :rules="props.fieldConfig.rules as FieldRule[]"
    is-link
    readonly
    :placeholder="props.fieldConfig.placeholder || t('common.pleaseSelect')"
    :disabled="props.fieldConfig.editable === false"
    clearable
    @click="showPicker = true"
    @update:model-value="($event) => emit('change', $event)"
  >
  </van-field>
  <van-popup v-model:show="showPicker" destroy-on-close round position="bottom">
    <van-picker-group
      :title="t('formCreate.pickDate')"
      :tabs="[t('formCreate.pickDate'), t('formCreate.pickTime')]"
      :next-step-text="t('formCreate.next')"
      @confirm="onConfirm"
      @cancel="showPicker = false"
    >
      <van-date-picker v-model="currentDate" />
      <van-time-picker v-model="currentTime" :columns-type="['hour', 'minute', 'second']" />
    </van-picker-group>
  </van-popup>
</template>

<script setup lang="ts">
  import { FieldRule } from 'vant';
  import dayjs from 'dayjs';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import { FormCreateField } from '@cordys/web/src/components/business/crm-form-create/types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
  }>();
  const emit = defineEmits<{
    (e: 'change', value: number): void;
  }>();

  const { t } = useI18n();

  const value = defineModel<number>('value', {
    default: null,
  });
  const fieldValue = computed(() => {
    if (!value.value) {
      return undefined;
    }
    if (props.fieldConfig.dateType === 'datetime') {
      return dayjs(value.value).format('YYYY-MM-DD HH:mm:ss');
    }
    if (props.fieldConfig.dateType === 'month') {
      return dayjs(value.value).format('YYYY-MM');
    }
    return dayjs(value.value).format('YYYY-MM-DD');
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
    value.value = dayjs(`${currentDate.value.join('-')} ${currentTime.value.join(':')}`).unix() * 1000;
    emit('change', value.value);
  }
</script>

<style lang="less" scoped></style>
