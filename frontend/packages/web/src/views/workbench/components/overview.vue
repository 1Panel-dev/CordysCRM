<template>
  <div class="overview overflow-hidden">
    <div class="header header-category font-semibold text-[var(--text-n1)]">
      {{ t('workbench.dataOverview.category') }}
    </div>
    <div class="category-time w-full">
      <div v-for="(item, index) of headerList" :key="`${item.icon}-${index}`" class="header">
        <div class="header-time flex items-center justify-center gap-[8px]">
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
        <div class="cell-value count" @click="goDetail(dim, defaultLeadData[dim])">
          {{ abbreviateNumber(defaultLeadData[dim].value) }}
        </div>
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
        <div v-for="item in defaultOpportunityData[dim]" :key="`iconicon_facial_business-${item.title}`" class="flex-1">
          <div class="cell-label">{{ item.title }}</div>
          <div class="count" @click="goDetail(dim, item)">{{ abbreviateNumber(item.value) }}</div>
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
      <div v-for="(dim, index) of dateKey" :key="`iconicon_facial_deal_win-${index}`" class="cell">
        <div v-for="item in defaultWinOrderData[dim]" :key="`iconicon_facial_deal_win-${item.title}`" class="flex-1">
          <div class="cell-label">{{ item?.title }}</div>
          <div class="count" @click="goDetail(dim, item)">{{ abbreviateNumber(item.value) }}</div>
          <div class="analytics-last-time">
            <div class="text-[var(--text-n2)]">{{ t('workbench.dataOverview.comparedSamePeriod') }}</div>
            <CrmIcon
              v-if="
                item.priorPeriodCompareRate &&
                typeof item.priorPeriodCompareRate === 'number' &&
                item.priorPeriodCompareRate !== 0
              "
              :type="item.priorPeriodCompareRate > 0 ? 'iconicon_caret_up_small' : 'iconicon_caret_down_small'"
              :class="getPriorPeriodClass(item.priorPeriodCompareRate)"
            />
            <div :class="getPriorPeriodClass(item.priorPeriodCompareRate)">
              {{ periodCompareRateAbs(item.priorPeriodCompareRate) }}
              <span v-if="typeof item.priorPeriodCompareRate === 'number'"> % </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  // TODO 联调 xinxinwu
  import { ref } from 'vue';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { abbreviateNumber } from '@lib/shared/method';

  import categoryCard from './categoryCard.vue';

  import useOpenNewPage from '@/hooks/useOpenNewPage';

  import { AppRouteEnum } from '@/enums/routeEnum';

  const { openNewPage } = useOpenNewPage();

  const { t } = useI18n();

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

  const defaultLeadData = ref<Record<string, any>>({
    today: {
      value: 0,
      priorPeriodCompareRate: 0,
      routeName: AppRouteEnum.CLUE_MANAGEMENT_CLUE,
    },
    week: {
      value: 0,
      priorPeriodCompareRate: 0,
      routeName: AppRouteEnum.CLUE_MANAGEMENT_CLUE,
    },
    month: {
      value: 0,
      priorPeriodCompareRate: 0,
      routeName: AppRouteEnum.CLUE_MANAGEMENT_CLUE,
    },
    year: {
      value: 0,
      priorPeriodCompareRate: 0,
      routeName: AppRouteEnum.CLUE_MANAGEMENT_CLUE,
    },
  });

  const defaultOpportunityData = ref<Record<string, Record<string, any>>>({});
  const createOpportunityBlock = () => ({
    newOpportunity: {
      title: t('workbench.dataOverview.followingOrder'),
      value: 0,
      priorPeriodCompareRate: 0,
      routeName: AppRouteEnum.OPPORTUNITY_OPT,
      status: 'FOLLOWING',
    },
    newOpportunityAmount: {
      title: t('workbench.dataOverview.amount'),
      value: 0,
      priorPeriodCompareRate: 0,
      routeName: AppRouteEnum.OPPORTUNITY_OPT,
      status: 'FOLLOWING',
    },
  });

  const defaultWinOrderData = ref<Record<string, any>>({});
  const createWinOrderBlock = () => ({
    successOpportunity: {
      title: t('workbench.dataOverview.winOrderUnit'),
      value: 0,
      priorPeriodCompareRate: 0,
      routeName: AppRouteEnum.OPPORTUNITY_OPT,
      status: 'SUCCESS',
    },
    successOpportunityAmount: {
      title: t('workbench.dataOverview.amount'),
      value: 0,
      priorPeriodCompareRate: 0,
      routeName: AppRouteEnum.OPPORTUNITY_OPT,
      status: 'SUCCESS',
    },
  });

  const getPriorPeriodClass = (priorPeriodCompareRate?: number) => {
    if (priorPeriodCompareRate && typeof priorPeriodCompareRate === 'number' && priorPeriodCompareRate !== 0) {
      return priorPeriodCompareRate > 0 ? 'last-time-rate-up' : 'last-time-rate-down';
    }
    if (priorPeriodCompareRate === 0 || typeof priorPeriodCompareRate === 'string') {
      return 'text-[var(--text-n2)]';
    }
  };

  const periodCompareRateAbs = (priorPeriodCompareRate?: number) => {
    if (typeof priorPeriodCompareRate !== 'number' || Number.isNaN(priorPeriodCompareRate)) return '-';
    return Math.abs(Number(priorPeriodCompareRate.toFixed(2)));
  };

  // TODO 参数没有查询
  function goDetail(dim: string, item: Record<string, any>) {
    openNewPage(item.routeName, {
      dim: dim.toLocaleUpperCase(),
      ...(item.status
        ? {
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
  }

  onBeforeMount(() => {
    initDefaultData();
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
      text-align: center;
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
        color: var(--primary-8);
        @apply cursor-pointer font-semibold;
      }
      .analytics-last-time {
        gap: 8px;
        @apply flex flex-nowrap items-center justify-start;

        font-size: 12px;
        .last-time-rate-up {
          color: var(--error-red);
        }
        .last-time-rate-down {
          color: var(--success-green);
        }
      }
    }
  }
</style>
