<template>
  <div class="flex h-full flex-col overflow-hidden">
    <div class="bg-[var(--text-n10)] p-[8px_16px]">
      <van-search
        v-model="keyword"
        shape="round"
        :placeholder="t('clue.searchPlaceholder')"
        class="crm-search"
        @search="search"
        @clear="search"
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
      :immediateCheck="false"
    >
      <template #item="{ item }">
        <CrmListCommonItem
          :item="item"
          :actions="getListItemActions(item)"
          resource-type="lead"
          @click="goDetail"
        ></CrmListCommonItem>
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
  import CrmListCommonItem, {
    type CrmListCommonItemActionsItem,
  } from '@/components/pure/crm-list-common-item/index.vue';

  import { deleteCluePool, getCluePoolList, getPoolOptions, pickClue } from '@/api/modules';
  import useApprovalOperation from '@/hooks/useApprovalOperation';
  import useApprovalResourceAction from '@/hooks/useApprovalResourceAction';
  import { hasAnyPermission } from '@/utils/permission';

  import { ClueRouteEnum, CustomerRouteEnum } from '@/enums/routeEnum';

  type PoolActionItem = CrmListCommonItemActionsItem & { key: string };

  const { t } = useI18n();
  const router = useRouter();

  const crmListRef = ref<InstanceType<typeof CrmList>>();
  const keyword = ref('');

  const activeFilter = ref('');
  const filterButtons = ref<CluePoolItem[]>([]);

  const refreshList = () => {
    crmListRef.value?.loadList(true);
  };
  const { reviewByResourceId, revokeByResourceId } = useApprovalResourceAction({
    formKey: FormDesignKeyEnum.CLUE,
  });
  const { resolveRowActions, initApprovalPermission, deleteExecute } = useApprovalOperation<CluePoolListItem>({
    formType: FormDesignKeyEnum.CLUE,
    ignoreRolePermissionCheck: true,
    onReview: (row) => reviewByResourceId(row.id, { onSuccess: refreshList }),
    onRevoke: (row) => revokeByResourceId(row.id, { onSuccess: refreshList }),
  });

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
      refreshList();
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
      confirmButtonText: deleteExecute.value ? t('crm.approval.confirmAndSubmitReview') : t('common.confirmDelete'),
      confirmButtonColor: 'var(--error-red)',
      beforeClose: async (action) => {
        if (action === 'confirm') {
          try {
            showLoadingToast(t('common.deleting'));
            await deleteCluePool(id);
            showSuccessToast(deleteExecute.value ? t('common.reviewSuccess') : t('common.deleteSuccess'));
            refreshList();
            return Promise.resolve(true);
          } catch (error) {
            // eslint-disable-next-line no-console
            console.log(error);
            return Promise.resolve(false);
          } finally {
            closeToast();
          }
        } else {
          return Promise.resolve(true);
        }
      },
    });
  }

  const poolActions: PoolActionItem[] = [
    {
      key: 'pick',
      label: t('common.pick'),
      icon: 'iconicon_user_add',
      permission: ['CLUE_MANAGEMENT_POOL:PICK'],
      action: (item: CluePoolListItem) => {
        handlePick(item.id);
      },
    },
    {
      key: 'distribute',
      label: t('common.distribute'),
      icon: 'iconicon_swap',
      permission: ['CLUE_MANAGEMENT_POOL:ASSIGN'],
      action: (item: CluePoolListItem) => {
        handleDistribute(item.id);
      },
    },
  ];

  const deleteAction: PoolActionItem = {
    key: 'delete',
    label: t('common.delete'),
    icon: 'iconicon_delete',
    permission: ['CLUE_MANAGEMENT:DELETE'],
    action: (item: CluePoolListItem) => {
      handleDelete(item.id);
    },
  };

  function normalizeListAction(action: Record<string, any>): PoolActionItem {
    return {
      ...action,
      key: action.key,
      label: action.label ?? action.text ?? '',
      icon: action.icon ?? '',
      action: action.action ?? (() => undefined),
    };
  }

  function getListItemActions(item: CluePoolListItem): CrmListCommonItemActionsItem[] {
    const approvalActions = resolveRowActions(item, [deleteAction]);
    const reviewActions = approvalActions
      .filter((action) => ['review', 'revoke'].includes(action.key || ''))
      .map(normalizeListAction);
    const approvedDeleteAction = approvalActions.find((action) => action.key === 'delete');
    const poolDeleteAction =
      approvedDeleteAction && hasAnyPermission(['CLUE_MANAGEMENT_POOL:DELETE'])
        ? normalizeListAction({
            ...approvedDeleteAction,
            permission: ['CLUE_MANAGEMENT_POOL:DELETE'],
          })
        : undefined;

    return [...reviewActions, ...poolActions, ...(poolDeleteAction ? [poolDeleteAction] : [])];
  }

  function search() {
    nextTick(() => {
      crmListRef.value?.loadList(true);
    });
  }

  onActivated(() => {
    initApprovalPermission();
    search();
  });

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
        poolId: activeFilter.value,
        needInitDetail: 'Y',
      },
    });
  }

  onMounted(async () => {
    initApprovalPermission();
    await getCluePoolOptions();
  });
</script>

<style lang="less" scoped>
  .filter-buttons {
    @apply flex;

    overflow: auto;
    padding: 8px 4px;
    min-height: 48px;
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
