<template>
  <div class="flex w-full items-center">
    <CrmInputNumber
      v-model:value="endTimeValue[0]"
      max="10000"
      :disabled="props.disabled"
      :precision="0"
      class="flex-1"
      :min="1"
    />
    <n-select
      v-model:value="endTimeValue[1]"
      class="end-time-unit-select w-[100px]"
      :placeholder="t('common.pleaseSelect')"
      :disabled="props.disabled"
      :options="unitOptions"
      :show="false"
    />
    <div class="ml-[8px] text-[var(--text-n2)]">
      {{ t('system.message.expirationReminder') }}
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NSelect } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmInputNumber from '@/components/pure/crm-input-number/index.vue';

  const props = defineProps<{
    disabled?: boolean;
  }>();

  const { t } = useI18n();

  const defaultTime = undefined;
  const defaultUnit = 'DAY';

  const endTimeValue = ref<[number | undefined, string | null]>([defaultTime, defaultUnit]);

  const modelValue = defineModel<string>('value', { default: '' });

  const unitOptions = ref([{ label: t('common.dayUnit'), value: 'DAY' }]);

  watch(
    () => modelValue.value,
    (val) => {
      let parts: any[] = [];
      if (val) {
        parts = val.split(',');
      }
      endTimeValue.value = [Number.isNaN(Number(parts[0])) ? defaultTime : Number(parts[0]), parts[1] ?? defaultUnit];
    },
    { immediate: true }
  );

  watch(
    () => endTimeValue.value[0],
    (val) => {
      modelValue.value = `${val},${defaultUnit}`;
    }
  );
</script>

<style lang="less">
  .n-select {
    &.end-time-unit-select {
      margin-left: -1px;
      .n-base-selection {
        border-radius: 0 3px 3px 0 !important;
      }
    }
  }
</style>
