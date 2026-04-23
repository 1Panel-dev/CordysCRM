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
            <div class="crm-modal-title one-line-text !text-[var(--text-n4)]"> ({{ props.approvalItem?.name }}) </div>
          </template>
          {{ props.approvalItem?.name }}
        </n-tooltip>
      </div>
    </template>
    <n-form ref="approvalFormRef" :model="approvalForm" :inline="false">
      <n-form-item
        :label="props.approvalType === 'approve' ? t('taskDrawer.approveReason') : t('taskDrawer.rejectReason')"
        path="reason"
        :rule="[
          {
            required: props.approvalType === 'reject',
            message: t('common.notNull', { value: t('taskDrawer.rejectReason') }),
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

  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import type { CrmFileItem } from '@/components/pure/crm-upload/types';
  import CrmFileInput from '@/components/business/crm-file-input/index.vue';

  const props = defineProps<{
    approvalItem?: any;
    approvalItemKeys?: string[];
    approvalType: 'approve' | 'reject';
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

  function handleApprovalCancel() {
    show.value = false;
    approvalForm.value = {
      reason: '',
    };
    fileList.value = [];
    emit('approvalCancel');
  }

  function handleApprovalSave() {
    approvalFormRef.value?.validate(async (errors) => {
      if (!errors) {
        try {
          message.success(props.approvalType === 'approve' ? t('taskDrawer.approved') : t('taskDrawer.rejected'));
          handleApprovalCancel();
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      }
    });
  }
</script>

<style lang="less" scoped></style>
