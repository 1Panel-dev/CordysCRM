<template>
  <CrmCard hide-footer auto-height class="mb-[16px]">
    <div class="analytics-title">
      <div class="analytics-name flex items-center">
        {{ t('workbench.dataOverView') }}
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
        <n-popover
          v-model:show="popoverVisible"
          trigger="click"
          placement="bottom-end"
          class="crm-table-column-setting-popover"
          @update:show="handleUpdateShow"
        >
          <template #trigger>
            <n-button
              :ghost="popoverVisible"
              :type="popoverVisible ? 'primary' : 'default'"
              class="outline--secondary px-[8px]"
            >
              <CrmIcon
                type="iconicon_set_up"
                :class="`cursor-pointer ${popoverVisible ? 'text-[var(--primary-8)]' : ''}`"
                :size="16"
              />
            </n-button>
          </template>
          <div class="mb-[4px] flex flex-col gap-[8px] p-[8px]">
            <div class="mb-[4px]">
              <n-radio-group v-model:value="winOrderType" name="layoutType">
                <n-radio-button v-for="e in winOrderTypeList" :key="e.value" :value="e.value" :label="e.label" />
              </n-radio-group>
            </div>
            <div v-if="winOrderType === 'winOrderSet'" class="flex flex-col gap-[8px]">
              <div class="flex items-center justify-between px-[8px]">
                <div>{{ t('workbench.dataOverview.comparedSamePeriod') }}</div>
                <n-switch
                  v-model:value="params.priorPeriodEnable"
                  size="small"
                  :rubber-band="false"
                  @update:value="handleChange"
                />
              </div>
              <div class="flex flex-col gap-[8px] px-[8px]">
                <div>{{ t('workbench.dataOverview.statisticalDimension') }}</div>
                <n-select
                  v-model:value="params.winOrderTimeField"
                  :options="winOrderTypeOptions"
                  :placeholder="t('common.pleaseSelect')"
                  @update:value="handleChange"
                />
              </div>
            </div>

            <div v-if="winOrderType === 'optSet'" class="flex flex-col gap-[8px] px-[8px]">
              <div>{{ t('workbench.dataOverview.statisticalDimension') }}</div>
              <n-select
                v-model:value="params.timeField"
                :options="dimTypeOptions"
                :placeholder="t('common.pleaseSelect')"
                @update:value="handleChange"
              />
            </div>
            <div v-if="winOrderType === 'clueSet'" class="flex flex-col gap-[8px] px-[8px]">
              <div class="flex items-center">
                <div>{{ t('workbench.dataOverview.statisticalDimension') }}</div>
                <n-tooltip>
                  <template #trigger>
                    <CrmIcon
                      type="iconicon_info_circle_filled"
                      :size="16"
                      class="ml-[4px] cursor-pointer text-[var(--text-n4)]"
                    />
                  </template>
                  <div>{{ t('workbench.dataOverview.statisticalDimensionTip1') }}</div>
                  <div>{{ t('workbench.dataOverview.statisticalDimensionTip2') }}</div>
                </n-tooltip>
              </div>
              <n-select
                v-model:value="params.userField"
                :options="userFieldOptions"
                :placeholder="t('common.pleaseSelect')"
                @update:value="handleChange"
              />
            </div>
          </div>
        </n-popover>
        <n-button type="default" class="outline--secondary px-[8px]" @click="refresh">
          <CrmIcon class="text-[var(--text-n1)]" type="iconicon_refresh" :size="16" />
        </n-button>
      </div>
    </div>
    <n-scrollbar x-scrollable>
      <overview ref="overviewRef" :params="params" />
    </n-scrollbar>
  </CrmCard>
</template>

<script setup lang="ts">
  import {
    NButton,
    NPopover,
    NRadioButton,
    NRadioGroup,
    NScrollbar,
    NSelect,
    NSwitch,
    NTooltip,
    NTreeSelect,
    SelectOption,
    TreeOption,
  } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { mapTree } from '@lib/shared/method';
  import { GetHomeStatisticParams } from '@lib/shared/models/home';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import type { CrmTreeNodeData } from '@/components/pure/crm-tree/type';
  import { getFormConfigApiMap } from '@/components/business/crm-form-create/config';
  import overview from './overview.vue';

  import { getHomeDepartmentTree } from '@/api/modules';
  import { useUserStore } from '@/store';
  import useOverviewStore from '@/store/modules/overview';
  import { hasAnyPermission } from '@/utils/permission';

  const { t } = useI18n();
  const overviewStore = useOverviewStore();
  const params = ref<GetHomeStatisticParams>({
    deptIds: [],
    searchType: '', // ALL/SELF/DEPARTMENT
    timeField: 'CREATE_TIME',
    userField: 'CREATE_USER_ID',
    priorPeriodEnable: true,
    winOrderTimeField: 'EXPECTED_END_TIME',
  });

  const activeDeptId = ref('');

  const useStore = useUserStore();

  const originDepartmentOptions = ref<CrmTreeNodeData[]>([]);
  const departmentOptions = ref<CrmTreeNodeData[]>([]);

  function getSpringIds(children: CrmTreeNodeData[] | undefined): string[] {
    const offspringIds: string[] = [];
    mapTree(children || [], (e) => {
      offspringIds.push(e.id);
      return e;
    });
    return offspringIds;
  }

  function setSearchType(isInit: boolean, activeId?: string) {
    const firstDeptId = originDepartmentOptions.value[0]?.id;
    const searchType = originDepartmentOptions.value.length > 1 ? 'DEPARTMENT' : 'ALL';

    if (isInit) {
      params.value.searchType = originDepartmentOptions.value.length === 0 ? 'SELF' : searchType;
      activeDeptId.value = params.value.searchType === 'SELF' ? 'SELF' : firstDeptId;
      return;
    }

    if (activeId) {
      activeDeptId.value = activeId;
    }

    if (activeDeptId.value === 'SELF') {
      params.value.searchType = 'SELF';
    } else if (activeDeptId.value === firstDeptId) {
      params.value.searchType = useStore.isAdmin ? 'ALL' : searchType;
    } else if (activeDeptId.value) {
      params.value.searchType = 'DEPARTMENT';
    } else {
      params.value.searchType = originDepartmentOptions.value.length === 0 ? 'SELF' : searchType;
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
      } else if (params.value.searchType === 'DEPARTMENT') {
        params.value.deptIds = [activeDeptId.value, ...getSpringIds(originDepartmentOptions.value[0]?.children ?? [])];
      } else {
        params.value.deptIds = [];
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }
  const overviewRef = ref<InstanceType<typeof overview>>();

  function refresh() {
    nextTick(() => {
      overviewRef.value?.initHomeStatistic(params.value);
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

  watch(
    () => params.value.searchType,
    () => {
      refresh();
    }
  );

  const winOrderType = ref('clueSet');
  const winOrderTypeList = [
    {
      value: 'clueSet',
      label: t('workbench.dataOverview.clueSet'),
    },
    {
      value: 'optSet',
      label: t('workbench.dataOverview.opportunitySet'),
    },
    {
      value: 'winOrderSet',
      label: t('workbench.dataOverview.windOrderConfig'),
    },
  ];

  const userFieldOptions = [
    {
      value: 'CREATE_USER',
      label: t('common.creator'),
    },
    {
      value: 'OWNER',
      label: t('common.head'),
    },
  ];

  const endTimeFieldName = ref(t('opportunity.endTime'));

  const dimTypeOptions = computed(() => {
    return [
      {
        value: 'CREATE_TIME',
        label: t('common.createTime'),
      },
      {
        value: 'EXPECTED_END_TIME',
        label: endTimeFieldName.value,
      },
    ];
  });

  const winOrderTypeOptions = computed(() => {
    return [
      {
        value: 'EXPECTED_END_TIME',
        label: endTimeFieldName.value,
      },
      {
        value: 'ACTUAL_END_TIME',
        label: t('opportunity.actualEndTime'),
      },
    ];
  });
  async function initDimType() {
    const config = await overviewStore.loadWinOrderConfig();
    const { timeField, priorPeriodEnable, userField, winOrderTimeField } = config;
    params.value = {
      ...params.value,
      timeField,
      priorPeriodEnable,
      userField,
      winOrderTimeField,
    };

    endTimeFieldName.value = t('opportunity.endTime');

    if (!hasAnyPermission(['OPPORTUNITY_MANAGEMENT:READ'])) return;
    try {
      const res = await getFormConfigApiMap[FormDesignKeyEnum.BUSINESS]();
      endTimeFieldName.value =
        res.fields.find((e) => e.businessKey === 'expectedEndTime')?.name ?? t('opportunity.endTime');
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  const popoverVisible = ref(false);
  const hasChange = ref(false);
  async function handleUpdateShow(show: boolean) {
    if (!show) {
      if (hasChange.value) {
        const { timeField, priorPeriodEnable, userField, winOrderTimeField } = params.value;
        overviewStore.setWinOrderConfig({
          timeField: timeField ?? 'CREATE_TIME',
          priorPeriodEnable: priorPeriodEnable ?? false,
          userField: userField ?? 'OWNER',
          winOrderTimeField: winOrderTimeField ?? 'EXPECTED_END_TIME',
        });
        refresh();
      }
    } else {
      const config = await overviewStore.loadWinOrderConfig();
      const { timeField, priorPeriodEnable, userField, winOrderTimeField } = config;
      params.value = {
        ...params.value,
        timeField,
        priorPeriodEnable,
        userField,
        winOrderTimeField,
      };
    }
  }

  function handleChange() {
    hasChange.value = true;
  }

  onMounted(async () => {
    await initDimType();
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
  .win-order-config-popover {
    padding: 0 !important;
  }
</style>
