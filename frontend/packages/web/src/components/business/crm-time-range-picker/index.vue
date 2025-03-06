<template>
  <n-date-picker v-if="isFixed" v-model:value="fixedValue" type="datetimerange" clearable />
  <div v-else>
    <div class="flex items-center gap-[8px]">
      <n-input-number v-model:value="dynamicValue[0]" class="flex-1" :min="1" />
      <n-select
        v-model:value="dynamicValue[1]"
        class="flex-1"
        :options="unitOptions"
        :placeholder="t('common.pleaseSelect')"
      />
      <div>{{ t('common.toPresent') }}</div>
    </div>
    <div class="text-[12px] text-[var(--primary-8)]">{{ formattedDateRange }}</div>
  </div>
</template>

<script setup lang="ts">
  import { NDatePicker, NInputNumber, NSelect } from 'naive-ui';
  import dayjs from 'dayjs';

  import { OperatorEnum } from '@lib/shared/enums/commonEnum';

  import { useI18n } from '@/hooks/useI18n';

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
      const parts = val.split(',');
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
      modelValue.value = `${value},${unit}`;
    },
    { deep: true }
  );

  watch(
    () => fixedValue.value,
    (val) => {
      modelValue.value = val ? `${val[0]},${val[1]}` : '';
    },
    { deep: true }
  );

  const unitOptions = [
    { label: t('common.month'), value: 'month' },
    { label: t('common.week'), value: 'week' },
    { label: t('common.day'), value: 'day' },
    { label: t('common.today'), value: 'today' },
    { label: t('common.monthAfter'), value: 'monthAfter' },
    { label: t('common.weekAfter'), value: 'weekAfter' },
    { label: t('common.dayAfter'), value: 'dayAfter' },
  ];

  function calculateDynamicDateRange(value: number, unitValue: string) {
    const now = dayjs();
    let startDate;
    let endDate;

    switch (unitValue) {
      case 'month':
        startDate = now.subtract(value, 'month');
        endDate = now;
        break;
      case 'week':
        startDate = now.subtract(value, 'week');
        endDate = now;
        break;
      case 'day':
        startDate = now.subtract(value, 'day');
        endDate = now;
        break;
      case 'today':
        startDate = now.startOf('day');
        endDate = now.endOf('day');
        break;
      case 'monthAfter':
        startDate = now;
        endDate = now.add(value, 'month');
        break;
      case 'weekAfter':
        startDate = now;
        endDate = now.add(value, 'week');
        break;
      case 'dayAfter':
        startDate = now;
        endDate = now.add(value, 'day');
        break;
      default:
        startDate = now;
        endDate = now;
    }
    return `${startDate.format('YYYY-MM-DD HH:mm:ss')} ${t('common.to')} ${endDate.format('YYYY-MM-DD HH:mm:ss')}`;
  }

  const formattedDateRange = computed(() => {
    // 只有在动态模式下才计算日期范围
    if (isFixed.value || (dynamicValue.value[0] === undefined && dynamicValue.value[1] !== 'today')) {
      return '';
    }
    return calculateDynamicDateRange(dynamicValue.value[0] as number, dynamicValue.value[1] as string);
  });
</script>
