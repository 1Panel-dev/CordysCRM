<template>
  <div class="flex h-full flex-col overflow-hidden">
    <div class="bg-[var(--text-n10)] p-[8px_16px]">
      <van-search v-model="keyword" shape="round" :placeholder="t('customer.searchPlaceholder')" class="flex-1 !p-0" />
    </div>
    <div class="filter-buttons">
      <van-button
        v-for="item of filterButtons"
        :key="item.name"
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
    </div>
    <div class="flex-1 overflow-hidden">
      <CrmList :list-params="listParams" class="p-[16px]" :item-gap="16">
        <template #item="{ item }">
          <CrmListCommonItem :item="item" :actions="actions"></CrmListCommonItem>
        </template>
      </CrmList>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmList from '@/components/pure/crm-list/index.vue';
  import CrmListCommonItem from '@/components/pure/crm-list-common-item/index.vue';

  const { t } = useI18n();
  const keyword = ref('');
  const activeFilter = ref('south');
  const filterButtons = [
    {
      name: 'south',
      tab: '南区',
    },
    {
      name: 'north',
      tab: '北区',
    },
    {
      name: 'west',
      tab: '西区',
    },
  ];
  const listParams = computed(() => {
    return {
      searchType: activeFilter.value,
      keyword: keyword.value,
    };
  });
  const actions = [
    {
      label: t('common.pick'),
      icon: 'iconicon_user_add',
      permission: [],
      action: (item: any) => {
        console.log('pick', item.id);
      },
    },
    {
      label: t('common.distribute'),
      icon: 'iconicon_swap',
      permission: [],
      action: (item: any) => {
        console.log('distribute', item.id);
      },
    },
    {
      label: t('common.delete'),
      icon: 'iconicon_delete',
      permission: [],
      action: (item: any) => {
        console.log('delete', item.id);
      },
    },
  ];
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
