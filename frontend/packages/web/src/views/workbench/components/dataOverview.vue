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
          v-model:value="activeDeptId"
          :options="departmentOptions"
          label-field="name"
          key-field="id"
          filterable
          :render-switcher-icon="renderSwitcherIconDom"
          class="w-[240px]"
          children-field="children"
          @update:value="changeHandler"
        />
        <CrmTab
          v-model:active-tab="activePeriod"
          no-content
          :tab-list="tabList"
          type="segment"
          class="w-fit"
          @change="handleChangeTab"
        />
        <n-date-picker
          v-if="activePeriod === 'CUSTOM'"
          v-model:value="range"
          class="w-[240px]"
          type="datetimerange"
          :default-time="[undefined, '23:59:59']"
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
      <analyticsDetail ref="analyticsDetailRef" :search-type="params.searchType" />
      <analyticsMiniCard ref="analyticsMiniCardRef" />
      <div
        v-if="
          !hasAnyPermission(['CUSTOMER_MANAGEMENT:READ', 'OPPORTUNITY_MANAGEMENT:READ', 'CLUE_MANAGEMENT:READ']) ||
          !hasAnyModuleEnable
        "
        class="flex items-center justify-center p-[16px] text-[var(--text-n4)]"
      >
        {{
          !hasAnyModuleEnable &&
          hasAnyPermission(['CUSTOMER_MANAGEMENT:READ', 'OPPORTUNITY_MANAGEMENT:READ', 'CLUE_MANAGEMENT:READ'])
            ? t('workbench.duplicateCheck.moduleNotEnabled')
            : t('common.noPermission')
        }}
      </div>
    </div>
  </CrmCard>
</template>

<script setup lang="ts">
  import { NDatePicker, NTooltip, NTreeSelect, TreeOption } from 'naive-ui';

  import { ModuleConfigEnum } from '@lib/shared/enums/moduleEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { mapTree } from '@lib/shared/method';
  import { GetHomeStatisticParams } from '@lib/shared/models/home';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import type { CrmTreeNodeData } from '@/components/pure/crm-tree/type';
  import analyticsDetail from './analyticsDetail.vue';
  import analyticsMiniCard from './analyticsMiniCard.vue';

  import { getHomeDepartmentTree } from '@/api/modules';
  import { useAppStore, useUserStore } from '@/store';
  import { hasAnyPermission } from '@/utils/permission';

  const { t } = useI18n();
  const appStore = useAppStore();

  const activePeriod = ref('TODAY');
  const params = ref<GetHomeStatisticParams>({
    deptIds: [],
    searchType: '', // ALL/SELF/DEPARTMENT
    period: activePeriod.value,
    startTime: undefined,
    endTime: undefined,
  });

  const activeDeptId = ref('');

  const useStore = useUserStore();

  const tabList = [
    {
      name: 'TODAY',
      tab: t('workbench.today'),
    },
    {
      name: 'THIS_WEEK',
      tab: t('workbench.thisWeek'),
    },
    {
      name: 'THIS_MONTH',
      tab: t('workbench.thisMonth'),
    },
    {
      name: 'CUSTOM',
      tab: t('common.custom'),
    },
  ];
  const range = ref();

  const originDepartmentOptions = ref<CrmTreeNodeData[]>([]);
  const departmentOptions = ref<CrmTreeNodeData[]>([]);

  const analyticsDetailRef = ref<InstanceType<typeof analyticsDetail>>();
  const analyticsMiniCardRef = ref<InstanceType<typeof analyticsMiniCard>>();
  function getSpringIds(children: CrmTreeNodeData[] | undefined): string[] {
    const offspringIds: string[] = [];
    mapTree(children || [], (e) => {
      offspringIds.push(e.id);
      return e;
    });
    return offspringIds;
  }

  function setSearchType(isInit: boolean, activeId?: string) {
    if (isInit) {
      params.value.searchType = useStore.userInfo.roles.some((e: any) => e?.dataScope === 'SELF') ? 'SELF' : 'ALL';
      activeDeptId.value = params.value.searchType === 'SELF' ? 'SELF' : originDepartmentOptions.value[0]?.id;
      return;
    }

    if (activeId) {
      activeDeptId.value = activeId;
    }

    if (activeDeptId.value === 'SELF') {
      params.value.searchType = 'SELF';
    } else if (activeDeptId.value !== originDepartmentOptions.value[0]?.id) {
      params.value.searchType = 'DEPARTMENT';
    } else if (useStore.isAdmin || activeDeptId.value === originDepartmentOptions.value[0]?.id) {
      params.value.searchType = 'ALL';
    } else {
      params.value.searchType = useStore.userInfo.roles.some((e: any) => e?.dataScope === 'SELF') ? 'SELF' : 'ALL';
    }
  }

  function renderSwitcherIconDom(nodeSwitchProps: { option: CrmTreeNodeData; expanded: boolean; selected: boolean }) {
    const { option } = nodeSwitchProps;
    return h(CrmIcon, {
      size: 16,
      type: option.children?.length ? 'iconicon_caret_right_small' : '',
      class: `text-[var(--text-n2)]`,
    });
  }

  const hasAnyModuleEnable = computed(() =>
    appStore.moduleConfigList.some(
      (e) =>
        [
          ModuleConfigEnum.CUSTOMER_MANAGEMENT,
          ModuleConfigEnum.BUSINESS_MANAGEMENT,
          ModuleConfigEnum.CLUE_MANAGEMENT,
        ].includes(e.moduleKey as ModuleConfigEnum) && e.enable
    )
  );

  async function initDepartList() {
    try {
      const result = await getHomeDepartmentTree();
      originDepartmentOptions.value = result;
      departmentOptions.value = [
        {
          id: 'SELF',
          name: t('opportunity.mine'),
          organizationId: '100001',
        },
        ...result,
      ];
      setSearchType(true);
      if (params.value.searchType === 'ALL') {
        params.value.deptIds = getSpringIds(result);
      } else {
        params.value.deptIds = [];
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  function refresh() {
    nextTick(() => {
      analyticsDetailRef.value?.initHomeStatistic(params.value);
      analyticsMiniCardRef.value?.initStatisticDetail(params.value);
    });
  }

  async function changeHandler(
    value: string | number | Array<string | number> | null,
    option: Array<CrmTreeNodeData | null> | CrmTreeNodeData,
    _: { node: TreeOption | null; action: 'select' | 'unselect' | 'delete' | 'clear' }
  ) {
    activeDeptId.value = value as string;
    if (value !== 'SELF') {
      params.value.deptIds = [value as string, ...getSpringIds((option as CrmTreeNodeData).children)];
    } else {
      params.value.deptIds = [];
    }
    setSearchType(false, value as string);
    refresh();
  }

  function handleChangeTab(value: string | number) {
    if (value === 'CUSTOM') {
      params.value.period = undefined;
    } else {
      params.value.period = value as string;
      params.value.startTime = undefined;
      params.value.endTime = undefined;
      range.value = undefined;
      refresh();
    }
  }

  function confirmTimePicker(value: number | [number, number] | null, _: string | [string, string] | null) {
    range.value = value;
    const [startTime, endTime] = range.value || [0, 0];
    params.value.startTime = startTime;
    params.value.endTime = endTime;
    refresh();
  }

  watch(
    () => params.value.searchType,
    () => {
      refresh();
    }
  );

  onMounted(() => {
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
