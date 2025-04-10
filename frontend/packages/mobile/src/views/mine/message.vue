<template>
  <CrmPageWrapper :title="t('common.message')">
    <div class="flex h-full flex-col overflow-hidden">
      <van-tabs v-model:active="activeName" border class="customer-tabs">
        <van-tab v-for="tab of tabList" :key="tab.name" :name="tab.name">
          <template #title>
            <div class="text-[16px]" :class="activeName === tab.name ? 'text-[var(--primary-8)]' : ''">
              {{ tab.title }}
            </div>
          </template>
          <div class="flex h-full flex-col overflow-hidden">
            <div class="flex items-center gap-[12px] bg-[var(--text-n10)] p-[8px_16px]">
              <van-search
                v-model="keyword"
                shape="round"
                :placeholder="t('customer.searchPlaceholder')"
                class="flex-1 !p-0"
              />
            </div>
            <div class="filter-buttons flex gap-2">
              <template v-for="item of messageButtons" :key="item.name">
                <van-badge :dot="item.count > 0">
                  <van-button
                    round
                    size="small"
                    class="!border-none !px-[16px] !py-[4px] !text-[14px]"
                    :class="
                      activeFilter === item.name
                        ? '!bg-[var(--primary-7)] !text-[var(--primary-8)]'
                        : '!bg-[var(--text-n9)] !text-[var(--text-n1)]'
                    "
                    @click="activeFilter = item.name"
                  >
                    {{ item.tab }}
                  </van-button>
                </van-badge>
              </template>
            </div>
          </div>
        </van-tab>
      </van-tabs>
      <div class="flex-1 overflow-auto">
        <CrmList :list-params="listParams" class="p-[16px]" :item-gap="16">
          <template #item="{ item }">
            <CrmMessageItem :item="item" />
          </template>
        </CrmList>
      </div>
    </div>
  </CrmPageWrapper>
</template>

<script setup lang="ts">
  import { SystemMessageTypeEnum, SystemResourceMessageTypeEnum } from '@lib/shared/enums/systemEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmList from '@/components/pure/crm-list/index.vue';
  import CrmMessageItem from '@/components/business/crm-message-item/index.vue';

  const { t } = useI18n();

  const activeName = ref('');

  const tabList = [
    {
      name: '',
      title: t('mine.allMessage'),
    },
    {
      name: SystemMessageTypeEnum.SYSTEM_NOTICE,
      title: t('mine.systemMessage'),
    },
    {
      name: SystemMessageTypeEnum.ANNOUNCEMENT_NOTICE,
      title: t('mine.announcementMessage'),
    },
  ];

  const keyword = ref('');

  const activeFilter = ref('');

  const messageButtons = [
    {
      name: '',
      tab: t('mine.allMessage'),
      count: 99,
    },
    {
      name: SystemResourceMessageTypeEnum.CUSTOMER,
      tab: t('menu.customer'),
      count: 99,
    },
    {
      name: SystemResourceMessageTypeEnum.CLUE,
      tab: t('menu.clue'),
      count: 99,
    },
    {
      name: SystemResourceMessageTypeEnum.OPPORTUNITY,
      tab: t('mine.opportunityMessage'),
      count: 0,
    },
  ];

  const listParams = computed(() => {
    return {
      searchType: activeFilter.value,
      keyword: keyword.value,
    };
  });
</script>

<style lang="less" scoped>
  .filter-buttons {
    @apply flex;

    gap: 8px;
    padding: 8px 4px;
    background-color: var(--text-n10);
    .half-px-border-bottom();
  }
</style>
