<template>
  <div class="flex flex-wrap gap-[16px]">
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
          <div class="text-[16px]">{{ item.name }}</div>
          <div class="analytics-count">
            {{ item.total }}
          </div>
        </div>

        <div class="analytics-last-time">
          <div>{{ t('workbench.comparedWithPreviousPeriod') }}</div>
          <div class="flex items-center justify-end">
            <CrmIcon
              :type="item.isGrowth > 0 ? 'iconicon_caret_up_small' : 'iconicon_caret_down_small'"
              :class="`last-time-rate-${item.isGrowth > 0 ? 'up' : 'down'}`"
            />
            <div :class="`last-time-rate-${item.isGrowth > 0 ? 'up' : 'down'}`">{{ item.isGrowth }}%</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import { defaultContactsData, defaultFollowPlanData, defaultFollowRecordData } from '@/config/workbench';

  const { t } = useI18n();

  const analyticsMiniCardList = computed(() => [defaultContactsData, defaultFollowRecordData, defaultFollowPlanData]);
</script>

<style scoped lang="less">
  .analytics-mini-card {
    padding: 24px;
    border: 1px solid var(--text-n8);
    border-radius: var(--border-radius-medium);
  }
</style>
