<template>
  <CrmCard hide-footer no-content-padding>
    <CrmSplitPanel
      v-if="props.approvalStatus && ![ProcessStatusEnum.PENDING, ProcessStatusEnum.NONE].includes(props.approvalStatus)"
      :size="0.7"
      :max="1"
      :min="0.7"
      :default-size="0.7"
      collapse-side="right"
      disabled
    >
      <template #1>
        <div class="flex h-full w-full p-[24px_8px_24px_24px]">
          <n-scrollbar :content-style="{ paddingRight: '8px' }" x-scrollable>
            <slot name="left" :fieldPermissions="filedPermission"></slot>
          </n-scrollbar>
        </div>
      </template>
      <template #2>
        <div v-if="noApproval" class="flex h-full w-full items-center justify-center border-l border-[var(--text-n8)]">
          {{ t('crm.approval.historyTip') }}
        </div>
        <div v-else class="flex h-full w-full flex-col overflow-hidden border-l border-[var(--text-n8)]">
          <div class="flex-1 overflow-hidden px-[16px] py-[24px] pb-[32px]">
            <div class="mb-[8px] text-[16px] font-semibold">{{ t('crm.approval.record') }}</div>
            <CrmApprovalLine
              :nodes="approvalInfo?.nodes || []"
              :submitter="{
                submitAvatar: approvalInfo?.submitAvatar,
                submitter: approvalInfo?.submitter,
                submitTime: approvalInfo?.submitTime,
                submitterId: approvalInfo?.submitterId,
              }"
              :currentApprovalNode="currentApprovalNode"
              :currentApprovalNodeIndex="currentApprovalNodeIndex"
              :finally-result="approvalInfo?.approvalStatus"
              class="pr-[8px]"
            />
          </div>
          <div
            v-if="
              approvalInfo?.approvalStatus === ProcessStatusEnum.APPROVING &&
              (isApprover || canCancelApply || canCancelApproval)
            "
            class="sticky bottom-0 border-t border-[var(--text-n8)] bg-[var(--text-n10)] p-[16px]"
          >
            <template v-if="isApprover">
              <div class="flex items-center gap-[4px]">
                <div class="mb-[8px] font-semibold">{{ t('crm.approval.opinion') }}</div>
                <span v-if="approvalConfig?.requireComment" class="text-[var(--error-red)]">*</span>
              </div>
              <CrmFileInput
                ref="CrmFileInputRef"
                v-model:value="approvalOpinion"
                v-model:file-list="fileList"
                :required="approvalConfig?.requireComment"
                :name="t('crm.approval.opinion')"
              />
              <div class="mt-[12px] flex gap-[12px]">
                <n-button type="primary" class="flex-1" :loading="approvalLoading" @click="handleApprove">
                  {{ t('common.approve') }}
                </n-button>
                <n-button type="error" ghost @click="handleReject">{{ t('common.reject') }}</n-button>
                <CrmMoreAction :options="moreActions" trigger="click" size="medium" @select="handleMoreActionSelect" />
              </div>
            </template>
            <n-button
              v-if="canCancelApply"
              type="primary"
              class="mt-[16px]"
              ghost
              block
              @click="cancelApproval('apply')"
            >
              <template #icon>
                <CrmIcon type="iconicon_rollfront" :size="16" />
              </template>
              {{ t('common.revoke') }}
            </n-button>
            <n-button
              v-if="canCancelApproval"
              type="primary"
              class="mt-[16px]"
              ghost
              block
              @click="() => cancelApproval()"
            >
              <template #icon>
                <CrmIcon type="iconicon_rollfront" :size="16" />
              </template>
              {{ t('crm.approval.cancelApproval') }}
            </n-button>
          </div>
        </div>
      </template>
    </CrmSplitPanel>
    <div v-else class="flex h-full w-full p-[24px_16px_24px_24px]">
      <n-scrollbar x-scrollable>
        <slot name="left" :fieldPermissions="[]"></slot>
      </n-scrollbar>
    </div>
  </CrmCard>
  <CrmModal
    v-model:show="addSignModalVisible"
    :title="t('common.COUNTERSIGNATURE')"
    :ok-loading="addSignLoading"
    :positive-text="addSignForm.type === 'AFTER' ? t('crm.approval.agreeAndAddSign') : t('crm.approval.confirmAddSign')"
    @confirm="handleAddSign"
  >
    <n-form
      ref="addSignFormRef"
      :model="addSignForm"
      label-placement="left"
      label-width="auto"
      require-mark-placement="right"
    >
      <n-form-item path="type" :label="t('crm.approval.addSignMethod')">
        <n-radio-group v-model:value="addSignForm.type" name="radiogroup">
          <n-radio key="BEFORE" value="BEFORE">
            {{ t('crm.approval.beforeMethod') }}
          </n-radio>
          <n-radio key="AFTER" value="AFTER">
            {{ t('crm.approval.afterMethod') }}
          </n-radio>
        </n-radio-group>
      </n-form-item>
      <n-form-item
        path="reviewer"
        :label="t('crm.approval.addSignApprover')"
        :rule="[
          {
            required: true,
            message: t('common.notNull', {
              value: t('crm.approval.addSignApprover'),
            }),
          },
        ]"
      >
        <CrmMemberSelect
          v-model:value="addSignForm.reviewer"
          :multiple="false"
          :apiTypeKey="MemberApiTypeEnum.FORM_FIELD"
          :member-types="[
            {
              label: t('menu.settings.org'),
              value: MemberSelectTypeEnum.ORG,
            },
          ]"
        />
      </n-form-item>
      <n-form-item path="reason" :label="t('crm.approval.addSignOpinion')">
        <CrmFileInput v-model:value="addSignForm.reason" v-model:file-list="addSignForm.fileList" />
      </n-form-item>
    </n-form>
  </CrmModal>
  <CrmModal
    v-model:show="fallbackModalVisible"
    :title="t('taskDrawer.operation.BACK')"
    :ok-loading="fallbackLoading"
    :positive-text="t('crm.approval.confirmFallback')"
    @confirm="handleFallback"
  >
    <n-form
      ref="fallbackFormRef"
      :model="fallbackForm"
      label-placement="left"
      label-width="auto"
      require-mark-placement="right"
    >
      <n-form-item path="node" :label="t('crm.approval.fallbackTo')" required>
        <n-select v-model:value="fallbackForm.node" :options="fallbackOptions" clearable />
      </n-form-item>
      <n-form-item path="reason" :label="t('crm.approval.fallbackReason')">
        <CrmFileInput v-model:value="fallbackForm.reason" v-model:file-list="fallbackForm.fileList" />
      </n-form-item>
    </n-form>
  </CrmModal>
</template>

<script setup lang="ts">
  import {
    type FormInst,
    NButton,
    NForm,
    NFormItem,
    NRadio,
    NRadioGroup,
    NScrollbar,
    NSelect,
    type UploadFileInfo,
    useMessage,
  } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { MemberApiTypeEnum, MemberSelectTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { MultiApproverModeEnum, ProcessStatusEnum } from '@lib/shared/enums/process';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { CollaborationType } from '@lib/shared/models/customer';
  import type { FormConfig } from '@lib/shared/models/system/module';
  import type {
    ApprovalDetail,
    ApprovalFieldPermission,
    ApprovalNode,
    ApprovalProcessDetail,
  } from '@lib/shared/models/system/process';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import CrmMoreAction from '@/components/pure/crm-more-action/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmSplitPanel from '@/components/pure/crm-split-panel/index.vue';
  import CrmFileInput from '@/components/business/crm-file-input/index.vue';
  import CrmMemberSelect from '@/components/business/crm-user-tag-selector/index.vue';
  import CrmApprovalLine from './crm-approval-line.vue';

  import {
    addSignApproval,
    agreeApproval,
    backApproval,
    getApprovalConfigDetail,
    getApprovalResourceDetail,
    rejectApproval,
    revokeApproval,
    revokeResource,
  } from '@/api/modules';
  import useModal from '@/hooks/useModal';
  import useUserStore from '@/store/modules/user';

  import type { SelectMixedOption } from 'naive-ui/es/select/src/interface';

  const props = defineProps<{
    sourceId: string;
    formKey: FormDesignKeyEnum;
    layout?: 'horizontal' | 'vertical';
    approvalStatus: ProcessStatusEnum;
  }>();
  const emit = defineEmits<{
    (
      e: 'descriptionInit',
      collaborationType?: CollaborationType,
      sourceName?: string,
      detail?: Record<string, any>,
      config?: FormConfig
    ): void;
    (e: 'saveApproval', callback: () => Promise<any>, hasFieldPermission: boolean): void;
    (e: 'refresh'): void;
  }>();

  const { t } = useI18n();
  const { openModal } = useModal();
  const message = useMessage();
  const userStore = useUserStore();

  const approvalInfo = ref<ApprovalDetail>();
  const approvalConfig = ref<ApprovalProcessDetail>(); // 审批配置详情

  async function initApprovalConfig() {
    try {
      if (props.formKey) {
        approvalConfig.value = await getApprovalConfigDetail(props.formKey);
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  const approvalOpinion = ref('');
  const fileList = ref<UploadFileInfo[]>([]);
  const CrmFileInputRef = ref<InstanceType<typeof CrmFileInput>>();
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
  const filedPermission = computed(() => {
    if (currentTaskNode.value && approvalInfo.value?.currentNodeFieldPermissions) {
      return (JSON.parse(approvalInfo.value?.currentNodeFieldPermissions || '[]') as ApprovalFieldPermission[]) || [];
    }
    return [];
  });

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
    // 未配置撤销申请
    if (!approvalConfig.value?.submitterCanRevoke) {
      return false;
    }
    // 流程结束不允许撤销
    if (currentApprovalNode.value?.endNode) {
      return false;
    }
    // 只有提交人可以撤销审批申请，已撤销状态不显示撤销按钮
    if (
      approvalInfo.value?.submitterId !== userStore.userInfo.id ||
      props.approvalStatus === ProcessStatusEnum.REVOKED
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
  const moduleKeyMap: Partial<Record<FormDesignKeyEnum, string>> = {
    [FormDesignKeyEnum.CONTACT]: 'CONTRACT_INDEX',
    [FormDesignKeyEnum.INVOICE]: 'CONTRACT_INVOICE',
    [FormDesignKeyEnum.OPPORTUNITY_QUOTATION]: 'OPPORTUNITY_QUOTATION',
    [FormDesignKeyEnum.ORDER]: 'ORDER_INDEX',
  };

  const noApproval = ref(false);
  async function initApprovalDetail() {
    try {
      approvalInfo.value = await getApprovalResourceDetail(props.sourceId);
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

  const approvalLoading = ref(false);
  async function approvalAgree() {
    if (!CrmFileInputRef.value?.validate() || !currentTaskNode.value || !currentApprovalNode.value) {
      return;
    }
    try {
      approvalLoading.value = true;
      await agreeApproval({
        id: currentTaskNode.value.taskId,
        nodeId: currentApprovalNode.value.nodeId,
        instanceId: approvalConfig.value?.id || '',
        attachmentIds: fileList.value.map((e) => e.id),
        approverId: currentTaskNode.value.approverId,
        comment: approvalOpinion.value,
        module: moduleKeyMap[props.formKey]!,
      });
      message.success(t('common.approved'));
      initApprovalDetail();
      approvalOpinion.value = '';
      fileList.value = [];
      emit('refresh');
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      approvalLoading.value = false;
    }
  }
  async function handleApprove() {
    if (!CrmFileInputRef.value?.validate() || !currentTaskNode.value || !currentApprovalNode.value) {
      return;
    }
    emit(
      'saveApproval',
      approvalAgree,
      filedPermission.value.some((e) => e.permissionType === 'EDIT')
    );
  }

  function handleReject() {
    if (!CrmFileInputRef.value?.validate()) {
      return;
    }
    // 审批驳回
    openModal({
      title: t('crm.approval.rejectConfirm'),
      content: t('crm.approval.rejectTip'),
      type: 'error',
      positiveText: t('crm.approval.confirmReject'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        if (!currentApprovalNode.value || !currentTaskNode.value) {
          return;
        }
        emit(
          'saveApproval',
          async () => {
            if (!currentApprovalNode.value || !currentTaskNode.value) {
              return;
            }
            try {
              await rejectApproval({
                id: currentTaskNode.value.taskId,
                nodeId: currentApprovalNode.value.nodeId,
                instanceId: approvalConfig.value?.id || '',
                attachmentIds: fileList.value.map((e) => e.id),
                approverId: currentTaskNode.value.approverId,
                comment: approvalOpinion.value,
                module: moduleKeyMap[props.formKey]!,
              });
              message.success(t('common.rejected'));
              initApprovalDetail();
              approvalOpinion.value = '';
              fileList.value = [];
              emit('refresh');
            } catch (error) {
              // eslint-disable-next-line no-console
              console.log(error);
            }
          },
          filedPermission.value.some((e) => e.permissionType === 'EDIT')
        );
      },
    });
  }

  const addSignModalVisible = ref(false);
  const addSignLoading = ref(false);
  const addSignForm = ref({
    type: 'BEFORE',
    reviewer: undefined,
    reason: '',
    fileList: [] as UploadFileInfo[],
  });
  const addSignFormRef = ref<FormInst>();

  function handleAddSign() {
    addSignFormRef.value?.validate(async (errors) => {
      if (!errors && currentApprovalNode.value && currentTaskNode.value) {
        emit(
          'saveApproval',
          async () => {
            if (!errors && currentApprovalNode.value && currentTaskNode.value) {
              try {
                addSignLoading.value = true;
                await addSignApproval({
                  id: currentTaskNode.value.taskId,
                  nodeId: currentApprovalNode.value.nodeId,
                  instanceId: approvalInfo.value?.id || '',
                  approverId: currentTaskNode.value?.approverId || '',
                  comment: addSignForm.value.reason,
                  attachmentIds: addSignForm.value.fileList.map((e) => e.id),
                  type: addSignForm.value.type,
                  module: moduleKeyMap[props.formKey]!,
                  signApprover: addSignForm.value.reviewer?.[0] || '',
                });
                addSignModalVisible.value = false;
                message.success(t('crm.approval.addSignSuccess'));
                addSignForm.value = {
                  type: 'BEFORE',
                  reviewer: undefined,
                  reason: '',
                  fileList: [],
                };
                initApprovalDetail();
                emit('refresh');
              } catch (error) {
                // eslint-disable-next-line no-console
                console.log(error);
              } finally {
                addSignLoading.value = false;
              }
            }
          },
          filedPermission.value.some((e) => e.permissionType === 'EDIT')
        );
      }
    });
  }

  const fallbackModalVisible = ref(false);
  const fallbackLoading = ref(false);
  const fallbackForm = ref({
    node: undefined,
    reason: '',
    fileList: [] as UploadFileInfo[],
  });
  const fallbackOptions = computed(() => {
    const options: SelectMixedOption[] = [];
    for (let i = currentApprovalNodeIndex.value - 1; i >= 0; i--) {
      options.push({
        label: t('crm.approval.preNode', { index: currentApprovalNodeIndex.value - i }),
        value: approvalInfo.value?.nodes[i].nodeId || '',
      });
    }
    return options;
  });
  const fallbackFormRef = ref<FormInst>();

  function handleFallback() {
    fallbackFormRef.value?.validate(async (errors) => {
      if (!errors && currentApprovalNode.value && currentTaskNode.value) {
        emit(
          'saveApproval',
          async () => {
            if (!errors && currentApprovalNode.value && currentTaskNode.value) {
              try {
                fallbackLoading.value = true;
                await backApproval({
                  id: currentTaskNode.value.taskId,
                  nodeId: currentApprovalNode.value.nodeId,
                  instanceId: approvalInfo.value?.id || '',
                  approverId: currentTaskNode.value?.approverId || '',
                  comment: fallbackForm.value.reason,
                  attachmentIds: fallbackForm.value.fileList.map((e) => e.id),
                  module: moduleKeyMap[props.formKey]!,
                  returnToNodeId: fallbackForm.value.node || '',
                });
                fallbackModalVisible.value = false;
                message.success(t('crm.approval.fallbackSuccess'));
                initApprovalDetail();
                fallbackForm.value = {
                  node: undefined,
                  reason: '',
                  fileList: [],
                };
                emit('refresh');
              } catch (error) {
                // eslint-disable-next-line no-console
                console.log(error);
              } finally {
                fallbackLoading.value = false;
              }
            }
          },
          filedPermission.value.some((e) => e.permissionType === 'EDIT')
        );
      }
    });
  }

  const moreActions = computed(() => {
    const fullActions: ActionsItem[] = [];
    if (approvalConfig.value?.allowAddSign) {
      fullActions.push({
        key: 'addSign',
        label: t('common.COUNTERSIGNATURE'),
      });
    }
    if (fallbackOptions.value.length) {
      fullActions.push({
        key: 'fallback',
        label: t('taskDrawer.operation.BACK'),
      });
    }
    return fullActions;
  });

  function handleMoreActionSelect(item: ActionsItem) {
    if (item.key === 'addSign') {
      // 添加会签
      addSignModalVisible.value = true;
    } else if (item.key === 'fallback') {
      // 退回
      fallbackModalVisible.value = true;
    }
  }

  function cancelApproval(type?: 'apply' | 'approval') {
    if (type === 'apply') {
      openModal({
        title: t('crm.approval.cancelApprovalApplyConfirm'),
        content: t('crm.approval.cancelApprovalApplyTip'),
        type: 'error',
        positiveText: t('crm.approval.confirmCancelApprovalApply'),
        negativeText: t('common.cancel'),
        onPositiveClick: async () => {
          emit(
            'saveApproval',
            async () => {
              await revokeResource({
                resourceId: props.sourceId,
                formKey: props.formKey,
              });
              message.success(t('common.revokeSuccess'));
              initApprovalDetail();
              emit('refresh');
            },
            filedPermission.value.some((e) => e.permissionType === 'EDIT')
          );
        },
      });
    } else {
      openModal({
        title: t('crm.approval.cancelApprovalConfirm'),
        content: t('crm.approval.cancelApprovalTip'),
        type: 'error',
        positiveText: t('crm.approval.confirmCancelApproval'),
        negativeText: t('common.cancel'),
        onPositiveClick: async () => {
          emit(
            'saveApproval',
            async () => {
              await revokeApproval({
                id: prevMineApprovalNode.value?.taskId || '',
              });
              message.success(t('crm.approval.cancelApprovalSuccess'));
              initApprovalDetail();
              emit('refresh');
            },
            filedPermission.value.some((e) => e.permissionType === 'EDIT')
          );
        },
      });
    }
  }

  watch(
    () => props.approvalStatus,
    () => {
      if (props.approvalStatus && ![ProcessStatusEnum.PENDING, ProcessStatusEnum.NONE].includes(props.approvalStatus)) {
        initApprovalDetail();
        initApprovalConfig();
      }
    },
    {
      immediate: true,
    }
  );
</script>

<style lang="less" scoped></style>
