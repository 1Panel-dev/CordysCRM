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
        <CrmContactList
          v-else-if="tab.name === 'contact'"
          :source-id="route.query.id?.toString()"
          :customer-name="route.query.name?.toString()"
          :form-key="FormDesignKeyEnum.CUSTOMER_CONTACT"
          :readonly="!hasAnyPermission(['CUSTOMER_MANAGEMENT:UPDATE']) || collaborationType === 'READ_ONLY'"
        />
        <CrmFollowRecordList
          v-else-if="tab.name === 'record'"
          ref="recordListRef"
          :source-id="sourceId"
          :type="FormDesignKeyEnum.FOLLOW_RECORD_CUSTOMER"
          :initial-source-name="sourceName"
          :readonly="!hasAnyPermission(['CUSTOMER_MANAGEMENT:UPDATE']) || collaborationType === 'READ_ONLY'"
        />
        <CrmFollowPlanList
          v-else-if="tab.name === 'plan'"
          ref="planListRef"
          :source-id="sourceId"
          :type="FormDesignKeyEnum.FOLLOW_PLAN_CUSTOMER"
          :initial-source-name="sourceName"
          :readonly="!hasAnyPermission(['CUSTOMER_MANAGEMENT:UPDATE']) || collaborationType === 'READ_ONLY'"
        />
        <relation
          v-else-if="tab.name === 'relation'"
          ref="relationListRef"
          :source-id="sourceId"
          :readonly="!hasAnyPermission(['CUSTOMER_MANAGEMENT:UPDATE']) || collaborationType === 'READ_ONLY'"
        />
        <collaborator v-else-if="tab.name === 'collaborator'" ref="collaboratorListRef" :source-id="sourceId" />
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
          />
        </div>
        <CrmHeaderList v-else :source-id="sourceId" :load-list-api="getCustomerHeaderList" />
      </van-tab>
    </van-tabs>
    <template v-if="showFooterActions" #footer>
      <CrmActionButtons
        v-if="activeTab === 'info'"
        :show-edit-button="showEditButton"
        :actions="operationActions"
        @select="handleMoreSelect"
      />
      <CrmApprovalRecordActions
        v-if="activeTab === 'approval' && approvalInfo?.approvalStatus === ProcessStatusEnum.APPROVING"
        :source-id="sourceId"
        :form-key="FormDesignKeyEnum.CUSTOMER"
        :approval-info="approvalInfo"
        :current-approval-node="currentApprovalNode"
        :current-approval-node-index="currentApprovalNodeIndex"
        @refresh="refreshDetail"
      />
    </template>
  </CrmPageWrapper>
</template>

<script setup lang="ts">
  import { useRoute, useRouter } from 'vue-router';
  import { showConfirmDialog, showSuccessToast } from 'vant';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { ProcessStatusEnum } from '@lib/shared/enums/process';
  import { ReasonTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { ApprovalDetail, ApprovalNode } from '@lib/shared/models/system/process';

  import CrmDescription from '@/components/pure/crm-description/index.vue';
  import CrmPageWrapper from '@/components/pure/crm-page-wrapper/index.vue';
  import ApprovalStatus from '@/components/business/crm-approval/crm-approval-status.vue';
  import CrmApprovalLine from '@/components/business/crm-approval/crm-approval-line.vue';
  import CrmApprovalRecordActions from '@/components/business/crm-approval/crm-approval-record-actions.vue';
  import CrmActionButtons, { CrmActionButtonsItem } from '@/components/business/crm-action-buttons/index.vue';
  import CrmContactList from '@/components/business/crm-contact-list/index.vue';
  import CrmFollowPlanList from '@/components/business/crm-follow-list/followPlan.vue';
  import CrmFollowRecordList from '@/components/business/crm-follow-list/followRecord.vue';
  import CrmHeaderList from '@/components/business/crm-header-list/index.vue';
  import collaborator from './components/collaborator.vue';
  import relation from './components/relation.vue';

  import { deleteCustomer, getApprovalResourceDetail, getCustomerHeaderList } from '@/api/modules';
  import useApprovalOperation from '@/hooks/useApprovalOperation';
  import useApprovalResourceAction from '@/hooks/useApprovalResourceAction';
  import useFormCreateApi from '@/hooks/useFormCreateApi';
  import { hasAnyPermission } from '@/utils/permission';

  import { ClueRouteEnum, CommonRouteEnum, CustomerRouteEnum } from '@/enums/routeEnum';

  defineOptions({
    name: CustomerRouteEnum.CUSTOMER_DETAIL,
  });
  const route = useRoute();
  const router = useRouter();
  const { t } = useI18n();

  const sourceId = computed(() => route.query.id?.toString() ?? '');
  const { sourceName, descriptions, collaborationType, detail, initFormConfig, initFormDescription } = useFormCreateApi(
    {
      formKey: FormDesignKeyEnum.CUSTOMER,
      sourceId,
      needInitDetail: true,
    }
  );
  const { reviewByResourceId, revokeByResourceId } = useApprovalResourceAction({
    formKey: FormDesignKeyEnum.CUSTOMER,
  });
  const refreshDetail = async () => {
    initFormDescription();
    await initApprovalDetail();
  };
  const { resolveRowActions, hasApprovalScopedPermission, initApprovalPermission, deleteExecute, enableApproval } =
    useApprovalOperation({
      formType: FormDesignKeyEnum.CUSTOMER,
      onReview: (row) => reviewByResourceId(row.id, { onSuccess: refreshDetail }),
      onRevoke: (row) => revokeByResourceId(row.id, { onSuccess: refreshDetail }),
    });

  const activeTab = ref('info');
  const tabList = computed(() => {
    const fullTabList = [
      {
        name: 'info',
        title: t('customer.info'),
      },
      {
        name: 'contact',
        title: t('menu.contact'),
      },
      {
        name: 'record',
        title: t('common.record'),
      },
      {
        name: 'plan',
        title: t('common.plan'),
      },
      {
        name: 'header',
        title: t('customer.headerRecord'),
      },
      {
        name: 'relation',
        title: t('customer.relation'),
      },
      {
        name: 'collaborator',
        title: t('customer.collaborator'),
      },
      ...(approvalInfo.value
        ? [
            {
              name: 'approval',
              title: t('workbench.approval.record'),
            },
          ]
        : []),
    ];
    if (collaborationType.value) {
      return fullTabList.filter((item) => item.name !== 'collaborator');
    }
    return fullTabList;
  });

  const actions = ref<CrmActionButtonsItem[]>([
    {
      key: 'moveToPool',
      text: t('customer.moveToOpenSea'),
      permission: ['CUSTOMER_MANAGEMENT:RECYCLE'],
    },
    {
      key: 'transfer',
      text: t('common.transfer'),
      permission: ['CUSTOMER_MANAGEMENT:TRANSFER'],
    },
    {
      key: 'delete',
      text: t('common.delete'),
      color: 'var(--error-red)',
      permission: ['CUSTOMER_MANAGEMENT:DELETE'],
    },
  ]);
  const approvalStatus = computed(() => approvalInfo.value?.approvalStatus ?? detail.value.approvalStatus);
  const renderDescriptions = computed(() => {
    const approvalStatusDescription =
      enableApproval.value && approvalStatus.value && approvalStatus.value !== ProcessStatusEnum.NONE
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
  const showEditButton = computed(() => hasApprovalScopedPermission(detail.value, ['CUSTOMER_MANAGEMENT:UPDATE']));
  const operationActions = computed(() => resolveRowActions(detail.value, actions.value));
  const showFooterActions = computed(
    () =>
      (activeTab.value === 'info' && (showEditButton.value || operationActions.value.length)) ||
      (activeTab.value === 'approval' && approvalInfo.value?.approvalStatus === ProcessStatusEnum.APPROVING)
  );

  function handleEdit(id: string) {
    router.push({
      name: CommonRouteEnum.FORM_CREATE,
      query: {
        id,
        formKey: FormDesignKeyEnum.CUSTOMER,
        needInitDetail: 'Y',
      },
    });
  }

  function handleTransfer(id: string) {
    router.push({
      name: CustomerRouteEnum.CUSTOMER_TRANSFER,
      query: {
        id,
        apiKey: FormDesignKeyEnum.CUSTOMER,
      },
    });
  }

  function handleDelete(id: string) {
    showConfirmDialog({
      title: t('customer.deleteTitle'),
      message: t('customer.deleteTip'),
      confirmButtonText: deleteExecute.value ? t('crm.approval.confirmAndSubmitReview') : t('common.confirmDelete'),
      confirmButtonColor: 'var(--error-red)',
      beforeClose: async (action) => {
        if (action === 'confirm') {
          try {
            await deleteCustomer(id);
            showSuccessToast(deleteExecute.value ? t('common.reviewSuccess') : t('common.deleteSuccess'));
            router.back();
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

  function handleMoveToPool() {
    router.push({
      name: ClueRouteEnum.MOVE_TO_POOL,
      query: {
        id: sourceId.value,
        name: sourceName.value,
        reasonKey: ReasonTypeEnum.CUSTOMER_POOL_RS,
      },
    });
  }

  function handleMoreSelect(key: string) {
    switch (key) {
      case 'edit':
        handleEdit(sourceId.value);
        break;
      case 'transfer':
        handleTransfer(sourceId.value);
        break;
      case 'delete':
        handleDelete(sourceId.value);
        break;
      case 'moveToPool':
        handleMoveToPool();
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

  const recordListRef = ref<InstanceType<typeof CrmFollowRecordList>[]>();
  const planListRef = ref<InstanceType<typeof CrmFollowPlanList>[]>();
  const relationListRef = ref<InstanceType<typeof relation>[]>();
  const collaboratorListRef = ref<InstanceType<typeof collaborator>[]>();
  const approvalInfo = ref<ApprovalDetail>();
  const currentApprovalNode = ref<ApprovalNode>();
  const currentApprovalNodeIndex = ref(0);

  async function initApprovalDetail() {
    if (!sourceId.value) {
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

  onBeforeMount(async () => {
    initApprovalPermission();
    await initFormConfig();
    initFormDescription();
    initApprovalDetail();
  });

  onActivated(() => {
    if (activeTab.value === 'record') {
      recordListRef.value?.[0].loadList();
    } else if (activeTab.value === 'plan') {
      planListRef.value?.[0].loadList();
    } else if (activeTab.value === 'relation') {
      relationListRef.value?.[0].initList();
    } else if (activeTab.value === 'collaborator') {
      collaboratorListRef.value?.[0].initList();
    } else if (activeTab.value === 'info') {
      initFormDescription();
      initApprovalDetail();
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
