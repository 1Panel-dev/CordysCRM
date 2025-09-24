<template>
  <n-spin :show="loading" class="w-full min-w-[1200px]">
    <div class="overview overflow-hidden">
      <div class="header header-category pl-[16px] text-left font-semibold text-[var(--text-n1)]">
        {{ t('workbench.dataOverview.category') }}
      </div>
      <div class="category-time w-full">
        <div v-for="(item, index) of headerList" :key="`${item.icon}-${index}`" class="header">
          <div class="header-time ml-[16px] flex items-center justify-start gap-[8px]">
            <CrmIcon :type="item.icon" :size="24" class="text-[var(--info-blue)]" />
            <div class="font-semibold text-[var(--text-n1)]">{{ item.title }}</div>
          </div>
        </div>
      </div>
      <!-- 概览线索 -->
      <categoryCard
        :title="t('workbench.dataOverview.lead')"
        width="159px"
        icon="iconicon_facial_clue"
        bg-color="var(--info-2)"
      />
      <div class="right-cell">
        <div v-for="(dim, i) of dateKey" :key="`iconicon_facial_clue-${i}`" class="cell">
          <div class="cell-label">{{ t('workbench.dataOverview.newCreateTitle') }}</div>
          <countPopover :value="defaultLeadData[dim].value" :unit="defaultLeadData[dim].unit">
            <div
              :class="`cell-value count ${
                hasLeadPermission && params.userField === 'OWNER'
                  ? 'cursor-pointer text-[var(--primary-8)]'
                  : 'text-[var(--text-n4)]'
              }`"
              @click="goDetail(dim, defaultLeadData[dim])"
            >
              {{ abbreviateNumber(defaultLeadData[dim].value, defaultLeadData[dim].unit).value }}
              <span class="unit">
                {{ abbreviateNumber(defaultLeadData[dim].value, defaultLeadData[dim].unit).unit }}
              </span>
            </div>
          </countPopover>
        </div>
      </div>
      <categoryCard
        :title="t('workbench.dataOverview.opportunity')"
        width="138px"
        icon="iconicon_facial_business"
        bg-color="var(--info-1)"
      />
      <div class="right-cell">
        <div v-for="(dim, i) of dateKey" :key="`iconicon_facial_business-${i}`" class="cell">
          <div
            v-for="item in defaultOpportunityData[dim]"
            :key="`iconicon_facial_business-${item.title}`"
            class="flex-1"
          >
            <div class="cell-label">{{ item.title }}</div>
            <countPopover :value="item.value" :unit="item.unit">
              <div
                :class="`count ${
                  hasOptPermission ? 'cursor-pointer text-[var(--primary-8)]' : 'text-[var(--text-n4)]'
                }`"
                @click="goDetail(dim, item)"
              >
                {{ abbreviateNumber(item.value, item.unit).value }}
                <span class="unit">{{ abbreviateNumber(item.value, item.unit).unit }}</span>
              </div>
            </countPopover>
          </div>
        </div>
      </div>
      <categoryCard
        :title="t('workbench.dataOverview.winOrder')"
        width="116px"
        icon="iconicon_facial_deal_win"
        bg-color="var(--info-blue)"
      />
      <div class="right-cell">
        <div v-for="(dim, index) of dateKey" :key="`iconicon_facial_deal_win-${index}`" class="cell overflow-hidden">
          <div v-for="item in defaultWinOrderData[dim]" :key="`iconicon_facial_deal_win-${item.title}`" class="flex-1">
            <div class="cell-label">{{ item?.title }}</div>
            <countPopover :value="item.value" :unit="item.unit">
              <div
                :class="`count ${
                  hasOptPermission ? ' cursor-pointer  text-[var(--primary-8)]' : 'text-[var(--text-n4)]'
                }`"
                @click="goDetail(dim, item)"
              >
                {{ abbreviateNumber(item.value, item.unit).value }}
                <span class="unit">{{ abbreviateNumber(item.value, item.unit).unit }}</span>
              </div>
            </countPopover>
            <div v-if="appStore.getWinOrderStatus.status" class="analytics-last-time">
              <div class="one-line-text text-[var(--text-n2)]">
                {{ dateKeyPriorPeriodTitleMap[dim] }}
              </div>
              <CrmIcon
                v-if="
                  item.priorPeriodCompareRate &&
                  typeof item.priorPeriodCompareRate === 'number' &&
                  item.priorPeriodCompareRate !== 0
                "
                :type="item.priorPeriodCompareRate > 0 ? 'iconicon_caret_up_small' : 'iconicon_caret_down_small'"
                :class="getPriorPeriodClass(item.priorPeriodCompareRate)"
              />
              <div :class="`${getPriorPeriodClass(item.priorPeriodCompareRate)} flex min-w-0`">
                <div class="one-line-text">{{ periodCompareRateAbs(item.priorPeriodCompareRate) }}</div>
                <span v-if="typeof item.priorPeriodCompareRate === 'number'">％</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </n-spin>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NSpin } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { abbreviateNumber, getGenerateId } from '@lib/shared/method';
  import { setSessionStorageTempState } from '@lib/shared/method/local-storage';
  import {
    FollowOptStatisticDetail,
    GetHomeStatisticParams,
    HomeLeadStatisticDetail,
    HomeWinOrderDetail,
  } from '@lib/shared/models/home';

  import categoryCard from './categoryCard.vue';
  import countPopover from './countPopover.vue';

  import { getHomeFollowOpportunity, getHomeLeadStatistic, getHomeSuccessOptStatistic } from '@/api/modules';
  import useOpenNewPage from '@/hooks/useOpenNewPage';
  import { useAppStore } from '@/store';
  import { hasAnyPermission } from '@/utils/permission';

  import { AppRouteEnum } from '@/enums/routeEnum';

  const appStore = useAppStore();

  const { openNewPage } = useOpenNewPage();

  const { t } = useI18n();

  const props = defineProps<{
    params: GetHomeStatisticParams;
  }>();

  const headerList = [
    {
      title: t('workbench.dataOverview.today'),
      icon: 'iconicon_jin',
      key: 'today',
    },
    {
      title: t('workbench.dataOverview.thisWeek'),
      icon: 'iconicon_7',
      key: 'week',
    },
    {
      title: t('workbench.dataOverview.thisMonth'),
      icon: 'iconicon_30',
      key: 'month',
    },
    {
      title: t('workbench.dataOverview.thisYear'),
      icon: 'iconicon_365',
      key: 'year',
    },
  ];

  const dateKey = computed<string[]>(() => headerList.map((e) => e.key));

  const dateKeyPriorPeriodTitleMap: Record<string, string> = {
    today: t('workbench.dataOverview.comparedYesterday'),
    week: t('workbench.dataOverview.comparedLastWeek'),
    month: t('workbench.dataOverview.comparedLastMonth'),
    year: t('workbench.dataOverview.comparedLastYear'),
  };

  const hasLeadPermission = computed(() => hasAnyPermission(['CLUE_MANAGEMENT:READ']));
  const hasOptPermission = computed(() => hasAnyPermission(['OPPORTUNITY_MANAGEMENT:READ']));

  const defaultLeadData = ref<Record<string, any>>({});

  const createLeadBlock = () => ({
    value: hasLeadPermission.value ? 0 : '-',
    priorPeriodCompareRate: hasLeadPermission.value ? 0 : '-',
    routeName: AppRouteEnum.CLUE_MANAGEMENT_CLUE,
    hasPermission: hasLeadPermission.value,
    unit: t('workbench.dataOverview.countUnit'),
  });

  const defaultOpportunityData = ref<Record<string, Record<string, any>>>({});
  const createOpportunityBlock = () => ({
    newOpportunity: {
      title: t('workbench.dataOverview.followingOrder'),
      value: hasOptPermission.value ? 0 : '-',
      priorPeriodCompareRate: hasOptPermission.value ? 0 : '-',
      routeName: AppRouteEnum.OPPORTUNITY_OPT,
      status: 'FOLLOWING',
      hasPermission: hasOptPermission.value,
      unit: t('workbench.dataOverview.countUnit'),
    },
    newOpportunityAmount: {
      title: t('workbench.dataOverview.amount'),
      value: hasOptPermission.value ? 0 : '-',
      priorPeriodCompareRate: hasOptPermission.value ? 0 : '-',
      routeName: AppRouteEnum.OPPORTUNITY_OPT,
      status: 'FOLLOWING',
      hasPermission: hasOptPermission.value,
      unit: t('workbench.dataOverview.amountUnit'),
    },
  });

  const defaultWinOrderData = ref<Record<string, any>>({});
  const createWinOrderBlock = () => ({
    successOpportunity: {
      title: t('workbench.dataOverview.winOrderUnit'),
      value: hasOptPermission.value ? 0 : '-',
      priorPeriodCompareRate: hasOptPermission.value ? 0 : '-',
      routeName: AppRouteEnum.OPPORTUNITY_OPT,
      status: 'SUCCESS',
      hasPermission: hasOptPermission.value,
      unit: t('workbench.dataOverview.winUnit'),
    },
    successOpportunityAmount: {
      title: t('workbench.dataOverview.winAmount'),
      value: hasOptPermission.value ? 0 : '-',
      priorPeriodCompareRate: hasOptPermission.value ? 0 : '-',
      routeName: AppRouteEnum.OPPORTUNITY_OPT,
      status: 'SUCCESS',
      hasPermission: hasOptPermission.value,
      unit: t('workbench.dataOverview.amountUnit'),
    },
  });

  const getPriorPeriodClass = (priorPeriodCompareRate?: number) => {
    if (priorPeriodCompareRate && typeof priorPeriodCompareRate === 'number' && priorPeriodCompareRate !== 0) {
      return priorPeriodCompareRate > 0 ? 'last-time-rate-up flex-nowrap' : 'last-time-rate-down flex-nowrap';
    }
    if (priorPeriodCompareRate === 0 || typeof priorPeriodCompareRate === 'string') {
      return 'text-[var(--text-n2)] inline-flex flex-nowrap';
    }
  };

  const periodCompareRateAbs = (priorPeriodCompareRate?: number) => {
    if (typeof priorPeriodCompareRate !== 'number' || Number.isNaN(priorPeriodCompareRate)) return '-';
    return Math.abs(Number(priorPeriodCompareRate.toFixed(2)));
  };

  function goDetail(dim: string, item: Record<string, any>) {
    const { searchType, deptIds, timeField, userField } = props.params;
    if (!item.hasPermission || (userField === 'CREATE_USER' && item.routeName === AppRouteEnum.CLUE_MANAGEMENT_CLUE))
      return;
    const homeKey = 'homeData';
    sessionStorage.removeItem(homeKey);
    const key = getGenerateId();
    const homeData = {
      [key]: searchType === 'ALL' ? [] : deptIds,
    };

    setSessionStorageTempState(homeKey, homeData);
    openNewPage(item.routeName, {
      dim: dim.toLocaleUpperCase(),
      key,
      ...(searchType === 'SELF'
        ? {
            type: searchType,
          }
        : {}),
      ...(item.status
        ? {
            timeField,
            status: item.status,
          }
        : {}),
    });
  }

  function initDefaultData() {
    defaultWinOrderData.value = dateKey.value.reduce((acc, key) => {
      acc[key] = createWinOrderBlock();
      return acc;
    }, {} as Record<string, any>);

    defaultOpportunityData.value = dateKey.value.reduce((acc, key) => {
      acc[key] = createOpportunityBlock();
      return acc;
    }, {} as Record<string, any>);

    defaultLeadData.value = dateKey.value.reduce((acc, key) => {
      acc[key] = createLeadBlock();
      return acc;
    }, {} as Record<string, any>);
  }

  async function initLeadDetail(params: GetHomeStatisticParams) {
    if (!hasAnyPermission(['CLUE_MANAGEMENT:READ'])) return;
    try {
      const { deptIds, searchType, userField } = params;
      const result = await getHomeLeadStatistic({
        deptIds,
        searchType,
        userField,
      });
      Object.keys(defaultLeadData.value).forEach((k) => {
        const resultArr: string[] = Object.keys(result);
        const leadKey: string | undefined = resultArr.find((e) =>
          e.toLocaleUpperCase().includes(k.toLocaleUpperCase())
        );
        if (leadKey) {
          defaultLeadData.value[k] = {
            ...defaultLeadData.value[k],
            ...result[leadKey as keyof HomeLeadStatisticDetail],
          };
        }
      });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  async function initOptDetail(params: GetHomeStatisticParams) {
    if (!hasAnyPermission(['OPPORTUNITY_MANAGEMENT:READ'])) return;
    try {
      const result = await getHomeFollowOpportunity(params);
      Object.keys(defaultOpportunityData.value).forEach((k) => {
        const resultArr: string[] = Object.keys(result);
        const optKeys: string[] = resultArr.filter((e) => e.toLocaleUpperCase().includes(k.toLocaleUpperCase()));
        if (optKeys?.length) {
          const [newKey, amountKey] = optKeys;
          defaultOpportunityData.value[k] = {
            newOpportunity: {
              ...defaultOpportunityData.value[k].newOpportunity,
              ...result[newKey as keyof FollowOptStatisticDetail],
            },
            newOpportunityAmount: {
              ...defaultOpportunityData.value[k].newOpportunityAmount,
              ...result[amountKey as keyof FollowOptStatisticDetail],
            },
          };
        }
      });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  async function initSuccessOptDetail(params: GetHomeStatisticParams) {
    if (!hasAnyPermission(['OPPORTUNITY_MANAGEMENT:READ'])) return;
    try {
      const { deptIds, searchType, priorPeriodEnable } = params;
      const result = await getHomeSuccessOptStatistic({
        deptIds,
        searchType,
        priorPeriodEnable,
      });
      Object.keys(defaultWinOrderData.value).forEach((k) => {
        const resultArr: string[] = Object.keys(result);
        const optKeys: string[] = resultArr.filter((e) => e.toLocaleUpperCase().includes(k.toLocaleUpperCase()));
        if (optKeys?.length) {
          const [newKey, amountKey] = optKeys;
          defaultWinOrderData.value[k] = {
            successOpportunity: {
              ...defaultWinOrderData.value[k].successOpportunity,
              ...result[newKey as keyof HomeWinOrderDetail],
            },
            successOpportunityAmount: {
              ...defaultWinOrderData.value[k].successOpportunityAmount,
              ...result[amountKey as keyof HomeWinOrderDetail],
            },
          };
        }
      });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }
  const loading = ref(false);
  async function initHomeStatistic(params: GetHomeStatisticParams) {
    initDefaultData();
    try {
      loading.value = true;
      await Promise.all([initLeadDetail(params), initOptDetail(params), initSuccessOptDetail(params)]);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  onBeforeMount(() => {
    initDefaultData();
  });

  defineExpose({
    initHomeStatistic,
  });
</script>

<style scoped lang="less">
  .overview {
    display: grid;
    row-gap: 16px;
    grid-template-columns: 172px 1fr;
    .header {
      height: 48px;
      font-size: 16px;
      background: var(--info-4);
      line-height: 48px;
      &.header-category {
        width: 174px;
        clip-path: polygon(0 0, 100% -15%, 92% 100%, 0% 100%);
      }
    }
    .category-time {
      display: grid;
      grid-template-columns: repeat(4, 1fr); /* 左侧固定宽度 + 右侧等分 */
      column-gap: 5px;
      .header {
        transform: skew(-14deg);
        .header-time {
          transform: skew(14deg);
        }
        &:last-child::after {
          position: absolute;
          top: 0;
          right: 0;
          display: block;
          width: 12px;
          height: 100%;
          background: var(--info-4);
          content: '';
          transform: skew(14deg);
        }
      }
    }
    .right-cell {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      column-gap: 1.5px;
    }
    .cell {
      display: grid;
      padding: 16px;
      text-align: left;
      background: var(--text-n9);
      grid-template-columns: repeat(2, 1fr);
      :nth-of-type(1) {
        margin: 0 1px;
      }
      @apply flex items-center;
      .cell-label {
        color: var(--text-n2);
      }
      .cell-label,
      .cell-value {
        @apply flex-1;
      }
      .count {
        font-size: 18px;
        @apply flex items-baseline font-semibold;
        .unit {
          font-size: 14px;
        }
      }
      .analytics-last-time {
        gap: 2px;
        @apply flex flex-nowrap items-center justify-start;

        font-size: 12px;
        .last-time-rate-up {
          color: var(--error-red);
          @apply inline-flex items-center;
        }
        .last-time-rate-down {
          color: var(--success-green);
          @apply inline-flex items-center;
        }
      }
    }
  }
</style>
