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
          <CrmDescription :description="renderDescriptions" :source-id="sourceId">
            <template #approvalStatus>
              <ApprovalStatus :status="approvalStatus" />
            </template>
          </CrmDescription>
        </div>
        <CrmFollowRecordList
          v-else-if="tab.name === 'record'"
          ref="recordListRef"
          :source-id="sourceId"
          :type="FormDesignKeyEnum.FOLLOW_RECORD_CLUE"
          :readonly="readonly"
          :initial-source-name="sourceName"
        />
        <CrmFollowPlanList
          v-else-if="tab.name === 'plan'"
          ref="planListRef"
          :source-id="sourceId"
          :type="FormDesignKeyEnum.FOLLOW_PLAN_CLUE"
          :readonly="readonly"
          :initial-source-name="sourceName"
        />
        <CrmHeaderList v-else-if="tab.name === 'header'" :load-list-api="getClueHeaderList" :source-id="sourceId" />
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
        :form-key="FormDesignKeyEnum.CLUE"
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
  import { sleep } from '@lib/shared/method';
  import type { ApprovalDetail, ApprovalNode } from '@lib/shared/models/system/process';

  import CrmDescription from '@/components/pure/crm-description/index.vue';
  import CrmPageWrapper from '@/components/pure/crm-page-wrapper/index.vue';
  import ApprovalStatus from '@/components/business/crm-approval/crm-approval-status.vue';
  import CrmApprovalLine from '@/components/business/crm-approval/crm-approval-line.vue';
  import CrmApprovalRecordActions from '@/components/business/crm-approval/crm-approval-record-actions.vue';
  import CrmActionButtons, { CrmActionButtonsItem } from '@/components/business/crm-action-buttons/index.vue';
  import CrmFollowPlanList from '@/components/business/crm-follow-list/followPlan.vue';
  import CrmFollowRecordList from '@/components/business/crm-follow-list/followRecord.vue';
  import CrmHeaderList from '@/components/business/crm-header-list/index.vue';

  import { deleteClue, getApprovalResourceDetail, getClueHeaderList } from '@/api/modules';
  import useApprovalOperation from '@/hooks/useApprovalOperation';
  import useApprovalResourceAction from '@/hooks/useApprovalResourceAction';
  import useFormCreateApi from '@/hooks/useFormCreateApi';
  import { hasAnyPermission } from '@/utils/permission';

  import { ClueRouteEnum, CommonRouteEnum, CustomerRouteEnum } from '@/enums/routeEnum';

  defineOptions({
    name: ClueRouteEnum.CLUE_DETAIL,
  });
  const route = useRoute();
  const { t } = useI18n();
  const router = useRouter();

  const activeTab = ref('info');
  const tabList = computed(() => [
    {
      name: 'info',
      title: t('clue.info'),
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

  const { sourceName, descriptions, detail, initFormConfig, initFormDescription } = useFormCreateApi({
    formKey: FormDesignKeyEnum.CLUE,
    sourceId,
    needInitDetail: true,
  });
  const { reviewByResourceId, revokeByResourceId } = useApprovalResourceAction({
    formKey: FormDesignKeyEnum.CLUE,
  });
  const refreshDetail = async () => {
    initFormDescription();
    await initApprovalDetail();
  };
  const { resolveRowActions, hasApprovalScopedPermission, initApprovalPermission, deleteExecute, enableApproval } =
    useApprovalOperation({
      formType: FormDesignKeyEnum.CLUE,
      onReview: (row) => reviewByResourceId(row.id, { onSuccess: refreshDetail }),
      onRevoke: (row) => revokeByResourceId(row.id, { onSuccess: refreshDetail }),
    });
  onBeforeMount(async () => {
    initApprovalPermission();
    await initFormConfig();
    initFormDescription();
    initApprovalDetail();
  });

  const actions = ref<CrmActionButtonsItem[]>([
    {
      key: 'transfer',
      text: t('common.transfer'),
      permission: ['CLUE_MANAGEMENT:TRANSFER'],
    },
    {
      key: 'moveToPool',
      text: t('clue.moveIntoCluePool'),
      permission: ['CLUE_MANAGEMENT:RECYCLE'],
    },
    {
      key: 'convert',
      text: t('clue.convert'),
      permission: ['CLUE_MANAGEMENT:UPDATE'],
    },
    {
      key: 'delete',
      text: t('common.delete'),
      color: 'var(--error-red)',
      permission: ['CLUE_MANAGEMENT:DELETE'],
    },
  ]);
  const hasTransitioned = computed(() => Boolean(detail.value.transitionType || route.query.transitionType));
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
  const showEditButton = computed(() => hasApprovalScopedPermission(detail.value, ['CLUE_MANAGEMENT:UPDATE']));
  const operationActions = computed(() => resolveRowActions(detail.value, actions.value));
  const showFooterActions = computed(
    () =>
      (activeTab.value === 'info' &&
        !hasTransitioned.value &&
        (showEditButton.value || operationActions.value.length)) ||
      (activeTab.value === 'approval' && approvalInfo.value?.approvalStatus === ProcessStatusEnum.APPROVING)
  );

  function handleEdit(id: string) {
    router.push({
      name: CommonRouteEnum.FORM_CREATE,
      query: {
        id,
        formKey: FormDesignKeyEnum.CLUE,
        needInitDetail: 'Y',
      },
    });
  }

  function handleTransfer(id: string) {
    router.push({
      name: CustomerRouteEnum.CUSTOMER_TRANSFER,
      query: {
        id,
        apiKey: FormDesignKeyEnum.CLUE,
      },
    });
  }

  function convertTo(id: string, name: string) {
    router.push({
      name: ClueRouteEnum.CONVERT,
      query: {
        clueId: id,
        clueName: name,
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
            await deleteClue(id);
            showSuccessToast(deleteExecute.value ? t('common.reviewSuccess') : t('common.deleteSuccess'));
            await sleep(300);
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
        reasonKey: ReasonTypeEnum.CLUE_POOL_RS,
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
      case 'convert':
        convertTo(sourceId.value, sourceName.value as string);
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

  const readonly = computed(() => !hasAnyPermission(['CLUE_MANAGEMENT:UPDATE']));

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
