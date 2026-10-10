<template>
  <CrmPageWrapper :title="route.query.name?.toString() || ''">
    <van-tabs v-model:active="activeTab" border class="detail-tabs">
      <van-tab v-for="tab of tabList" :key="tab.name" :name="tab.name">
        <template #title>
          <div class="text-[16px]" :class="activeTab === tab.name ? 'text-[var(--primary-8)]' : ''">
            {{ tab.title }}
          </div>
        </template>
        <div v-if="tab.name === 'info'" class="relative h-full overflow-auto bg-[var(--text-n9)]">
          <div class="bg-[var(--text-n9)] p-[16px]">
            <CrmWorkflowCard
              v-model:stage="currentStatus"
              :stage-config-list="stageConfig?.stageConfigList || []"
              :form-stage-key="FormDesignKeyEnum.BUSINESS"
              :show-confirm-status="true"
              :title="t('opportunity.progress')"
              is-limit-back
              :back-stage-permission="['OPPORTUNITY_MANAGEMENT:UPDATE', 'OPPORTUNITY_MANAGEMENT:RESIGN']"
              :source-id="sourceId"
              :operation-permission="['OPPORTUNITY_MANAGEMENT:UPDATE']"
              :readonly="!canUpdateOpportunity"
              :failure-reason="lastFailureReason"
              :afoot-roll-back="stageConfig?.afootRollBack"
              :end-roll-back="stageConfig?.endRollBack"
              @load-detail="() => initStage(true)"
            />
          </div>
          <CrmDescription :description="renderDescriptions" :source-id="sourceId">
            <template #approvalStatus>
              <ApprovalStatus :status="approvalStatus" />
            </template>
          </CrmDescription>
        </div>
        <CrmContactList
          v-else-if="tab.name === 'contact'"
          :source-id="route.query.id?.toString()"
          :form-key="FormDesignKeyEnum.BUSINESS_CONTACT"
          readonly
        />
        <CrmFollowRecordList
          v-else-if="tab.name === 'record'"
          ref="recordListRef"
          :source-id="sourceId"
          :type="FormDesignKeyEnum.FOLLOW_RECORD_BUSINESS"
          :readonly="readonly"
          :initial-source-name="initialSourceName"
        />
        <CrmFollowPlanList
          v-else-if="tab.name === 'plan'"
          ref="planListRef"
          :source-id="sourceId"
          :type="FormDesignKeyEnum.FOLLOW_PLAN_BUSINESS"
          :readonly="readonly"
          :initial-source-name="initialSourceName"
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
          />
        </div>
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
        :form-key="FormDesignKeyEnum.BUSINESS"
        :approval-info="approvalInfo"
        :current-approval-node="currentApprovalNode"
        :current-approval-node-index="currentApprovalNodeIndex"
        @refresh="refreshDetail"
      />
    </template>
  </CrmPageWrapper>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import { showConfirmDialog, showSuccessToast } from 'vant';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { ProcessStatusEnum } from '@lib/shared/enums/process';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { OpportunityStageConfig } from '@lib/shared/models/opportunity';
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
  import CrmWorkflowCard from '@/components/business/crm-workflow-card/index.vue';

  import { deleteOpt, getApprovalResourceDetail, getOpportunityStageConfig } from '@/api/modules';
  import useApprovalOperation from '@/hooks/useApprovalOperation';
  import useApprovalResourceAction from '@/hooks/useApprovalResourceAction';
  import useFormCreateApi from '@/hooks/useFormCreateApi';
  import { hasAllPermission } from '@/utils/permission';

  import { CommonRouteEnum, CustomerRouteEnum, OpportunityRouteEnum } from '@/enums/routeEnum';

  defineOptions({
    name: OpportunityRouteEnum.OPPORTUNITY_DETAIL,
  });
  const route = useRoute();
  const { t } = useI18n();
  const router = useRouter();

  const activeTab = ref('info');
  const tabList = computed(() => [
    {
      name: 'info',
      title: t('opportunity.info'),
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
  const stageConfig = ref<OpportunityStageConfig>();
  async function initStageConfig() {
    try {
      stageConfig.value = await getOpportunityStageConfig();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }
  const { descriptions, initFormConfig, initFormDescription, detail } = useFormCreateApi({
    formKey: FormDesignKeyEnum.BUSINESS,
    sourceId,
    needInitDetail: true,
  });
  const { reviewByResourceId, revokeByResourceId } = useApprovalResourceAction({
    formKey: FormDesignKeyEnum.BUSINESS,
  });
  const refreshDetail = async () => {
    initFormDescription();
    await initApprovalDetail();
  };
  const { resolveRowActions, hasApprovalScopedPermission, initApprovalPermission, deleteExecute, enableApproval } =
    useApprovalOperation({
      formType: FormDesignKeyEnum.BUSINESS,
      onReview: (row) => reviewByResourceId(row.id, { onSuccess: refreshDetail }),
      onRevoke: (row) => revokeByResourceId(row.id, { onSuccess: refreshDetail }),
    });

  const initialSourceName = computed(() => {
    const { customerName, customerId, name } = detail.value;

    return JSON.stringify({
      name,
      customerName,
      customerId,
    });
  });

  const currentStatus = ref<string>(stageConfig.value?.stageConfigList?.[0].id || '');

  const lastFailureReason = ref('');
  async function initStage(isInit = false) {
    if (isInit) {
      initFormDescription();
    }

    const { stage, failureReason } = detail.value;
    currentStatus.value = stage;
    lastFailureReason.value = failureReason;
  }

  const isSuccess = computed(
    () =>
      currentStatus.value === stageConfig.value?.stageConfigList.find((e) => e.type === 'END' && e.rate === '100')?.id
  );
  const isFail = computed(
    () => currentStatus.value === stageConfig.value?.stageConfigList.find((e) => e.type === 'END' && e.rate === '0')?.id
  );

  const readonly = computed(() => !hasAllPermission(['OPPORTUNITY_MANAGEMENT:UPDATE']));
  const canUpdateOpportunity = computed(() =>
    hasApprovalScopedPermission(detail.value, ['OPPORTUNITY_MANAGEMENT:UPDATE', 'OPPORTUNITY_MANAGEMENT:RESIGN'])
  );

  const showEditButton = computed(() => {
    if (isFail.value) return false;
    if (isSuccess.value) {
      return (
        hasAllPermission(['OPPORTUNITY_MANAGEMENT:UPDATE', 'OPPORTUNITY_MANAGEMENT:RESIGN']) &&
        hasApprovalScopedPermission(detail.value, ['OPPORTUNITY_MANAGEMENT:RESIGN'])
      );
    }
    return canUpdateOpportunity.value;
  });
  const actions = computed<CrmActionButtonsItem[]>(() => {
    const transferAction: CrmActionButtonsItem[] = [
      {
        key: 'transfer',
        text: t('common.transfer'),
        permission: ['OPPORTUNITY_MANAGEMENT:TRANSFER'],
      },
    ];

    const deleteAction: CrmActionButtonsItem[] = [
      {
        key: 'delete',
        text: t('common.delete'),
        color: 'var(--error-red)',
        permission: ['OPPORTUNITY_MANAGEMENT:DELETE'],
      },
    ];

    return resolveRowActions(detail.value, isSuccess.value ? [...deleteAction] : [...transferAction, ...deleteAction]);
  });
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
  const operationActions = computed(() => actions.value);
  const showFooterActions = computed(
    () =>
      (activeTab.value === 'info' && (showEditButton.value || operationActions.value.length)) ||
      (activeTab.value === 'approval' && approvalInfo.value?.approvalStatus === ProcessStatusEnum.APPROVING)
  );

  function handleTransfer(id: string) {
    router.push({
      name: CustomerRouteEnum.CUSTOMER_TRANSFER,
      query: {
        id,
        apiKey: FormDesignKeyEnum.BUSINESS,
      },
    });
  }

  function handleMoreSelect(key: string) {
    switch (key) {
      case 'edit':
        router.push({
          name: CommonRouteEnum.FORM_CREATE,
          query: {
            id: sourceId.value,
            formKey: FormDesignKeyEnum.BUSINESS,
            needInitDetail: 'Y',
          },
        });
        break;
      case 'transfer':
        handleTransfer(sourceId.value);
        break;
      case 'delete':
        showConfirmDialog({
          title: t('opportunity.deleteTitle'),
          message: t('opportunity.deleteContentTip'),
          confirmButtonText: deleteExecute.value ? t('crm.approval.confirmAndSubmitReview') : t('common.confirmDelete'),
          confirmButtonColor: 'var(--error-red)',
          beforeClose: async (action) => {
            if (action === 'confirm') {
              try {
                await deleteOpt(sourceId.value);
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

  onActivated(() => {
    if (activeTab.value === 'record') {
      recordListRef.value?.[0].loadList();
    } else if (activeTab.value === 'plan') {
      planListRef.value?.[0].loadList();
    } else if (activeTab.value === 'info') {
      initFormDescription();
      initApprovalDetail();
    }
  });

  onBeforeMount(async () => {
    initApprovalPermission();
    initStageConfig();
    await initFormConfig();
    initFormDescription();
    initApprovalDetail();
  });

  watch([() => detail.value.stage, () => detail.value.lastStage], () => {
    initStage(false);
  });
</script>

<style lang="less" scoped>
  :deep(.crm-page-content) {
    @apply !overflow-hidden;
  }
  .detail-tabs {
    @apply flex-1 overflow-hidden;
    :deep(.van-tabs__content) {
      height: calc(100% - var(--van-tabs-line-height));
      .van-tab__panel {
        @apply h-full;
      }
    }
  }
</style>
