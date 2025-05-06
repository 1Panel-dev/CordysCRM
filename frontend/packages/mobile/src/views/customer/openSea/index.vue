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
        v-for="item of openSeaList"
        :key="item.id"
        round
        size="small"
        class="!border-none !px-[16px] !py-[4px] !text-[14px]"
        :class="
          activeOpenSea === item.id
            ? '!bg-[var(--primary-7)] !text-[var(--primary-8)]'
            : '!bg-[var(--text-n9)] !text-[var(--text-n1)]'
        "
        @click="activeOpenSea = item.id"
      >
        {{ item.name }}
      </van-button>
    </div>
    <CrmList
      v-if="openSeaList.length"
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
</template>

<script setup lang="ts">
  import { useRouter } from 'vue-router';
  import { closeToast, showConfirmDialog, showLoadingToast, showSuccessToast } from 'vant';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { sleep } from '@lib/shared/method';
  import { CustomerOptionsItem } from '@lib/shared/models/customer';

  import CrmList from '@/components/pure/crm-list/index.vue';
  import CrmListCommonItem from '@/components/pure/crm-list-common-item/index.vue';

  import { deleteOpenSeaCustomer, getOpenSeaCustomerList, getOpenSeaOptions, pickOpenSeaCustomer } from '@/api/modules';

  import { CustomerRouteEnum } from '@/enums/routeEnum';

  const { t } = useI18n();
  const router = useRouter();

  const crmListRef = ref<InstanceType<typeof CrmList>>();
  const keyword = ref('');
  const activeOpenSea = ref<string | number>('');
  const openSeaList = ref<CustomerOptionsItem[]>([]);
  const listParams = computed(() => {
    return {
      poolId: activeOpenSea.value,
      keyword: keyword.value,
    };
  });
  const actions = [
    {
      label: t('common.pick'),
      icon: 'iconicon_user_add',
      permission: ['CUSTOMER_MANAGEMENT_POOL:PICK'],
      action: async (item: any) => {
        try {
          showLoadingToast(t('common.picking'));
          await pickOpenSeaCustomer({
            customerId: item.id,
            poolId: activeOpenSea.value,
          });
          showSuccessToast(t('common.pickSuccess'));
          await sleep(300);
          crmListRef.value?.loadList(true);
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        } finally {
          closeToast();
        }
      },
    },
    {
      label: t('common.distribute'),
      icon: 'iconicon_swap',
      permission: ['CUSTOMER_MANAGEMENT_POOL:ASSIGN'],
      action: (item: any) => {
        router.push({
          name: CustomerRouteEnum.CUSTOMER_DISTRIBUTE,
          query: {
            id: item.id,
            apiKey: FormDesignKeyEnum.CUSTOMER,
          },
        });
      },
    },
    {
      label: t('common.delete'),
      icon: 'iconicon_delete',
      permission: ['CUSTOMER_MANAGEMENT_POOL:DELETE'],
      action: (item: any) => {
        showConfirmDialog({
          title: t('customer.deleteTitle'),
          message: t('customer.deleteTip'),
          confirmButtonText: t('common.confirmDelete'),
          confirmButtonColor: 'var(--error-red)',
          beforeClose: async (action) => {
            if (action === 'confirm') {
              try {
                showLoadingToast(t('common.deleting'));
                await deleteOpenSeaCustomer(item.id);
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
    () => activeOpenSea.value,
    () => {
      crmListRef.value?.loadList(true);
    }
  );

  function search() {
    crmListRef.value?.loadList(true);
  }

  onActivated(() => {
    search();
  });

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

  async function init() {
    try {
      showLoadingToast(t('common.loading'));
      openSeaList.value = await getOpenSeaOptions();
      activeOpenSea.value = openSeaList.value[0].id;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      closeToast();
    }
  }

  onBeforeMount(() => {
    init();
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
