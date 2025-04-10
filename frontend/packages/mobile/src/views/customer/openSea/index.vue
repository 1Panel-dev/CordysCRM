<template>
  <div class="flex h-full flex-col overflow-hidden">
    <div class="bg-[var(--text-n10)] p-[8px_16px]">
      <van-search
        v-model="keyword"
        shape="round"
        :placeholder="t('customer.searchPlaceholder')"
        class="flex-1 !p-0"
        @search="search"
      />
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
      <CrmList
        ref="crmListRef"
        :list-params="listParams"
        :load-list-api="getOpenSeaCustomerList"
        class="p-[16px]"
        :item-gap="16"
      >
        <template #item="{ item }">
          <CrmListCommonItem :item="item" :actions="actions" @click="goDetail"></CrmListCommonItem>
        </template>
      </CrmList>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { useRouter } from 'vue-router';
  import { showConfirmDialog, showSuccessToast } from 'vant';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmList from '@/components/pure/crm-list/index.vue';
  import CrmListCommonItem from '@/components/pure/crm-list-common-item/index.vue';

  import { getOpenSeaCustomerList } from '@/api/modules';

  import { CustomerRouteEnum } from '@/enums/routeEnum';

  const { t } = useI18n();
  const router = useRouter();

  const crmListRef = ref<InstanceType<typeof CrmList>>();
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
        router.push({
          name: CustomerRouteEnum.CUSTOMER_DISTRIBUTE,
          query: {
            id: item.id,
          },
        });
      },
    },
    {
      label: t('common.delete'),
      icon: 'iconicon_delete',
      permission: [],
      action: (item: any) => {
        showConfirmDialog({
          title: t('customer.deleteTitle'),
          message: t('customer.deleteTip'),
          confirmButtonText: t('common.confirmDelete'),
          confirmButtonColor: 'var(--error-red)',
          beforeClose: (action) => {
            if (action === 'confirm') {
              try {
                // TODO: delete customer
                showSuccessToast(t('common.deleteSuccess'));
                crmListRef.value?.loadList(true);
                return Promise.resolve(true);
              } catch (error) {
                // eslint-disable-next-line no-console
                console.log(error);
                return Promise.resolve(false);
              }
            } else {
              return Promise.resolve(true);
            }
          },
        });
      },
    },
  ];

  watch(
    () => activeFilter.value,
    () => {
      crmListRef.value?.loadList(true);
    }
  );

  function search() {
    crmListRef.value?.loadList(true);
  }

  function goDetail(item: any) {
    router.push({
      name: CustomerRouteEnum.CUSTOMER_OPENSEA_DETAIL,
      query: {
        id: item.id,
        name: item.name,
        needInitDetail: 'Y',
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
