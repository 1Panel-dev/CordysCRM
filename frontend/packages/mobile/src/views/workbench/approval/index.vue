<template>
  <CrmPageWrapper :title="sourceName || ''">
    <div class="relative h-full overflow-auto bg-[var(--text-n9)] pt-[16px]">
      <CrmDescription :description="renderDescriptions">
        <template #approvalStatus>
          <ApprovalStatus v-if="approvalInfo" :status="approvalInfo?.approvalStatus" />
        </template>
        <template #quotationStatus="{ item }">
          <CrmTag :tag="item.value ? t('common.voided') : t('common.normal')" />
        </template>
      </CrmDescription>
    </div>
    <template #footer>
      <div
        v-if="
          approvalInfo?.approvalStatus === ProcessStatusEnum.APPROVING &&
          (isApprover || canCancelApply || canCancelApproval)
        "
        class="flex justify-between gap-[16px]"
      >
        <van-button v-if="canShowMore" plain type="primary" block @click="showMore = true">
          {{ t('common.more') }}
        </van-button>
        <van-button
          v-if="isApprover"
          plain
          type="danger"
          @click="
            () => {
              isRejecting = true;
              showApprovalPopup = true;
            }
          "
          block
        >
          {{ t('workbench.operation.REJECT') }}
        </van-button>
        <van-button
          v-if="isApprover"
          type="primary"
          block
          @click="
            () => {
              isRejecting = false;
              showApprovalPopup = true;
            }
          "
        >
          {{ t('workbench.operation.APPROVE') }}
        </van-button>
      </div>
    </template>
  </CrmPageWrapper>
  <ApprovalPopup
    v-model:show="showApprovalPopup"
    :approving-item="approvingItem"
    :is-rejecting="isRejecting"
    :resource-type="route.query.formKey?.toString().replace('Snapshot', '') || ''"
    @refresh="router.back()"
  />
  <FallbackPopup
    v-model:show="showFallbackPopup"
    :approving-item="approvingItem"
    :is-rejecting="isRejecting"
    :resource-type="route.query.formKey?.toString().replace('Snapshot', '') || ''"
    :approval-config="approvalConfig"
    :fallbackOptions="fallbackOptions"
    @refresh="refresh"
  />
  <AddSignPopup
    v-model:show="showAddSignPopup"
    :approving-item="approvingItem"
    :is-rejecting="isRejecting"
    :resource-type="route.query.formKey?.toString().replace('Snapshot', '') || ''"
    :approval-config="approvalConfig"
    :fallbackOptions="fallbackOptions"
    @refresh="refresh"
  />
  <van-popup v-model:show="showMore" position="bottom">
    <template v-if="fallbackOptions.length && isApprover">
      <van-button
        class="rounded-none border-none"
        @click="
          () => {
            showMore = false;
            showFallbackPopup = true;
          }
        "
        block
      >
        {{ t('workbench.operation.BACK') }}
      </van-button>
      <van-divider class="!m-0" />
    </template>
    <template v-if="approvalConfig?.allowAddSign && isApprover">
      <van-button
        class="rounded-none border-none"
        @click="
          () => {
            showMore = false;
            showAddSignPopup = true;
          }
        "
        block
      >
        {{ t('workbench.operation.SIGN') }}
      </van-button>
      <van-divider class="!m-0" />
    </template>
    <template v-if="canCancelApproval">
      <van-button class="rounded-none border-none" @click="cancelApproval" block>
        {{ t('workbench.cancelApproval') }}
      </van-button>
      <van-divider class="!m-0" />
    </template>
    <template v-if="canCancelApply">
      <van-button class="rounded-none border-none" @click="() => cancelApproval('apply')" block>
        {{ t('workbench.canApply') }}
      </van-button>
      <van-divider class="!m-0" />
    </template>
  </van-popup>
</template>
<script setup lang="ts">
  import { getApprovalConfigDetail, getApprovalResourceDetail, revokeApproval, revokeResource } from '@/api/modules';
  import useUserStore from '@/store/modules/user';
  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { MultiApproverModeEnum, ProcessStatusEnum } from '@lib/shared/enums/process';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { ApprovalDetail, ApprovalNode, ApprovalProcessDetail } from '@lib/shared/models/system/process';
  import CrmDescription, { type CrmDescriptionItem } from '@/components/pure/crm-description/index.vue';
  import { useRoute } from 'vue-router';
  import useFormCreateApi from '@/hooks/useFormCreateApi';
  import ApprovalPopup from './approvalPopup.vue';
  import FallbackPopup from './fallbackPopup.vue';
  import AddSignPopup from './addSignPopup.vue';
  import { showConfirmDialog, showSuccessToast, type PickerOption } from 'vant';
  import { sleep } from '@lib/shared/method/index.js';
  import router from '@/router/index.js';
  import ApprovalStatus from './approvalStatus.vue';

  const { t } = useI18n();
  const userStore = useUserStore();
  const route = useRoute();

  const sourceId = computed(() => route.query.id?.toString() ?? '');

  const { sourceName, descriptions, detail, initFormConfig, initFormDescription } = useFormCreateApi({
    formKey: route.query.formKey as FormDesignKeyEnum,
    sourceId,
    needInitDetail: true,
    otherSaveParams: {
      approvalTaskId: route.query.taskId,
    },
  });

  const renderDescriptions = computed(
    () =>
      [
        {
          label: t('workbench.approvalStatus'),
          value: approvalInfo.value?.approvalStatus,
          valueSlotName: 'approvalStatus',
        },
        route.query.formKey === FormDesignKeyEnum.OPPORTUNITY_QUOTATION_SNAPSHOT
          ? {
              label: t('workbench.quotationStatus'),
              value: detail.value.invalid,
              valueSlotName: 'quotationStatus',
            }
          : null,
        ...descriptions.value,
      ].filter(Boolean) as CrmDescriptionItem[]
  );

  const approvalInfo = ref<ApprovalDetail>();
  const approvalConfig = ref<ApprovalProcessDetail>(); // 审批配置详情

  async function initApprovalConfig() {
    try {
      if (route.query.formKey) {
        approvalConfig.value = await getApprovalConfigDetail(route.query.formKey?.toString().replace('Snapshot', ''));
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  const currentApprovalNode = ref<ApprovalNode>();
  const currentApprovalNodeIndex = ref(0);
  const hasApprovalStatus = [ProcessStatusEnum.APPROVED, ProcessStatusEnum.UNAPPROVED, ProcessStatusEnum.NONE];
  // 我审批的上一个节点
  const prevMineApprovalNode = computed(() => {
    if (!currentApprovalNode.value) {
      return undefined;
    }
    const prevMineApprovalIndex = currentApprovalNode.value.taskNodes?.findIndex(
      (e) => hasApprovalStatus.includes(e.approvalStatus) && e.approverId === userStore.userInfo.id
    );
    if (prevMineApprovalIndex !== -1) {
      return currentApprovalNode.value?.taskNodes?.[prevMineApprovalIndex];
    }
    const prevApprovalNode = approvalInfo.value?.nodes[currentApprovalNodeIndex.value - 1];
    if (prevApprovalNode) {
      return prevApprovalNode.taskNodes.find(
        (e) => hasApprovalStatus.includes(e.approvalStatus) && e.approverId === userStore.userInfo.id
      );
    }
  });
  const currentTaskNode = computed(() => {
    if (!currentApprovalNode.value) {
      return undefined;
    }
    return currentApprovalNode.value.taskNodes?.find(
      (e) => e.approvalStatus === ProcessStatusEnum.APPROVING && e.approverId === userStore.userInfo.id
    );
  });
  // 只有当前审批中的人才展示编辑权限，其他节点展示只读权限
  // const filedPermission = computed(() => {
  //   if (currentTaskNode.value && approvalInfo.value?.currentNodeFieldPermissions) {
  //     return (JSON.parse(approvalInfo.value?.currentNodeFieldPermissions || '[]') as ApprovalFieldPermission[]) || [];
  //   }
  //   return [];
  // });

  // 是否是审批人
  const isApprover = computed(() => {
    if (currentApprovalNode.value?.multiApproverMode === MultiApproverModeEnum.SEQUENTIAL) {
      // 顺序审批，只有当前审批人可以操作
      return (
        currentApprovalNode.value?.taskNodes?.find((e) => e.approvalStatus === ProcessStatusEnum.APPROVING)
          ?.approverId === userStore.userInfo.id
      );
    }
    return currentApprovalNode.value?.taskNodes?.some(
      (taskNode) =>
        taskNode.approvalStatus === ProcessStatusEnum.APPROVING && taskNode.approverId === userStore.userInfo.id
    ); // 会签/或签/单人审批
  });
  // 是否可以撤销审批申请
  const canCancelApply = computed(() => {
    // 未配置撤销申请且第一个节点已完成审批则不允许
    if (
      !approvalConfig.value?.submitterCanRevoke &&
      approvalInfo.value?.nodes[0].approvalStatus !== ProcessStatusEnum.APPROVING
    ) {
      return false;
    }
    // 流程结束不允许撤销
    if (currentApprovalNode.value?.endNode) {
      return false;
    }
    // 只有提交人可以撤销审批申请，已撤销状态不显示撤销按钮
    if (
      approvalInfo.value?.submitterId !== userStore.userInfo.id ||
      route.query.approvalStatus === ProcessStatusEnum.REVOKED
    ) {
      return false;
    }
    // 当前没有审批完成节点时可撤销
    if (
      !approvalInfo.value?.nodes.some(
        (node) =>
          node.approvalStatus === ProcessStatusEnum.APPROVED || node.approvalStatus === ProcessStatusEnum.UNAPPROVED
      )
    ) {
      return true;
    }
    // 第一个节点完成审批，且配置了可撤销配置，允许撤销
    if (
      approvalInfo.value?.nodes[0].approvalStatus === ProcessStatusEnum.APPROVED &&
      approvalConfig.value?.submitterCanRevoke
    ) {
      return true;
    }
  });
  // 是否可以撤销审批
  const canCancelApproval = computed(() => {
    // 未配置撤销
    if (!approvalConfig.value?.allowWithdraw) {
      return false;
    }
    // 流程结束不允许撤销
    if (currentApprovalNode.value?.endNode) {
      return false;
    }
    // 当前节点包含自己审批的任务节点，则说明是多人审批节点且节点未结束，判断当前节点情况即可
    if (
      currentApprovalNode.value &&
      currentApprovalNode.value.taskNodes?.findIndex(
        (e) => hasApprovalStatus.includes(e.approvalStatus) && e.approverId === userStore.userInfo.id
      ) !== -1
    ) {
      // 当前是顺序审批节点，上一个审批节点是自己，并且下一个节点未审批，允许撤销自己的审批
      if (currentApprovalNode.value?.multiApproverMode === MultiApproverModeEnum.SEQUENTIAL) {
        const currentTaskNodeIndex = currentApprovalNode.value?.taskNodes?.findIndex(
          (e) => e.approvalStatus === ProcessStatusEnum.APPROVING
        );
        return currentApprovalNode.value?.taskNodes[currentTaskNodeIndex - 1]?.approverId === userStore.userInfo.id;
      }
      // 当前是会签节点，且自己已经审批时允许撤回
      if (
        currentApprovalNode.value.taskNodes?.findIndex(
          (e) => hasApprovalStatus.includes(e.approvalStatus) && e.approverId === userStore.userInfo.id
        ) !== -1
      ) {
        return true;
      }
    }
    // 当前节点未包含自己审批的任务节点，则判断上一个节点情况
    // 上一个节点非多人审批，是自己审批的，且当前节点下所有人都未审批
    if (approvalInfo.value?.nodes[currentApprovalNodeIndex.value - 1]?.taskNodes?.length === 1) {
      return (
        approvalInfo.value?.nodes[currentApprovalNodeIndex.value - 1].taskNodes[0].approverId ===
          userStore.userInfo.id &&
        currentApprovalNode.value?.taskNodes?.every((e) => e.approvalStatus === ProcessStatusEnum.APPROVING)
      );
    }
    // 上一个节点是多人审批且是或签，且是自己审批通过的，且当前节点下所有人都未审批
    if (
      approvalInfo.value?.nodes[currentApprovalNodeIndex.value - 1]?.multiApproverMode === MultiApproverModeEnum.ANY
    ) {
      return (
        approvalInfo.value?.nodes[currentApprovalNodeIndex.value - 1].taskNodes.some(
          (e) => hasApprovalStatus.includes(e.approvalStatus) && e.approverId === userStore.userInfo.id
        ) && currentApprovalNode.value?.taskNodes?.every((e) => e.approvalStatus === ProcessStatusEnum.APPROVING)
      );
    }
  });

  const noApproval = ref(false);
  async function initApprovalDetail() {
    try {
      approvalInfo.value = await getApprovalResourceDetail(route.query.id?.toString() || '');
      if (!approvalInfo.value) {
        noApproval.value = true;
      }
      currentApprovalNodeIndex.value = approvalInfo.value?.nodes.findIndex(
        (node) => node.nodeId === approvalInfo.value?.currentNodeId
      );
      currentApprovalNode.value = approvalInfo.value?.nodes[currentApprovalNodeIndex.value];
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  const showApprovalPopup = ref(false);
  const isRejecting = ref(false);
  const approvingItem = computed(() => {
    if (!currentApprovalNode.value || !currentTaskNode.value) return undefined;
    return {
      approvalTaskId: currentTaskNode.value.taskId,
      approvalNodeId: currentApprovalNode.value.nodeId,
      approvalInstanceId: approvalInfo.value?.id,
      approvalId: currentTaskNode.value.approverId,
    };
  });

  const showMore = ref(false);

  const showFallbackPopup = ref(false);
  const fallbackOptions = computed(() => {
    const options: PickerOption[] = [];
    for (let i = currentApprovalNodeIndex.value - 1; i >= 0; i--) {
      options.push({
        text: t('workbench.preNode', { index: currentApprovalNodeIndex.value - i }),
        value: approvalInfo.value?.nodes[i].nodeId || '',
      });
    }
    return options;
  });
  const showAddSignPopup = ref(false);
  const canShowMore = computed(
    () =>
      approvalConfig.value?.allowAddSign ||
      fallbackOptions.value.length ||
      canCancelApproval.value ||
      canCancelApply.value
  );

  function cancelApproval(type?: 'apply' | 'approval') {
    showMore.value = false;
    if (type === 'apply') {
      showConfirmDialog({
        title: t('workbench.cancelApprovalApplyConfirm'),
        message: t('workbench.cancelApprovalApplyTip'),
        confirmButtonText: t('workbench.confirmCancelApprovalApply'),
        confirmButtonColor: 'var(--error-red)',
        beforeClose: async (action) => {
          if (action === 'confirm') {
            try {
              await revokeResource({
                resourceId: route.query.id?.toString() || '',
                formKey: route.query.formKey?.toString().replace('Snapshot', '') || '',
              });
              showSuccessToast(t('workbench.cancelApprovalApplySuccess'));
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
    } else {
      showConfirmDialog({
        title: t('workbench.cancelApprovalConfirm'),
        message: t('workbench.cancelApprovalTip'),
        confirmButtonText: t('workbench.confirmCancelApproval'),
        confirmButtonColor: 'var(--error-red)',
        beforeClose: async (action) => {
          if (action === 'confirm') {
            try {
              await revokeApproval({
                id: prevMineApprovalNode.value?.taskId || '',
              });
              initFormDescription();
              initApprovalDetail();
              showSuccessToast(t('workbench.cancelApprovalSuccess'));
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
  }

  function refresh() {
    initApprovalConfig();
    initFormDescription();
    initApprovalDetail();
  }

  onBeforeMount(async () => {
    initApprovalConfig();
    await initFormConfig();
    initFormDescription();
    initApprovalDetail();
  });
</script>
<style lang="less" scoped></style>
