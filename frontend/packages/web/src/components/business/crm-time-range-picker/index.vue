<template>
  <n-date-picker
    v-if="isFixed"
    v-model:value="fixedValue"
    class="w-full"
    type="datetimerange"
    clearable
    :default-time="[undefined, '23:59:59']"
  />
  <div v-else class="w-full">
    <div class="flex items-center gap-[8px]">
      <CrmInputNumber v-model:value="dynamicValue[0]" max="10000" :precision="0" class="flex-1" :min="1" />
      <n-select
        v-model:value="dynamicValue[1]"
        class="flex-1"
        :options="unitOptions"
        :placeholder="t('common.pleaseSelect')"
      />
    </div>
    <div class="text-[12px] text-[var(--primary-8)]">{{ formattedDateRange }}</div>
  </div>
</template>

<script setup lang="ts">
  import { NDatePicker, NSelect } from 'naive-ui';
  import dayjs, { ManipulateType } from 'dayjs';

  import { OperatorEnum } from '@lib/shared/enums/commonEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmInputNumber from '@/components/pure/crm-input-number/index.vue';

  const props = defineProps<{
    timeRangeType?: OperatorEnum.FIXED | OperatorEnum.DYNAMICS;
  }>();

  const { t } = useI18n();

  const modelValue = defineModel<string>('value', { default: '' });

  // 是否固定模式
  const isFixed = computed(() => props.timeRangeType === OperatorEnum.FIXED);

  // 固定模式下的数据：一个日期范围（两个时间戳）
  const fixedValue = ref<[number, number] | undefined>(undefined);

  // 动态模式下的数据：数值和单位的组合
  const dynamicValue = ref<[number | undefined, string]>([6, 'month']);

  watch(
    () => modelValue.value,
    (val) => {
      let parts: any[] = [];
      if (val) {
        parts = val.split(',');
      }
      if (!isFixed.value) {
        if (val) {
          if (!Number(parts[1])) {
            dynamicValue.value = [!Number(parts[0]) ? undefined : Number(parts[0]), parts[1]];
            return;
          }
        }
        dynamicValue.value = [6, 'month']; // 默认值
      } else {
        const start = Number(parts[0]);
        const end = Number(parts[1]);
        fixedValue.value = start && end ? [start, end] : undefined;
      }
    },
    { immediate: true }
  );

  watch(
    () => dynamicValue.value,
    ([value, unit]) => {
      if (!isFixed.value) {
        modelValue.value = `${value},${unit}`;
      }
    },
    { deep: true, immediate: true }
  );

  watch(
    () => fixedValue.value,
    (val) => {
      modelValue.value = val ? `${val[0]},${val[1]}` : '';
    },
    { deep: true }
  );

  watch(
    () => props.timeRangeType,
    () => {
      if (!isFixed.value && !modelValue.value) {
        modelValue.value = '6,month';
      }
    }
  );

  const unitOptions = [
    { label: t('common.month'), value: 'month' },
    { label: t('common.week'), value: 'week' },
    { label: t('common.day'), value: 'day' },
  ];

  function calculateDynamicDateRange(value: number, unitValue: string = 'month') {
    const DATE_FORMAT = 'YYYY-MM-DD HH:mm:ss';
    const now = dayjs();
    const startDate = now.subtract(value, unitValue as ManipulateType);
    return startDate.format(DATE_FORMAT);
  }

  const formattedDateRange = computed(() => {
    // 只有在动态模式下才计算日期范围
    if (isFixed.value || (dynamicValue.value[0] === undefined && dynamicValue.value[1] !== 'today')) {
      return '';
    }
    return calculateDynamicDateRange(dynamicValue.value[0] as number, dynamicValue.value[1] as string);
  });
</script>
