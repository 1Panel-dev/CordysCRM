<template>
  <CrmModal
    v-model:show="show"
    :positive-text="props.approvalType === 'approve' ? t('taskDrawer.confirmApprove') : t('taskDrawer.confirmReject')"
    :negative-text="t('common.cancel')"
    :ok-loading="loading"
    @confirm="handleApprovalSave"
    @cancel="handleApprovalCancel"
  >
    <template #title>
      <div class="flex items-center gap-[8px]">
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
            <div class="crm-modal-title one-line-text !text-[var(--text-n4)]">
              ({{ props.approvalItem?.resourceName }})
            </div>
          </template>
          {{ props.approvalItem?.resourceName }}
        </n-tooltip>
      </div>
    </template>
    <n-form ref="approvalFormRef" :model="approvalForm" :inline="false">
      <n-form-item
        :label="props.approvalType === 'approve' ? t('taskDrawer.approveReason') : t('taskDrawer.rejectReason')"
        path="reason"
        :rule="[
          {
            required: approvalConfig?.requireComment,
            message: t('common.notNull', {
              value: props.approvalType === 'approve' ? t('taskDrawer.approveReason') : t('taskDrawer.rejectReason'),
            }),
          },
        ]"
      >
        <CrmFileInput v-model:value="approvalForm.reason" v-model:file-list="fileList" />
      </n-form-item>
    </n-form>
  </CrmModal>
</template>

<script setup lang="ts">
  import { type FormInst, NForm, NFormItem, NTooltip, useMessage } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { ApprovalProcessDetail, ApprovalTodoItem } from '@lib/shared/models/system/process';

  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import type { CrmFileItem } from '@/components/pure/crm-upload/types';
  import CrmFileInput from '@/components/business/crm-file-input/index.vue';

  import {
    agreeApproval,
    approvalProcessDetail,
    batchAgreeApproval,
    batchRejectApproval,
    rejectApproval,
  } from '@/api/modules/index';

  const props = defineProps<{
    approvalItem?: ApprovalTodoItem;
    approvalItemKeys?: string[];
    approvalType: 'approve' | 'reject';
    module: 'WORKBENCH' | 'CONTRACT_INDEX' | 'ORDER_INDEX' | 'OPPORTUNITY_QUOTATION' | 'CONTRACT_INVOICE';
    approvalFlowId: string; // 审批流 id
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
  const approvalFormRef = ref<FormInst>();
  const fileList = ref<CrmFileItem[]>([]);

  function reset() {
    approvalForm.value = {
      reason: '',
    };
    fileList.value = [];
    show.value = false;
  }

  function handleApprovalCancel() {
    reset();
    emit('approvalCancel');
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
          module: props.module,
        });
      } else {
        await agreeApproval({
          id: props.approvalItem.approvalTaskId,
          nodeId: props.approvalItem.approvalNodeId,
          instanceId: props.approvalItem.approvalInstanceId,
          attachmentIds: fileList.value.map((e) => e.id),
          approverId: props.approvalItem.approvalId,
          comment: approvalForm.value.reason,
          module: props.module,
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
    approvalFormRef.value?.validate(async (errors) => {
      if (!errors) {
        if (props.approvalItemKeys?.length) {
          batchApproval();
        } else {
          approval();
        }
      }
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

  watch(
    () => show.value,
    (val) => {
      if (!val) {
        approvalForm.value = {
          reason: '',
        };
        fileList.value = [];
      } else {
        initApprovalConfig();
      }
    },
    {
      immediate: true,
    }
  );
</script>

<style lang="less" scoped></style>
