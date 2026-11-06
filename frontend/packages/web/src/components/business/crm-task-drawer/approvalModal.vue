<template>
  <ApprovalOpinionModal
    v-model:show="show"
    v-model:opinion="approvalForm.reason"
    v-model:file-list="fileList"
    v-model:resubmit-mode="resubmitMode"
    v-model:resubmit-start-node-number="resubmitStartNodeNumber"
    :approval-type="props.approvalType"
    :loading="loading"
    :required="isCommentRequired"
    :show-resubmit-path="!props.approvalItemKeys?.length"
    :current-node-name="props.approvalItem?.approvalNodeName"
    :resubmit-node-options="resubmitNodeOptions"
    @confirm="handleApprovalSave"
    @cancel="handleApprovalCancel"
  >
    <template #title>
      <div class="flex items-center gap-[8px] overflow-hidden">
        <div class="whitespace-nowrap font-medium">
          {{ t('common.approval') }}
        </div>
        <div
          v-if="props.approvalItemKeys && props.approvalItemKeys.length > 0 && !props.approvalItem"
          class="text-[var(--text-n4)]"
        >
          {{ t('taskDrawer.items', { count: props.approvalItemKeys.length }) }}
        </div>
        <n-tooltip v-else flip :delay="300" trigger="hover">
          <template #trigger>
            <div class="crm-modal-title one-line-text w-full !text-[var(--text-n4)]">
              ({{ props.approvalItem?.resourceName }})
            </div>
          </template>
          {{ props.approvalItem?.resourceName }}
        </n-tooltip>
      </div>
    </template>
  </ApprovalOpinionModal>
</template>

<script setup lang="ts">
  import { NTooltip, type UploadFileInfo, useMessage } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { ApprovalProcessDetail, ApprovalTodoItem } from '@lib/shared/models/system/process';

  import ApprovalOpinionModal from '@/components/business/crm-approval/components/approvalOpinionModal.vue';

  import {
    agreeApproval,
    batchAgreeApproval,
    batchRejectApproval,
    getApprovalConfigDetail,
    getApprovalResubmitNodes,
    rejectApproval,
  } from '@/api/modules/index';

  const props = defineProps<{
    approvalItem?: ApprovalTodoItem;
    approvalItemKeys?: string[];
    approvalType: 'approve' | 'reject';
    resourceType: string;
  }>();
  const emit = defineEmits<{
    (e: 'approvalSuccess'): void;
    (e: 'approvalCancel'): void;
  }>();

  const message = useMessage();
  const { t } = useI18n();

  const show = defineModel<boolean>('show', {
    required: true,
    default: false,
  });

  const loading = ref(false);
  const approvalForm = ref({
    reason: '',
  });
  const fileList = ref<UploadFileInfo[]>([]);
  const resubmitMode = ref<'SUBMITTER' | 'CURRENT_NODE' | 'SPECIFIED_NODE'>('SUBMITTER');
  const resubmitStartNodeNumber = ref<string | null>(null);
  const resubmitNodeOptions = ref<Array<{ label: string; value: string }>>([]);

  function reset() {
    approvalForm.value = {
      reason: '',
    };
    fileList.value = [];
    resubmitMode.value = 'SUBMITTER';
    resubmitStartNodeNumber.value = null;
    resubmitNodeOptions.value = [];
    show.value = false;
  }

  function handleApprovalCancel() {
    reset();
    emit('approvalCancel');
  }

  function getResubmitStartNodeNumber() {
    switch (resubmitMode.value) {
      case 'CURRENT_NODE':
        return props.approvalItem?.approvalNodeNumber || undefined;
      case 'SPECIFIED_NODE':
        return resubmitStartNodeNumber.value ?? undefined;
      default:
        return undefined;
    }
  }

  async function approval() {
    if (!props.approvalItem) {
      return;
    }
    try {
      loading.value = true;
      if (props.approvalType === 'reject') {
        await rejectApproval({
          id: props.approvalItem.approvalTaskId,
          nodeId: props.approvalItem.approvalNodeId,
          instanceId: props.approvalItem.approvalInstanceId,
          attachmentIds: fileList.value.map((e) => e.id),
          approverId: props.approvalItem.approvalId,
          comment: approvalForm.value.reason,
          resubmitMode: resubmitMode.value,
          resubmitStartNodeNumber: getResubmitStartNodeNumber(),
        });
      } else {
        await agreeApproval({
          id: props.approvalItem.approvalTaskId,
          nodeId: props.approvalItem.approvalNodeId,
          instanceId: props.approvalItem.approvalInstanceId,
          attachmentIds: fileList.value.map((e) => e.id),
          approverId: props.approvalItem.approvalId,
          comment: approvalForm.value.reason,
        });
      }
      message.success(props.approvalType === 'approve' ? t('taskDrawer.approved') : t('taskDrawer.rejected'));
      emit('approvalSuccess');
      reset();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  async function batchApproval() {
    if (!props.approvalItemKeys) {
      return;
    }
    try {
      loading.value = true;
      if (props.approvalType === 'reject') {
        await batchRejectApproval({
          ids: props.approvalItemKeys,
          comment: approvalForm.value.reason,
          attachmentIds: fileList.value.map((e) => e.id),
        });
      } else {
        await batchAgreeApproval({
          ids: props.approvalItemKeys,
          comment: approvalForm.value.reason,
          attachmentIds: fileList.value.map((e) => e.id),
        });
      }
      message.success(props.approvalType === 'approve' ? t('taskDrawer.approved') : t('taskDrawer.rejected'));
      emit('approvalSuccess');
      reset();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  function handleApprovalSave() {
    if (props.approvalItemKeys?.length) {
      batchApproval();
    } else {
      approval();
    }
  }

  const approvalConfig = ref<ApprovalProcessDetail>(); // 审批配置详情
  const isCommentRequired = computed(() =>
    props.approvalType === 'reject'
      ? approvalConfig.value?.requireRejectComment
      : approvalConfig.value?.requireApproveComment
  );
  async function initApprovalConfig() {
    try {
      approvalConfig.value = await getApprovalConfigDetail(props.resourceType);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  async function loadResubmitNodeOptions(taskId: string) {
    try {
      const nodes = await getApprovalResubmitNodes(taskId);
      resubmitNodeOptions.value = nodes.map((node, index) => {
        let tag: 'current' | 'submitter' | undefined;
        if (node.nodeNumber && node.nodeNumber === props.approvalItem?.approvalNodeNumber) {
          tag = 'current';
        } else if (index === 0 && !node.nodeNumber) {
          tag = 'submitter';
        }
        return { label: node.nodeName, value: node.nodeNumber, tag };
      });
    } catch (error) {
      resubmitNodeOptions.value = [];
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  watch(
    () => show.value,
    (val) => {
      if (!val) {
        approvalForm.value = {
          reason: '',
        };
        fileList.value = [];
        resubmitMode.value = 'SUBMITTER';
        resubmitStartNodeNumber.value = null;
        resubmitNodeOptions.value = [];
      } else {
        initApprovalConfig();
        if (props.approvalType === 'reject' && !props.approvalItemKeys?.length && props.approvalItem?.approvalTaskId) {
          loadResubmitNodeOptions(props.approvalItem.approvalTaskId);
        }
      }
    },
    {
      immediate: true,
    }
  );
</script>

<style lang="less" scoped></style>
