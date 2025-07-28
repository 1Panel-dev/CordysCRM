<template>
  <div v-if="isDynamics" class="w-full">
    <div class="flex items-center gap-[8px]">
      <n-select
        v-model:value="dynamicValue[0]"
        class="flex-1"
        :disabled="props.disabled"
        :options="timeOptions"
        :placeholder="t('common.pleaseSelect')"
      />
      <CrmInputNumber
        v-if="dynamicValue[0] === 'CUSTOM'"
        v-model:value="dynamicValue[1]"
        max="10000"
        :disabled="props.disabled"
        :precision="0"
        class="flex-1"
        :min="1"
      />
      <n-select
        v-if="dynamicValue[0] === 'CUSTOM'"
        v-model:value="dynamicValue[2]"
        class="flex-1"
        :disabled="props.disabled"
        :options="unitOptions"
        :placeholder="t('common.pleaseSelect')"
      />
    </div>
    <div class="text-[12px] text-[var(--primary-8)]">{{ formattedDateRange }}</div>
  </div>
  <n-date-picker
    v-else
    v-model:value="fixedValue"
    class="w-full"
    :disabled="props.disabled"
    type="datetimerange"
    clearable
    :default-time="[undefined, '23:59:59']"
  />
</template>

<script setup lang="ts">
  import { computed, ref, watch } from 'vue';
  import { NDatePicker, NSelect } from 'naive-ui';
  import dayjs from 'dayjs';

  import { OperatorEnum } from '@lib/shared/enums/commonEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmInputNumber from '@/components/pure/crm-input-number/index.vue';

  import { timeOptions, unitOptions } from './config';

  const props = defineProps<{
    timeRangeType?: OperatorEnum;
    disabled?: boolean;
  }>();

  const { t } = useI18n();

  const modelValue = defineModel<string>('value', { default: '' });

  const isDynamics = computed(() => props.timeRangeType === OperatorEnum.DYNAMICS);

  // 固定模式下的数据：一个日期范围（两个时间戳）
  const fixedValue = ref<[number, number] | undefined>(undefined);
  const defaultTime = 'WEEK';
  const defaultNum = 6;
  const defaultUnit = 'BEFORE_DAY';

  // 动态时间段（如：本周、过去7天、自定义等）
  const dynamicValue = ref<[string, number | undefined, string | undefined]>([defaultTime, defaultNum, defaultUnit]);

  watch(
    () => modelValue.value,
    (val) => {
      let parts: any[] = [];
      if (val) {
        parts = val.split(',');
      }
      if (isDynamics.value) {
        const type = parts[0] ?? defaultTime;

        if (type === 'CUSTOM') {
          dynamicValue.value = ['CUSTOM', !Number(parts[1]) ? undefined : Number(parts[1]), parts[2]];
        } else {
          dynamicValue.value = [type, undefined, undefined];
        }
      } else {
        const start = Number(parts[0]);
        const end = Number(parts[1]);
        fixedValue.value = start && end ? [start, end] : undefined;
      }
    },
    { immediate: true }
  );

  watch(
    () => dynamicValue.value[0],
    () => {
      if (
        isDynamics.value &&
        dynamicValue.value[0] === 'CUSTOM' &&
        dynamicValue.value[1] === undefined &&
        dynamicValue.value[2] === undefined
      ) {
        modelValue.value = `CUSTOM,${defaultNum},${defaultUnit}`;
      }
    }
  );

  watch(
    () => dynamicValue.value,
    ([type, value, unit]) => {
      if (isDynamics.value) {
        if (type === 'CUSTOM') {
          modelValue.value = `CUSTOM,${value},${unit}`;
        } else {
          modelValue.value = type;
        }
      }
    },
    { deep: true, immediate: true }
  );

  watch(
    () => fixedValue.value,
    (val) => {
      if (!isDynamics.value) {
        modelValue.value = val ? `${val[0]},${val[1]}` : '';
      }
    },
    { deep: true }
  );

  watch(
    () => props.timeRangeType,
    () => {
      if (isDynamics.value && !modelValue.value) {
        modelValue.value = defaultTime;
      }
    }
  );

  function calculateDynamicDateRange(value: number, unitValue: string) {
    const DATE_FORMAT = 'YYYY-MM-DD HH:mm:ss';
    const now = dayjs();
    let startDate: dayjs.Dayjs;
    let endDate: dayjs.Dayjs;

    switch (unitValue) {
      case 'BEFORE_DAY':
        startDate = now.subtract(value, 'day');
        endDate = now;
        break;
      case 'AFTER_DAY':
        startDate = now;
        endDate = now.add(value, 'day');
        break;
      case 'BEFORE_WEEK':
        startDate = now.subtract(value, 'week');
        endDate = now;
        break;
      case 'AFTER_WEEK':
        startDate = now;
        endDate = now.add(value, 'week');
        break;
      case 'BEFORE_MONTH':
        startDate = now.subtract(value, 'month');
        endDate = now;
        break;
      case 'AFTER_MONTH':
        startDate = now;
        endDate = now.add(value, 'month');
        break;
      default:
        return '';
    }

    return `${startDate.format(DATE_FORMAT)} ~ ${endDate.format(DATE_FORMAT)}`;
  }

  const formattedDateRange = computed(() => {
    // 只有在动态模式下才计算日期范围
    if (!isDynamics.value || dynamicValue.value[1] === undefined || dynamicValue.value[2] === undefined) return '';
    return calculateDynamicDateRange(dynamicValue.value[1], dynamicValue.value[2]);
  });
</script>
