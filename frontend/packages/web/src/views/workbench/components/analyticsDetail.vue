<template>
  <div
    v-if="hasAnyPermission(['CUSTOMER_MANAGEMENT:READ', 'OPPORTUNITY_MANAGEMENT:READ', 'CLUE_MANAGEMENT:READ'])"
    class="flex flex-wrap gap-[16px]"
  >
    <div v-for="item of data" :key="item.icon" v-permission="item.permission" class="analytics-wrapper flex-auto">
      <div class="analytics-header">
        <div class="flex items-center gap-[4px]">
          <div class="analytics-icon" :style="{ background: item.bgColor }">
            <CrmIcon :type="item.icon" :size="16" :class="item.iconColor" />
          </div>
          <div class="text-[16px]">{{ item.name }}</div>
        </div>
        <div
          :class="`analytics-count ${
            item.routeName && hasAnyPermission(item.permission) ? 'cursor-pointer text-[var(--primary-8)]' : ''
          }`"
          @click="goDetail(item)"
        >
          {{ addCommasToNumber(item.total || 0) }}
        </div>
      </div>
      <div class="flex h-[92px] items-center gap-[8px]">
        <div
          v-for="(ele, index) of item.analytics"
          :key="`${ele.title}-${index}`"
          class="analytics-item flex h-full flex-1 flex-col justify-between"
        >
          <div>{{ ele.title }}</div>
          <div class="analytics-count">{{ addCommasToNumber(ele.count || 0) }}</div>
          <div v-if="!['remainingCapacity', 'totalAmount'].includes(ele.countValue)" class="analytics-last-time">
            <div>{{ t('workbench.comparedWithPreviousPeriod') }}</div>
            <div class="flex items-center justify-end gap-[4px]">
              <CrmIcon
                v-if="
                  ele.priorPeriodCompareRate &&
                  typeof ele.priorPeriodCompareRate === 'number' &&
                  ele.priorPeriodCompareRate !== 0
                "
                :type="ele.priorPeriodCompareRate > 0 ? 'iconicon_caret_up_small' : 'iconicon_caret_down_small'"
                :class="getPriorPeriodCompareRateClass(ele.priorPeriodCompareRate)"
              />
              <div :class="getPriorPeriodCompareRateClass(ele.priorPeriodCompareRate)">
                {{ ele.priorPeriodCompareRate }}
                <span
                  v-if="typeof ele.priorPeriodCompareRate === 'number'"
                  :class="getPriorPeriodCompareRateClass(ele.priorPeriodCompareRate)"
                >
                  %
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { useRouter } from 'vue-router';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { addCommasToNumber } from '@lib/shared/method';
  import { DefaultAnalyticsData, GetHomeStatisticParams } from '@lib/shared/models/home';

  import { getHomeAccountStatistic, getHomeFollowOpportunity, getHomeLeadStatistic } from '@/api/modules';
  import { defaultAccountData, defaultClueData, defaultOpportunityData } from '@/config/workbench';
  import { hasAnyPermission } from '@/utils/permission';

  const { t } = useI18n();

  const props = defineProps<{
    searchType: string;
  }>();

  const router = useRouter();
  function goDetail(item: Record<string, any>) {
    if (hasAnyPermission(item.permission) && item.routeName) {
      router.push({ name: item.routeName });
    }
  }

  const leadData = ref();
  async function initLeadDetail(params: GetHomeStatisticParams) {
    if (!hasAnyPermission(['CLUE_MANAGEMENT:READ'])) return;
    try {
      leadData.value = await getHomeLeadStatistic(params);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  const accountData = ref();
  async function initAccountDetail(params: GetHomeStatisticParams) {
    if (!hasAnyPermission(['CUSTOMER_MANAGEMENT:READ'])) return;
    try {
      accountData.value = await getHomeAccountStatistic(params);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  const opportunityData = ref();
  async function initFollowOptDetail(params: GetHomeStatisticParams) {
    if (!hasAnyPermission(['OPPORTUNITY_MANAGEMENT:READ'])) return;
    try {
      opportunityData.value = await getHomeFollowOpportunity(params);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  function getStatisticDetailData(defaultData: DefaultAnalyticsData, detail: Record<string, any>) {
    const originAnalyticsList = defaultData.analytics;
    if (!detail) {
      return {
        ...defaultData,
        analytics: originAnalyticsList.filter((e) => e.countValue !== 'remainingCapacity'),
      };
    }
    // 部门和全部不展示剩余库容 未配置库容也不展示库容
    const analyticsList =
      props.searchType === 'SELF' && detail.unConfigured === false
        ? originAnalyticsList
        : originAnalyticsList.filter((e) => e.countValue !== 'remainingCapacity');

    return {
      ...defaultData,
      total: detail?.total,
      analytics: analyticsList.map((e: any) => {
        const countKey = e.countValue;
        const count = typeof detail[countKey] === 'number' ? detail[countKey] : detail[countKey]?.value;
        const countValue = typeof count === 'number' ? count : '-';
        return {
          ...e,
          count: countValue,
          priorPeriodCompareRate:
            detail[countKey] && typeof detail[countKey]?.priorPeriodCompareRate === 'number'
              ? Number(detail[countKey]?.priorPeriodCompareRate.toFixed(2))
              : '-',
        };
      }),
    };
  }

  const getPriorPeriodCompareRateClass = (priorPeriodCompareRate: number | string) => {
    if (priorPeriodCompareRate && typeof priorPeriodCompareRate === 'number' && priorPeriodCompareRate !== 0) {
      return priorPeriodCompareRate > 0 ? 'last-time-rate-up' : 'last-time-rate-down';
    }
    if (priorPeriodCompareRate === 0 || typeof priorPeriodCompareRate === 'string') {
      return 'text-[var(--text-n2)]';
    }
  };

  const data = computed<DefaultAnalyticsData[]>(() => {
    const lastLeadData = getStatisticDetailData(defaultClueData, leadData.value);
    const lastAccountData = getStatisticDetailData(defaultAccountData, accountData.value);
    const lastOpportunityData = getStatisticDetailData(defaultOpportunityData, opportunityData.value);
    return [lastLeadData, lastAccountData, lastOpportunityData];
  });

  function initHomeStatistic(params: GetHomeStatisticParams) {
    initLeadDetail(params);
    initAccountDetail(params);
    initFollowOptDetail(params);
  }

  defineExpose({
    initHomeStatistic,
  });
</script>

<style scoped lang="less">
  .analytics-wrapper {
    padding: 24px;
    border: 1px solid var(--text-n8);
    border-radius: var(--border-radius-medium);
    .analytics-header {
      margin-bottom: 14px;
      color: var(--text-n1);
      @apply flex flex-nowrap items-center justify-between;
    }
    .analytics-item {
      padding: 12px;
      border-radius: var(--border-radius-medium);
      background: var(--text-n9);
    }
  }
</style>
