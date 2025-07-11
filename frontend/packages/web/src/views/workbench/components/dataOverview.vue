<template>
  <CrmCard hide-footer auto-height class="mb-[16px]">
    <div class="analytics-title">
      <div class="analytics-name flex items-center">
        {{ t('workbench.dataOverView') }}
        <n-tooltip trigger="hover" placement="right">
          <template #trigger>
            <CrmIcon
              type="iconicon_help_circle"
              class="ml-[8px] cursor-pointer text-[var(--text-n4)] hover:text-[var(--primary-1)]"
            />
          </template>
          {{ t('workbench.dataOverviewPlaceholderTip') }}
        </n-tooltip>
      </div>
      <div class="flex items-center gap-[12px]">
        <n-tree-select
          v-model:value="params.departmentId"
          :options="departmentOptions"
          label-field="name"
          key-field="id"
          filterable
          clearable
          class="w-[240px]"
          children-field="children"
          :placeholder="t('common.all')"
        />
        <CrmTab v-model:active-tab="activeTab" no-content :tab-list="tabList" type="segment" class="w-fit" />
        <n-date-picker
          v-if="activeTab === 'custom'"
          v-model:value="range"
          class="w-[240px]"
          type="datetimerange"
          @confirm="confirmTimePicker"
        >
          <template #date-icon>
            <CrmIcon class="text-[var(--text-n4)]" type="iconicon_time" :size="16" />
          </template>
          <template #separator>
            <div class="text-[var(--text-n4)]">{{ t('common.to') }}</div>
          </template>
        </n-date-picker>
      </div>
    </div>
    <div class="flex flex-col gap-[16px]">
      <analyticsDetail />
      <analyticsMiniCard />
    </div>
  </CrmCard>
</template>

<script setup lang="ts">
  import { NDatePicker, NTooltip, NTreeSelect } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import type { CrmTreeNodeData } from '@/components/pure/crm-tree/type';
  import analyticsDetail from './analyticsDetail.vue';
  import analyticsMiniCard from './analyticsMiniCard.vue';

  import { getDepartmentTree } from '@/api/modules';

  const { t } = useI18n();

  const params = ref({
    departmentId: '',
  });

  const activeTab = ref('today');
  const tabList = [
    {
      name: 'today',
      tab: t('workbench.today'),
    },
    {
      name: '7',
      tab: t('workbench.thisWeek'),
    },
    {
      name: '30',
      tab: t('workbench.thisMonth'),
    },
    {
      name: 'custom',
      tab: t('common.custom'),
    },
  ];
  const range = ref();

  function confirmTimePicker(value: number | [number, number] | null, _: string | [string, string] | null) {
    range.value = value;
  }

  const departmentOptions = ref<CrmTreeNodeData[]>([]);

  async function initDepartList() {
    try {
      departmentOptions.value = await getDepartmentTree();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  onBeforeMount(() => {
    initDepartList();
  });
</script>

<style lang="less" scoped>
  .analytics-title {
    display: flex;
    justify-content: space-between;
    margin-bottom: 16px;
    .analytics-name {
      font-weight: 600;
    }
  }
</style>

<style lang="less">
  .analytics-icon {
    @apply flex items-center justify-center;

    width: 24px;
    height: 24px;
    border-radius: 50%;
  }
  .analytics-count {
    font-size: 18px;
    line-height: 26px;
    @apply font-semibold;
  }
  .analytics-last-time {
    @apply flex flex-nowrap items-center justify-between;

    font-size: 12px;
    .last-time-rate-up {
      color: var(--error-red);
    }
    .last-time-rate-down {
      color: var(--success-green);
    }
  }
</style>
