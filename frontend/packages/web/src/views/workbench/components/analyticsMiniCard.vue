<template>
  <div
    v-if="hasAnyPermission(['CUSTOMER_MANAGEMENT:READ', 'OPPORTUNITY_MANAGEMENT:READ', 'CLUE_MANAGEMENT:READ'])"
    class="flex flex-wrap gap-[16px]"
  >
    <div
      v-for="item of analyticsMiniCardList"
      :key="item.icon"
      v-permission="item.permission"
      class="analytics-mini-card flex-auto"
    >
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-[8px]">
          <div
            class="analytics-icon"
            :style="{
              background: `${item.bgColor}`,
            }"
          >
            <CrmIcon :type="item.icon" :size="16" :class="item.iconColor" />
          </div>
          <div class="text-[16px] leading-[28px]">{{ item.name }}</div>
          <div
            :class="`analytics-count !text-[18px] ${
              item?.routeName && hasAnyPermission(item.permission) ? 'cursor-pointer text-[var(--primary-8)]' : ''
            }`"
            @click="goDetail(item)"
          >
            {{ addCommasToNumber(item.total || 0) }}
          </div>
        </div>
        <div class="analytics-last-time">
          <div class="mx-[4px]">{{ t('workbench.comparedWithPreviousPeriod') }}</div>
          <div class="flex items-center justify-end gap-[4px]">
            <CrmIcon
              v-if="
                item.priorPeriodCompareRate &&
                typeof item.priorPeriodCompareRate === 'number' &&
                item.priorPeriodCompareRate !== 0
              "
              :type="item.priorPeriodCompareRate > 0 ? 'iconicon_caret_up_small' : 'iconicon_caret_down_small'"
              :class="getPriorPeriodCompareRateClass(item.priorPeriodCompareRate)"
            />
            <div :class="getPriorPeriodCompareRateClass(item.priorPeriodCompareRate)">
              {{ item.priorPeriodCompareRateAbs }}
              <span
                v-if="typeof item.priorPeriodCompareRate === 'number'"
                :class="getPriorPeriodCompareRateClass(item.priorPeriodCompareRate)"
              >
                %
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { useRouter } from 'vue-router';

  import { ModuleConfigEnum } from '@lib/shared/enums/moduleEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { addCommasToNumber } from '@lib/shared/method';
  import { AnalyticsDataWithValueKey, GetHomeStatisticParams } from '@lib/shared/models/home';

  import { getHomeContactStatistic, getHomeFollowPlanStatistic, getHomeFollowStatistic } from '@/api/modules';
  import { defaultContactsData, defaultFollowPlanData, defaultFollowRecordData } from '@/config/workbench';
  import { useAppStore } from '@/store';
  import { hasAnyPermission } from '@/utils/permission';

  const { t } = useI18n();
  const appStore = useAppStore();

  function getAnalyticsData(defaultData: AnalyticsDataWithValueKey, detail: Record<string, any>) {
    if (detail) {
      const nameKey = defaultData.valueKey;

      const priorPeriodCompareRate =
        detail[nameKey] && typeof detail[nameKey]?.priorPeriodCompareRate === 'number'
          ? Number(detail[nameKey]?.priorPeriodCompareRate.toFixed(2))
          : '-';

      const priorPeriodCompareRateAbs =
        typeof detail[nameKey]?.priorPeriodCompareRate === 'number' ? Math.abs(priorPeriodCompareRate as number) : '-';
      return {
        ...defaultData,
        ...detail[nameKey],
        priorPeriodCompareRate,
        priorPeriodCompareRateAbs,
        total: detail[nameKey].value,
      };
    }
    return defaultData;
  }

  const getPriorPeriodCompareRateClass = (priorPeriodCompareRate?: number) => {
    if (priorPeriodCompareRate && typeof priorPeriodCompareRate === 'number' && priorPeriodCompareRate !== 0) {
      return priorPeriodCompareRate > 0 ? 'last-time-rate-up' : 'last-time-rate-down';
    }
    if (priorPeriodCompareRate === 0 || typeof priorPeriodCompareRate === 'string') {
      return 'text-[var(--text-n2)]';
    }
  };

  const router = useRouter();
  function goDetail(item: Record<string, any>) {
    if (hasAnyPermission(item.permission) && item.routeName) {
      router.push({ name: item.routeName });
    }
  }

  const contactData = ref();
  async function initContactDetail(params: GetHomeStatisticParams) {
    if (!hasAnyPermission(['CUSTOMER_MANAGEMENT_CONTACT:READ'])) return;
    try {
      contactData.value = await getHomeContactStatistic(params);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }
  const followRecordData = ref();
  async function initFollowRecordDetail(params: GetHomeStatisticParams) {
    if (!hasAnyPermission(['CUSTOMER_MANAGEMENT:READ', 'OPPORTUNITY_MANAGEMENT:READ', 'CLUE_MANAGEMENT:READ'])) return;
    try {
      followRecordData.value = await getHomeFollowStatistic(params);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }
  const followPlanData = ref();
  async function initFollowPlanDetail(params: GetHomeStatisticParams) {
    if (!hasAnyPermission(['CUSTOMER_MANAGEMENT:READ', 'OPPORTUNITY_MANAGEMENT:READ', 'CLUE_MANAGEMENT:READ'])) return;
    try {
      followPlanData.value = await getHomeFollowPlanStatistic(params);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  const analyticsMiniCardList = computed<AnalyticsDataWithValueKey[]>(() => {
    const lastContactData = getAnalyticsData(defaultContactsData, contactData.value);
    const lastFollowRecordData = getAnalyticsData(defaultFollowRecordData, followRecordData.value);
    const lastFollowPlanData = getAnalyticsData(defaultFollowPlanData, followPlanData.value);
    return [lastContactData, lastFollowRecordData, lastFollowPlanData].filter((item) =>
      item.moduleKey.some((moduleKey: ModuleConfigEnum) =>
        appStore.moduleConfigList.some((m) => m.moduleKey === moduleKey && m.enable)
      )
    );
  });

  function initStatisticDetail(params: GetHomeStatisticParams) {
    initContactDetail(params);
    initFollowRecordDetail(params);
    initFollowPlanDetail(params);
  }

  defineExpose({
    initStatisticDetail,
  });
</script>

<style scoped lang="less">
  .analytics-mini-card {
    padding: 24px;
    border: 1px solid var(--text-n8);
    border-radius: var(--border-radius-medium);
  }
</style>
