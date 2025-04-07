<template>
  <div class="flex h-full flex-col overflow-hidden">
    <div class="flex items-center gap-[12px] bg-[var(--text-n10)] p-[8px_16px]">
      <van-button plain icon="plus" type="primary" size="small" @click="goCreate"> </van-button>
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
          <CrmListCommonItem :item="item" :actions="actions" @click="goDetail"></CrmListCommonItem>
        </template>
      </CrmList>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { useRouter } from 'vue-router';

  import { CustomerSearchTypeEnum } from '@lib/shared/enums/customerEnum';
  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmList from '@/components/pure/crm-list/index.vue';
  import CrmListCommonItem from '@/components/pure/crm-list-common-item/index.vue';

  import { CommonRouteEnum, CustomerRouteEnum } from '@/enums/routeEnum';

  const { t } = useI18n();
  const router = useRouter();

  const keyword = ref('');
  const activeFilter = ref(CustomerSearchTypeEnum.ALL);
  const filterButtons = [
    {
      name: CustomerSearchTypeEnum.ALL,
      tab: t('customer.all'),
    },
    {
      name: CustomerSearchTypeEnum.SELF,
      tab: t('customer.mine'),
    },
    {
      name: CustomerSearchTypeEnum.DEPARTMENT,
      tab: t('customer.deptCustomer'),
    },
    {
      name: CustomerSearchTypeEnum.VISIBLE,
      tab: t('customer.cooperationCustomer'),
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
      label: t('common.edit'),
      icon: 'iconicon_handwritten_signature',
      permission: [],
      action: (item: any) => {
        router.push({
          name: CommonRouteEnum.FORM_CREATE,
          query: {
            id: item.id,
            type: FormDesignKeyEnum.CUSTOMER,
          },
        });
      },
    },
    {
      label: t('common.writeRecord'),
      icon: 'iconicon_handwritten_signature',
      permission: [],
      action: (item: any) => {
        console.log('writeRecord', item.id);
      },
    },
    {
      label: t('common.transfer'),
      icon: 'iconicon_jump',
      permission: [],
      action: (item: any) => {
        console.log('transfer', item.id);
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

  function goCreate() {
    router.push({
      name: CommonRouteEnum.FORM_CREATE,
      query: {
        type: FormDesignKeyEnum.CUSTOMER,
      },
    });
  }

  function goDetail(item: any) {
    router.push({
      name: CustomerRouteEnum.CUSTOMER_DETAIL,
      query: {
        id: item.id,
        name: item.name,
      },
    });
  }
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
