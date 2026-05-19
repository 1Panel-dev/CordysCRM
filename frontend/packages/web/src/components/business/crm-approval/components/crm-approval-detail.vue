<template>
  <CrmCard hide-footer no-content-padding>
    <CrmSplitPanel :size="0.7" :max="1" :min="0.7" :default-size="0.7" collapse-side="right" disabled>
      <template #1>
        <div class="flex h-full w-full p-[24px_16px_24px_24px]">
          <n-scrollbar x-scrollable>
            <slot name="left"></slot>
          </n-scrollbar>
        </div>
      </template>
      <template #2>
        <div class="flex h-full w-full flex-col overflow-hidden border-l border-[var(--text-n8)]">
          <div class="flex-1 overflow-hidden px-[16px] py-[24px]">
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
              class="h-[calc(100%-26px)] pr-[8px]"
            />
          </div>
          <div class="border-t border-[var(--text-n8)] p-[16px]">
            <template v-if="isApprover">
              <div class="mb-[8px] font-semibold">{{ t('crm.approval.opinion') }}</div>
              <CrmFileInput
                ref="CrmFileInputRef"
                v-model:value="approvalOpinion"
                v-model:file-list="fileList"
                :required="approvalConfig?.requireComment"
              />
              <div class="mt-[12px] flex gap-[12px]">
                <n-button type="primary" class="flex-1" @click="handleApprove">{{ t('common.approve') }}</n-button>
                <n-button type="error" ghost @click="handleReject">{{ t('common.reject') }}</n-button>
                <CrmMoreAction :options="moreActions" trigger="click" size="medium" @select="handleMoreActionSelect" />
              </div>
            </template>
            <n-button v-if="approvalConfig?.allowWithdraw" type="primary" ghost block @click="cancelApproval">
              <template #icon>
                <CrmIcon type="iconicon_rollfront" :size="16" />
              </template>
              {{ isApprover ? t('crm.approval.cancelApproval') : t('crm.approval.cancelApprovalApply') }}
            </n-button>
          </div>
        </div>
      </template>
    </CrmSplitPanel>
  </CrmCard>
  <CrmModal
    v-model:show="addSignModalVisible"
    :title="t('common.COUNTERSIGNATURE')"
    :ok-loading="addSignLoading"
    :positive-text="addSignForm.type === 'after' ? t('crm.approval.agreeAndAddSign') : t('crm.approval.confirmAddSign')"
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
          <n-radio key="before" value="before">
            {{ t('crm.approval.beforeMethod') }}
          </n-radio>
          <n-radio key="after" value="after">
            {{ t('crm.approval.afterMethod') }}
          </n-radio>
        </n-radio-group>
      </n-form-item>
      <n-form-item path="reviewer" :label="t('crm.approval.addSignApprover')" required>
        <CrmMemberSelect v-model:value="addSignForm.reviewer" :multiple="false" />
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
        <CrmFileInput v-model:value="fallbackForm.reason" v-model:file-list="addSignForm.fileList" />
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
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { CollaborationType } from '@lib/shared/models/customer';
  import type { FormConfig } from '@lib/shared/models/system/module';
  import type { ApprovalDetail, ApprovalNode, ApprovalProcessDetail } from '@lib/shared/models/system/process';

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
    approvalProcessDetail,
    getApprovalResourceDetail,
    rejectApproval,
  } from '@/api/modules';
  import useModal from '@/hooks/useModal';

  import type { SelectMixedOption } from 'naive-ui/es/select/src/interface';

  const props = defineProps<{
    sourceId: string;
    formKey: FormDesignKeyEnum;
    layout?: 'horizontal' | 'vertical';
    approvalFlowId?: string; // 审批流 id
  }>();
  const emit = defineEmits<{
    (
      e: 'descriptionInit',
      collaborationType?: CollaborationType,
      sourceName?: string,
      detail?: Record<string, any>,
      config?: FormConfig
    ): void;
  }>();

  const { t } = useI18n();
  const { openModal } = useModal();
  const message = useMessage();

  const isApprover = ref(true); // 是否是审批人，测试数据，后续根据接口返回设置
  const approvalOpinion = ref('');
  const fileList = ref<UploadFileInfo[]>([]);
  const CrmFileInputRef = ref<InstanceType<typeof CrmFileInput>>();
  const approvalInfo = ref<ApprovalDetail>();
  const currentApprovalNode = ref<ApprovalNode>();
  const currentApprovalNodeIndex = ref(0);
  const moduleKeyMap: Partial<Record<FormDesignKeyEnum, string>> = {
    [FormDesignKeyEnum.CONTACT]: 'CONTRACT_INDEX',
    [FormDesignKeyEnum.INVOICE]: 'CONTRACT_INVOICE',
    [FormDesignKeyEnum.OPPORTUNITY_QUOTATION]: 'OPPORTUNITY_QUOTATION',
    [FormDesignKeyEnum.ORDER]: 'ORDER_INDEX',
  };

  async function initApprovalDetail() {
    try {
      approvalInfo.value = await getApprovalResourceDetail(props.sourceId);
      currentApprovalNodeIndex.value = approvalInfo.value.nodes.findIndex(
        (node) => node.nodeId === approvalInfo.value?.currentNodeId
      );
      currentApprovalNode.value = approvalInfo.value.nodes[currentApprovalNodeIndex.value];
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  async function handleApprove() {
    if (!CrmFileInputRef.value?.validate() || !currentApprovalNode.value) {
      return;
    }
    try {
      await agreeApproval({
        id: currentApprovalNode.value.taskId,
        nodeId: currentApprovalNode.value.nodeId,
        instanceId: currentApprovalNode.value.recordId,
        attachmentIds: fileList.value.map((e) => e.id),
        approverId: currentApprovalNode.value.approverId,
        comment: approvalOpinion.value,
        module: moduleKeyMap[props.formKey]!,
      });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
    message.success(t('common.approved'));
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
      onPositiveClick: async () => {
        if (!currentApprovalNode.value) {
          return;
        }
        try {
          await rejectApproval({
            id: currentApprovalNode.value.taskId,
            nodeId: currentApprovalNode.value.nodeId,
            instanceId: currentApprovalNode.value.recordId,
            attachmentIds: fileList.value.map((e) => e.id),
            approverId: currentApprovalNode.value.approverId,
            comment: approvalOpinion.value,
            module: moduleKeyMap[props.formKey]!,
          });
          message.success(t('common.rejected'));
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
    });
  }

  const approvalConfig = ref<ApprovalProcessDetail>(); // 审批配置详情
  async function initApprovalConfig() {
    try {
      if (props.approvalFlowId) {
        approvalConfig.value = await approvalProcessDetail(props.approvalFlowId);
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  const addSignModalVisible = ref(false);
  const addSignLoading = ref(false);
  const addSignForm = ref({
    type: 'before',
    reviewer: undefined,
    reason: '',
    fileList: [] as UploadFileInfo[],
  });
  const addSignFormRef = ref<FormInst>();

  function handleAddSign() {
    addSignFormRef.value?.validate(async (errors) => {
      if (!errors && currentApprovalNode.value) {
        try {
          addSignLoading.value = true;
          await addSignApproval({
            id: props.sourceId,
            nodeId: currentApprovalNode.value.nodeId,
            instanceId: currentApprovalNode.value.recordId,
            approverId: currentApprovalNode.value.approverId,
            comment: addSignForm.value.reason,
            attachmentIds: addSignForm.value.fileList.map((e) => e.id),
            type: addSignForm.value.type,
            module: moduleKeyMap[props.formKey]!,
          });
          addSignModalVisible.value = false;
          message.success(t('crm.approval.addSignSuccess'));
          addSignForm.value = {
            type: 'before',
            reviewer: undefined,
            reason: '',
            fileList: [],
          };
          initApprovalDetail();
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        } finally {
          addSignLoading.value = false;
        }
      }
    });
  }

  const fallbackModalVisible = ref(false);
  const fallbackLoading = ref(false);
  const fallbackForm = ref({
    node: undefined,
    reason: '',
  });
  const fallbackOptions = computed(() => {
    const options: SelectMixedOption[] = [];
    for (let i = 0; i < currentApprovalNodeIndex.value; i++) {
      options.push({
        label: t('crm.approval.preNode', { index: i + 1 }),
        value: approvalInfo.value?.nodes[i].nodeId || '',
      });
    }
    return options;
  });
  const fallbackFormRef = ref<FormInst>();

  function handleFallback() {
    fallbackFormRef.value?.validate((errors) => {
      if (!errors) {
        fallbackLoading.value = true;
        setTimeout(() => {
          fallbackLoading.value = false;
          fallbackModalVisible.value = false;
          message.success(t('crm.approval.fallbackSuccess'));
          fallbackForm.value = {
            node: undefined,
            reason: '',
          };
        }, 1000);
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

  function cancelApproval() {
    if (isApprover.value) {
      openModal({
        title: t('crm.approval.cancelApprovalConfirm'),
        content: t('crm.approval.cancelApprovalTip'),
        type: 'error',
        positiveText: t('crm.approval.confirmCancelApproval'),
        onPositiveClick: async () => {
          message.success(t('crm.approval.cancelApprovalSuccess'));
        },
      });
    } else {
      openModal({
        title: t('crm.approval.cancelApprovalApplyConfirm'),
        content: t('crm.approval.cancelApprovalApplyTip'),
        type: 'error',
        positiveText: t('crm.approval.confirmCancelApprovalApply'),
        onPositiveClick: async () => {
          message.success(t('crm.approval.cancelApprovalSuccess'));
        },
      });
    }
  }

  onBeforeMount(() => {
    initApprovalDetail();
    initApprovalConfig();
  });
</script>

<style lang="less" scoped></style>
