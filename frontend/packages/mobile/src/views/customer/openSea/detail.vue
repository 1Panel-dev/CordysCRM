<template>
  <CrmPageWrapper :title="route.query.name?.toString() || ''">
    <van-tabs v-model:active="activeTab" border class="detail-tabs">
      <van-tab v-for="tab of tabList" :key="tab.name" :name="tab.name">
        <template #title>
          <div class="text-[16px]" :class="activeTab === tab.name ? 'text-[var(--primary-8)]' : ''">
            {{ tab.title }}
          </div>
        </template>
        <div v-if="tab.name === 'info'" class="relative h-full overflow-auto bg-[var(--text-n9)] pt-[16px]">
          <CrmDescription :description="renderDescriptions" :source-id="sourceId">
            <template #approvalStatus>
              <ApprovalStatus :status="approvalStatus" />
            </template>
          </CrmDescription>
        </div>
        <CrmFollowRecordList
          v-else-if="tab.name === 'record'"
          :source-id="sourceId"
          :type="FormDesignKeyEnum.FOLLOW_RECORD_CUSTOMER"
          readonly
        />
        <div v-else-if="tab.name === 'approval'" class="flex h-full bg-[var(--text-n9)] p-[16px]">
          <CrmApprovalLine
            :nodes="approvalInfo?.nodes || []"
            :submitter="{
              submitAvatar: approvalInfo?.submitAvatar,
              submitter: approvalInfo?.submitter,
              submitTime: approvalInfo?.submitTime,
              submitterId: approvalInfo?.submitterId,
              comment: approvalInfo?.comment,
            }"
            :currentApprovalNode="currentApprovalNode"
            :currentApprovalNodeIndex="currentApprovalNodeIndex"
            :finally-result="approvalInfo?.approvalStatus"
            :approval-id="approvalInfo?.id"
            :comment-count="approvalInfo?.commentCount"
            :commentable="approvalInfo?.commentable"
          />
        </div>
        <CrmHeaderList v-else :source-id="sourceId" :load-list-api="getCustomerHeaderList" />
      </van-tab>
    </van-tabs>
    <template v-if="showFooterActions" #footer>
      <CrmApprovalRecordActions
        v-if="showApprovalActions"
        v-slot="{ actions: approvalActions, handleSelect: handleApprovalAction }"
        hide-actions
        :source-id="sourceId"
        :form-key="FormDesignKeyEnum.CUSTOMER"
        :approval-info="approvalInfo"
        :current-approval-node="currentApprovalNode"
        :current-approval-node-index="currentApprovalNodeIndex"
        @refresh="refreshDetail"
        @delete-approved="router.back()"
      >
        <CrmActionButtons
          :actions="getFooterActions(approvalActions)"
          @select="
            (key) => {
              handleSelect(key);
              handleApprovalAction(key);
            }
          "
        />
      </CrmApprovalRecordActions>
      <CrmActionButtons v-else-if="showDetailActions" :actions="operationActions" @select="handleSelect" />
    </template>
  </CrmPageWrapper>
</template>

<script setup lang="ts">
  import { useRoute, useRouter } from 'vue-router';
  import { closeToast, showConfirmDialog, showLoadingToast, showSuccessToast } from 'vant';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { ProcessStatusEnum } from '@lib/shared/enums/process';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { sleep } from '@lib/shared/method';
  import type { ApprovalDetail, ApprovalNode } from '@lib/shared/models/system/process';

  import CrmDescription from '@/components/pure/crm-description/index.vue';
  import CrmPageWrapper from '@/components/pure/crm-page-wrapper/index.vue';
  import CrmActionButtons, { type CrmActionButtonsItem } from '@/components/business/crm-action-buttons/index.vue';
  import CrmApprovalLine from '@/components/business/crm-approval/crm-approval-line.vue';
  import CrmApprovalRecordActions from '@/components/business/crm-approval/crm-approval-record-actions.vue';
  import ApprovalStatus from '@/components/business/crm-approval/crm-approval-status.vue';
  import CrmFollowRecordList from '@/components/business/crm-follow-list/followRecord.vue';
  import CrmHeaderList from '@/components/business/crm-header-list/index.vue';

  import {
    deleteOpenSeaCustomer,
    getApprovalResourceDetail,
    getCustomerHeaderList,
    pickOpenSeaCustomer,
  } from '@/api/modules';
  import useApprovalOperation from '@/hooks/useApprovalOperation';
  import useApprovalResourceAction from '@/hooks/useApprovalResourceAction';
  import useFormCreateApi from '@/hooks/useFormCreateApi';
  import { hasAnyPermission } from '@/utils/permission';

  import { CustomerRouteEnum } from '@/enums/routeEnum';

  defineOptions({
    name: CustomerRouteEnum.CUSTOMER_OPENSEA_DETAIL,
  });
  const route = useRoute();
  const router = useRouter();
  const { t } = useI18n();

  const activeTab = ref('info');
  const approvalInfo = ref<ApprovalDetail>();
  const currentApprovalNode = ref<ApprovalNode>();
  const currentApprovalNodeIndex = ref(0);
  const tabList = computed(() => [
    {
      name: 'info',
      title: t('customer.info'),
    },
    {
      name: 'record',
      title: t('common.record'),
    },
    {
      name: 'header',
      title: t('customer.headerRecord'),
    },
    ...(approvalInfo.value
      ? [
          {
            name: 'approval',
            title: t('workbench.approval.record'),
          },
        ]
      : []),
  ]);

  const sourceId = computed(() => route.query.id?.toString() ?? '');

  const { descriptions, detail, initFormConfig, initFormDescription } = useFormCreateApi({
    formKey: FormDesignKeyEnum.CUSTOMER_OPEN_SEA,
    sourceId,
    needInitDetail: true,
  });

  async function initApprovalDetail() {
    if (
      !sourceId.value ||
      !detail.value.approvalStatus ||
      [ProcessStatusEnum.PENDING, ProcessStatusEnum.NONE].includes(detail.value.approvalStatus)
    ) {
      approvalInfo.value = undefined;
      currentApprovalNode.value = undefined;
      return;
    }

    try {
      approvalInfo.value = await getApprovalResourceDetail(sourceId.value);
      currentApprovalNodeIndex.value =
        approvalInfo.value?.nodes.findIndex((node) => node.nodeId === approvalInfo.value?.currentNodeId) ?? 0;
      currentApprovalNode.value = approvalInfo.value?.nodes[currentApprovalNodeIndex.value];
    } catch (error) {
      approvalInfo.value = undefined;
      currentApprovalNode.value = undefined;
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  const { reviewByResourceId, revokeByResourceId } = useApprovalResourceAction({
    formKey: FormDesignKeyEnum.CUSTOMER,
  });
  const refreshDetail = async () => {
    await initFormDescription();
    await initApprovalDetail();
  };
  const { resolveRowActions, initApprovalPermission, deleteExecute, enableApproval } = useApprovalOperation({
    formType: FormDesignKeyEnum.CUSTOMER,
    ignoreRolePermissionCheck: true,
    onReview: (row) => reviewByResourceId(row.id, { onSuccess: refreshDetail }),
    onRevoke: (row) => revokeByResourceId(row.id, { onSuccess: refreshDetail }),
  });

  const poolId = computed(() => route.query.poolId?.toString() ?? '');
  const poolActions = computed<CrmActionButtonsItem[]>(() => [
    ...(poolId.value
      ? [
          {
            key: 'pick',
            text: t('common.pick'),
            permission: ['CUSTOMER_MANAGEMENT_POOL:PICK'],
          },
        ]
      : []),
    {
      key: 'distribute',
      text: t('common.distribute'),
      permission: ['CUSTOMER_MANAGEMENT_POOL:ASSIGN'],
    },
  ]);
  const deleteAction: CrmActionButtonsItem = {
    key: 'delete',
    text: t('common.delete'),
    color: 'var(--error-red)',
    permission: ['CUSTOMER_MANAGEMENT:DELETE'],
  };
  const approvalStatus = computed(() => approvalInfo.value?.approvalStatus ?? detail.value.approvalStatus);
  const renderDescriptions = computed(() => {
    const approvalStatusDescription = enableApproval.value
      ? [
          {
            label: t('workbench.approvalStatus'),
            value: approvalStatus.value,
            valueSlotName: 'approvalStatus',
          },
        ]
      : [];

    return [...approvalStatusDescription, ...descriptions.value];
  });
  const operationActions = computed(() => {
    const approvalActions = resolveRowActions(detail.value, [deleteAction]);
    const reviewActions = approvalActions.filter((action) => ['review', 'revoke'].includes(action.key));
    const approvedDeleteAction = approvalActions.find((action) => action.key === 'delete');
    const poolDeleteAction =
      approvedDeleteAction && hasAnyPermission(['CUSTOMER_MANAGEMENT_POOL:DELETE'])
        ? {
            ...approvedDeleteAction,
            permission: ['CUSTOMER_MANAGEMENT_POOL:DELETE'],
          }
        : undefined;

    return [...reviewActions, ...poolActions.value, ...(poolDeleteAction ? [poolDeleteAction] : [])];
  });
  const showDetailActions = computed(() => Boolean(operationActions.value.length));
  const showApprovalActions = computed(() => approvalInfo.value?.approvalStatus === ProcessStatusEnum.APPROVING);
  const showFooterActions = computed(
    () => ['info', 'approval'].includes(activeTab.value) && (showDetailActions.value || showApprovalActions.value)
  );

  function getFooterActions(approvalActions: CrmActionButtonsItem[]) {
    const hasCancelApplyAction = approvalActions.some((action) => action.key === 'cancelApply');
    const detailActions = showDetailActions.value
      ? operationActions.value.filter((action) => action.key !== 'revoke' || !hasCancelApplyAction)
      : [];

    return [...approvalActions, ...detailActions];
  }

  async function handlePick() {
    try {
      showLoadingToast(t('common.picking'));
      await pickOpenSeaCustomer({
        customerId: sourceId.value,
        poolId: poolId.value,
      });
      showSuccessToast(t('common.pickSuccess'));
      await sleep(300);
      router.back();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      closeToast();
    }
  }

  function handleDistribute() {
    router.push({
      name: CustomerRouteEnum.CUSTOMER_DISTRIBUTE,
      query: {
        id: sourceId.value,
        apiKey: FormDesignKeyEnum.CUSTOMER,
      },
    });
  }

  function handleDelete() {
    showConfirmDialog({
      title: t('customer.deleteTitle'),
      message: t('customer.deleteTip'),
      confirmButtonText: deleteExecute.value ? t('crm.approval.confirmAndSubmitReview') : t('common.confirmDelete'),
      confirmButtonColor: 'var(--error-red)',
      beforeClose: async (action) => {
        if (action !== 'confirm') {
          return Promise.resolve(true);
        }

        try {
          showLoadingToast(t('common.deleting'));
          await deleteOpenSeaCustomer(sourceId.value);
          showSuccessToast(deleteExecute.value ? t('common.reviewSuccess') : t('common.deleteSuccess'));
          if (deleteExecute.value) {
            await refreshDetail();
          } else {
            router.back();
          }
          return Promise.resolve(true);
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
          return Promise.resolve(false);
        } finally {
          closeToast();
        }
      },
    });
  }

  function handleSelect(key: string) {
    switch (key) {
      case 'pick':
        handlePick();
        break;
      case 'distribute':
        handleDistribute();
        break;
      case 'delete':
        handleDelete();
        break;
      case 'review':
        reviewByResourceId(sourceId.value, { onSuccess: refreshDetail });
        break;
      case 'revoke':
        revokeByResourceId(sourceId.value, { onSuccess: refreshDetail });
        break;
      default:
        break;
    }
  }

  onBeforeMount(async () => {
    initApprovalPermission();
    await initFormConfig();
    await refreshDetail();
  });

  onActivated(() => {
    if (['info', 'approval'].includes(activeTab.value)) {
      refreshDetail();
    }
  });
</script>

<style lang="less" scoped>
  :deep(.crm-page-content) {
    @apply !overflow-hidden;
  }
  .detail-tabs {
    @apply flex-1 overflow-hidden;
    :deep(.van-hairline--top-bottom) {
      margin-top: -0.5px;
    }
    :deep(.van-tabs__content) {
      height: calc(100% - var(--van-tabs-line-height));
      .van-tab__panel {
        @apply h-full;
      }
    }
  }
</style>
