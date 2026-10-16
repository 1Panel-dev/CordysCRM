<template>
  <div class="flex h-full flex-col overflow-hidden">
    <div class="bg-[var(--text-n10)] p-[8px_16px]">
      <van-search
        v-model="keyword"
        shape="round"
        :placeholder="t('customer.searchPlaceholder')"
        class="crm-search"
        @search="search"
        @clear="search"
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
      ref="crmListRef"
      :list-params="listParams"
      :load-list-api="getOpenSeaCustomerList"
      class="p-[16px]"
      :item-gap="16"
      :immediateCheck="false"
    >
      <template #item="{ item }">
        <CrmListCommonItem
          :item="item"
          :actions="getListItemActions(item)"
          resource-type="customer"
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
  import type { CustomerListItem } from '@lib/shared/models/customer';
  import { CluePoolItem } from '@lib/shared/models/system/module';

  import CrmList from '@/components/pure/crm-list/index.vue';
  import CrmListCommonItem, {
    type CrmListCommonItemActionsItem,
  } from '@/components/pure/crm-list-common-item/index.vue';

  import { deleteOpenSeaCustomer, getOpenSeaCustomerList, getOpenSeaOptions, pickOpenSeaCustomer } from '@/api/modules';
  import useApprovalOperation from '@/hooks/useApprovalOperation';
  import useApprovalResourceAction from '@/hooks/useApprovalResourceAction';
  import { hasAnyPermission } from '@/utils/permission';

  import { CustomerRouteEnum } from '@/enums/routeEnum';

  type OpenSeaActionItem = CrmListCommonItemActionsItem & { key: string };

  const { t } = useI18n();
  const router = useRouter();

  const crmListRef = ref<InstanceType<typeof CrmList>>();
  const keyword = ref('');
  const activeOpenSea = ref<string | number>('');
  const openSeaList = ref<CluePoolItem[]>([]);
  const listParams = computed(() => {
    return {
      poolId: activeOpenSea.value,
      keyword: keyword.value,
    };
  });

  const refreshList = () => {
    crmListRef.value?.loadList(true);
  };
  const { reviewByResourceId, revokeByResourceId } = useApprovalResourceAction({
    formKey: FormDesignKeyEnum.CUSTOMER,
  });
  const { resolveRowActions, hasApprovalScopedPermission, initApprovalPermission, deleteExecute } =
    useApprovalOperation<CustomerListItem>({
      formType: FormDesignKeyEnum.CUSTOMER,
      ignoreRolePermissionCheck: true,
      onReview: (row) => reviewByResourceId(row.id, { onSuccess: refreshList }),
      onRevoke: (row) => revokeByResourceId(row.id, { onSuccess: refreshList }),
    });
  const openSeaActions: OpenSeaActionItem[] = [
    {
      key: 'pick',
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
          refreshList();
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        } finally {
          closeToast();
        }
      },
    },
    {
      key: 'distribute',
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
  ];
  function handleDelete(id: string) {
    showConfirmDialog({
      title: t('customer.deleteTitle'),
      message: t('customer.deleteTip'),
      confirmButtonText: deleteExecute.value ? t('crm.approval.confirmAndSubmitReview') : t('common.confirmDelete'),
      confirmButtonColor: 'var(--error-red)',
      beforeClose: async (action) => {
        if (action === 'confirm') {
          try {
            showLoadingToast(t('common.deleting'));
            await deleteOpenSeaCustomer(id);
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

  const deleteAction: OpenSeaActionItem = {
    key: 'delete',
    label: t('common.delete'),
    icon: 'iconicon_delete',
    permission: ['CUSTOMER_MANAGEMENT:DELETE'],
    action: (item: any) => {
      handleDelete(item.id);
    },
  };

  function normalizeListAction(action: Record<string, any>): OpenSeaActionItem {
    return {
      ...action,
      key: action.key,
      label: action.label ?? action.text ?? '',
      icon: action.icon ?? '',
      action: action.action ?? (() => undefined),
    };
  }

  function getListItemActions(item: any): CrmListCommonItemActionsItem[] {
    const approvalActions = resolveRowActions(item, [deleteAction]);
    const reviewActions = approvalActions
      .filter((action) => ['review', 'revoke'].includes(action.key || ''))
      .map(normalizeListAction);
    const approvedDeleteAction = approvalActions.find((action) => action.key === 'delete');
    const poolDeleteAction =
      approvedDeleteAction && hasAnyPermission(['CUSTOMER_MANAGEMENT_POOL:DELETE'])
        ? normalizeListAction({
            ...approvedDeleteAction,
            permission: ['CUSTOMER_MANAGEMENT_POOL:DELETE'],
          })
        : undefined;

    return [...reviewActions, ...openSeaActions, ...(poolDeleteAction ? [poolDeleteAction] : [])];
  }

  watch(
    () => activeOpenSea.value,
    () => {
      nextTick(() => {
        crmListRef.value?.loadList(true);
      });
    }
  );

  function search() {
    nextTick(() => {
      crmListRef.value?.loadList(true);
    });
  }

  onActivated(() => {
    initApprovalPermission();
    search();
  });

  function goDetail(item: any) {
    if (
      !hasAnyPermission(['CUSTOMER_MANAGEMENT_POOL:READ']) ||
      !hasApprovalScopedPermission(item, ['CUSTOMER_MANAGEMENT:READ'])
    ) {
      return;
    }

    router.push({
      name: CustomerRouteEnum.CUSTOMER_OPENSEA_DETAIL,
      query: {
        id: item.id,
        name: item.name,
        poolId: activeOpenSea.value,
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
    initApprovalPermission();
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
