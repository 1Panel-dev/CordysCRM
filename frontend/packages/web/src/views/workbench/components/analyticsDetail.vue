<template>
  <div class="flex flex-wrap gap-[16px]">
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
      <div class="flex items-center gap-[8px]">
        <div v-for="(ele, index) of item.analytics" :key="`${ele.title}-${index}`" class="analytics-item flex-1">
          <div>{{ ele.title }}</div>
          <div class="analytics-count">{{ addCommasToNumber(ele.count || 0) }}</div>
          <div class="analytics-last-time">
            <div>{{ t('workbench.comparedWithPreviousPeriod') }}</div>
            <div class="flex items-center justify-end">
              <CrmIcon
                :type="ele.isGrowth > 0 ? 'iconicon_caret_up_small' : 'iconicon_caret_down_small'"
                :class="`last-time-rate-${ele.isGrowth > 0 ? 'up' : 'down'}`"
              />
              <div :class="`last-time-rate-${ele.isGrowth > 0 ? 'up' : 'down'}`">{{ ele.isGrowth }}%</div>
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

  import { defaultAccountData, defaultClueData, defaultOpportunityData } from '@/config/workbench';
  import { hasAnyPermission } from '@/utils/permission';

  const { t } = useI18n();

  const data = computed(() => [defaultAccountData, defaultClueData, defaultOpportunityData]);

  const router = useRouter();
  function goDetail(item: Record<string, any>) {
    if (hasAnyPermission(item.permission) && item.routeName) {
      router.push({ name: item.routeName });
    }
  }
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
