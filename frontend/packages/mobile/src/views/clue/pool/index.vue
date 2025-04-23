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
    <div v-if="filterButtons.length" class="filter-buttons">
      <van-button
        v-for="item of filterButtons"
        :key="item.id"
        round
        size="small"
        class="!border-none !px-[16px] !py-[4px] !text-[14px]"
        :class="
          activeFilter === item.id
            ? '!bg-[var(--primary-7)] !text-[var(--primary-8)]'
            : '!bg-[var(--text-n9)] !text-[var(--text-n1)]'
        "
        @click="activeFilter = item.id"
      >
        {{ item.name }}
      </van-button>
    </div>
    <CrmList
      ref="crmListRef"
      :keyword="keyword"
      :list-params="{
        poolId: activeFilter,
      }"
      class="p-[16px]"
      :item-gap="16"
      :load-list-api="getCluePoolList"
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
  import type { CluePoolListItem } from '@lib/shared/models/clue';
  import type { CluePoolItem } from '@lib/shared/models/system/module';

  import CrmList from '@/components/pure/crm-list/index.vue';
  import CrmListCommonItem from '@/components/pure/crm-list-common-item/index.vue';

  import { deleteCluePool, getCluePoolList, getPoolOptions, pickClue } from '@/api/modules';

  import { ClueRouteEnum, CustomerRouteEnum } from '@/enums/routeEnum';

  const { t } = useI18n();
  const router = useRouter();

  const crmListRef = ref<InstanceType<typeof CrmList>>();
  const keyword = ref('');

  const activeFilter = ref('');
  const filterButtons = ref<CluePoolItem[]>([]);
  async function getCluePoolOptions() {
    try {
      showLoadingToast(t('common.loading'));
      filterButtons.value = await getPoolOptions();
      activeFilter.value = filterButtons.value[0]?.id || '';
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      closeToast();
    }
  }

  async function handlePick(id: string) {
    try {
      showLoadingToast(t('common.picking'));
      await pickClue({
        clueId: id,
        poolId: activeFilter.value,
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
  }

  function handleDistribute(id: string) {
    router.push({
      name: CustomerRouteEnum.CUSTOMER_DISTRIBUTE,
      query: {
        id,
        apiKey: FormDesignKeyEnum.CLUE,
      },
    });
  }

  function handleDelete(id: string) {
    showConfirmDialog({
      title: t('clue.deleteTitle'),
      message: t('clue.batchDeleteContentTip'),
      confirmButtonText: t('common.confirmDelete'),
      confirmButtonColor: 'var(--error-red)',
      beforeClose: async (action) => {
        if (action === 'confirm') {
          try {
            showLoadingToast(t('common.deleting'));
            await deleteCluePool(id);
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
  }

  const actions = [
    {
      label: t('common.pick'),
      icon: 'iconicon_user_add',
      permission: ['CLUE_MANAGEMENT_POOL:PICK'],
      action: (item: CluePoolListItem) => {
        handlePick(item.id);
      },
    },
    {
      label: t('common.distribute'),
      icon: 'iconicon_swap',
      permission: ['CLUE_MANAGEMENT_POOL:ASSIGN'],
      action: (item: CluePoolListItem) => {
        handleDistribute(item.id);
      },
    },
    {
      label: t('common.delete'),
      icon: 'iconicon_delete',
      permission: ['CLUE_MANAGEMENT_POOL:DELETE'],
      action: (item: CluePoolListItem) => {
        handleDelete(item.id);
      },
    },
  ];

  function search() {
    crmListRef.value?.loadList(true);
  }

  watch(
    () => activeFilter.value,
    (val) => {
      if (val.length) {
        nextTick(() => {
          search();
        });
      }
    }
  );

  function goDetail(item: Record<string, any>) {
    router.push({
      name: ClueRouteEnum.CLUE_POOL_DETAIL,
      query: {
        id: item.id,
        name: item.name,
        needInitDetail: 'Y',
      },
    });
  }

  onMounted(async () => {
    await getCluePoolOptions();
  });
</script>

<style lang="less" scoped>
  .filter-buttons {
    @apply flex;

    overflow: auto;
    padding: 8px 4px;
    white-space: nowrap;
    background-color: var(--text-n10);
    gap: 8px;
    &::-webkit-scrollbar {
      display: none;
    }
    .half-px-border-bottom();
  }
  :deep(.van-action-sheet__cancel) {
    color: var(--text-n1);
  }
</style>
