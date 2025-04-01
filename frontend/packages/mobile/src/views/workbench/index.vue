<template>
  <div class="flex h-full flex-col gap-[16px] overflow-hidden">
    <div class="flex items-center gap-[12px] bg-[var(--text-n10)] px-[12px] py-[4px]">
      <van-image round width="40px" height="40px" src="https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg" />
      <van-search v-model="keyword" shape="round" :placeholder="t('workbench.searchPlaceholder')" class="flex-1 !p-0" />
      <CrmIcon name="iconicon_notification" width="21px" height="21px" />
    </div>
    <van-cell-group inset class="py-[16px]">
      <van-cell :border="false" class="!py-0">
        <template #title>
          <div class="font-semibold text-[var(--text-n1)]">{{ t('workbench.quickEntry') }}</div>
        </template>
      </van-cell>
      <div class="flex flex-wrap">
        <div v-for="card of entryCardList" :key="card.name" class="quick-entry-card">
          <CrmIcon :name="card.icon" width="40px" height="40px" />
          <div class="text-[12px] text-[var(--text-n1)]">{{ card.label }}</div>
        </div>
      </div>
    </van-cell-group>
    <van-cell-group inset class="flex-1 py-[16px]">
      <van-cell :border="false" class="!py-0">
        <template #title>
          <div class="font-semibold text-[var(--text-n1)]">{{ t('common.message') }}</div>
        </template>
        <template #value>
          <div class="text-[var(--text-n4)]">
            {{ t('common.checkMore') }}
          </div>
        </template>
      </van-cell>
      <van-pull-refresh v-model="refreshing" class="h-full" @refresh="onLoad">
        <van-list
          v-model:loading="loading"
          v-model:error="error"
          :error-text="t('common.listLoadErrorTip')"
          :finished="finished"
          :finished-text="t('common.listFinishedTip')"
          class="h-full overflow-auto"
          @load="onLoad"
        >
          <div
            v-for="item in list"
            :key="item"
            :title="item"
            class="border-b border-[var(--text-n8)] px-[20px] py-[12px]"
          >
            <div class="mb-[4px] flex items-center justify-between">
              <div class="flex items-center gap-[4px]">
                <van-tag color="var(--info-5)" text-color="var(--info-blue)" class="!px-[8px] !py-[2px]">
                  {{ t('common.system') }}
                </van-tag>
                <van-badge dot>
                  <div class="font-semibold text-[var(--text-n1)]">{{ t('common.systemNotification') }}</div>
                </van-badge>
              </div>
              <div class="text-[var(--primary-8)]">{{ t('common.signRead') }}</div>
            </div>
            <div class="flex flex-col gap-[4px] text-[12px]">
              <div>你负责的客户 赵胖胖文化有限公司 已被移入公海，并踢了你一脚和扇了一巴掌，请知悉！</div>
              <div class="text-[var(--text-n2)]">2025-09-09 12:12:00</div>
            </div>
          </div>
        </van-list>
      </van-pull-refresh>
    </van-cell-group>
  </div>
</template>

<script setup lang="ts">
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  import { useI18n } from '@cordys/web/src/hooks/useI18n';

  const { t } = useI18n();
  const keyword = ref('');
  const entryCardList = [
    {
      icon: 'icon-newCustomer',
      label: t('common.newCustomer'),
      name: 'customer',
    },
    {
      icon: 'icon-newContact',
      label: t('common.newContact'),
      name: 'contact',
    },
    {
      icon: 'icon-newOpportunity',
      label: t('common.newOpportunity'),
      name: 'opportunity',
    },
    {
      icon: 'icon-newClue',
      label: t('common.newClue'),
      name: 'clue',
    },
    {
      icon: 'icon-newRecord',
      label: t('common.newFollowRecord'),
      name: 'followRecord',
    },
    {
      icon: 'icon-newPlan',
      label: t('common.newFollowPlan'),
      name: 'followPlan',
    },
  ];
  const loading = ref(false);
  const error = ref(false);
  const finished = ref(false);
  const refreshing = ref(false);
  const list = ref(['1', '2', '3', '4', '5', '6', '7', '8']);
  const onLoad = () => {
    loading.value = true;
    setTimeout(() => {
      finished.value = true;
      refreshing.value = false;
      loading.value = false;
      error.value = false;
    }, 2000);
  };
</script>

<style lang="less" scoped>
  .quick-entry-card {
    @apply flex basis-1/4 flex-col items-center;

    gap: 8px;
    padding-top: 16px;
    padding-bottom: 8px;
    &:active {
      background-color: var(--text-n9);
    }
  }
</style>
