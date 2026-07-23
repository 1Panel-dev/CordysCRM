<template>
  <van-popup
    v-model:show="showApprovalPopup"
    position="bottom"
    :style="{ height: '100vh' }"
    safe-area-inset-top
    safe-area-inset-bottom
  >
    <div class="relative p-[16px] text-center">
      <CrmTextButton
        icon="iconicon_chevron_left"
        icon-size="24px"
        color="var(--text-n1)"
        class="absolute left-[12px] top-[16px]"
        @click="showApprovalPopup = false"
      />
      <div class="text-[16px] font-semibold">{{ t('workbench.approval') }}</div>
    </div>
    <van-form ref="approvalFormRef" label-align="top" @submit="">
      <van-field
        name="reason"
        :label="isRejecting ? t('workbench.rejectReason') : t('workbench.approvedReason')"
        :placeholder="t('common.pleaseInput')"
        v-model="approvalForm.reason"
        autosize
        show-word-limit
        rows="2"
        type="textarea"
        maxlength="300"
        :required="approvalConfig?.requireComment"
        :rules="[
          {
            required: approvalConfig?.requireComment,
            message: t('common.notNull', {
              value: isRejecting ? t('workbench.rejectReason') : t('workbench.approvedReason'),
            }),
          },
        ]"
      />
      <CrmUploadFile
        v-model:value="approvalForm.fileList"
        :field-config="{
          id: getGenerateId(),
          name: t('crm.fileListPop.title'),
          type: FieldTypeEnum.ATTACHMENT,
          showLabel: true,
          readable: true,
          editable: true,
          fieldWidth: 1,
          description: '',
          icon: '',
          rules: [],
          limitSize: '50',
        }"
      />
    </van-form>
    <div class="fixed bottom-0 left-0 right-0 flex items-center justify-between gap-[16px] p-[16px]">
      <van-button
        type="default"
        class="flex-1 !border-none !bg-[var(--text-n8)] !text-[var(--text-n1)]"
        :disabled="loading"
        @click="showApprovalPopup = false"
      >
        {{ t('common.cancel') }}
      </van-button>
      <van-button
        :type="isRejecting ? 'danger' : 'primary'"
        class="flex-1"
        :loading="loading"
        :disabled="approvalConfig?.requireComment && !approvalForm.reason"
        @click="handleApprovalSave"
      >
        {{ isRejecting ? t('workbench.operation.confirmRejected') : t('workbench.operation.confirmApproved') }}
      </van-button>
    </div>
  </van-popup>
</template>
<script setup lang="ts">
  import CrmUploadFile from '@/components/business/crm-form-create/components/advanced/file.vue';
  import { getGenerateId } from '@lib/shared/method';
  import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import {
    agreeApproval,
    batchAgreeApproval,
    batchRejectApproval,
    getApprovalConfigDetail,
    rejectApproval,
  } from '@/api/modules';
  import { showToast, type FormInstance } from 'vant';
  import type { ApprovalProcessDetail, ApprovalTodoItem } from '@lib/shared/models/system/process';

  const props = defineProps<{
    approvingItem?: Partial<ApprovalTodoItem>;
    approvalItemKeys?: string[];
    isRejecting: boolean;
    selectedKeys?: string[];
    resourceType: string;
  }>();
  const emit = defineEmits<{
    (e: 'refresh'): void;
  }>();

  const { t } = useI18n();

  const showApprovalPopup = defineModel<boolean>('show', {
    default: false,
  });
  const approvalForm = ref({
    reason: '',
    fileList: [] as string[],
  });
  const loading = ref(false);
  const approvalFormRef = ref<FormInstance>();

  async function approval() {
    if (!props.approvingItem) {
      return;
    }
    try {
      loading.value = true;
      if (props.isRejecting) {
        await rejectApproval({
          id: props.approvingItem?.approvalTaskId || '',
          nodeId: props.approvingItem?.approvalNodeId || '',
          instanceId: props.approvingItem?.approvalInstanceId || '',
          attachmentIds: approvalForm.value.fileList,
          approverId: props.approvingItem?.approvalId!,
          comment: approvalForm.value.reason,
          module: 'WORKBENCH',
        });
      } else {
        await agreeApproval({
          id: props.approvingItem?.approvalTaskId || '',
          nodeId: props.approvingItem?.approvalNodeId || '',
          instanceId: props.approvingItem?.approvalInstanceId || '',
          attachmentIds: approvalForm.value.fileList,
          approverId: props.approvingItem?.approvalId!,
          comment: approvalForm.value.reason,
          module: 'WORKBENCH',
        });
      }
      showToast({
        type: 'success',
        message: props.isRejecting ? t('workbench.result.UNAPPROVED') : t('workbench.result.APPROVED'),
      });
      setTimeout(() => {
        showApprovalPopup.value = false;
        emit('refresh');
      }, 2000);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  async function batchApproval() {
    try {
      loading.value = true;
      if (props.isRejecting) {
        await batchRejectApproval({
          ids: props.selectedKeys || [],
          comment: approvalForm.value.reason,
          attachmentIds: approvalForm.value.fileList,
        });
      } else {
        await batchAgreeApproval({
          ids: props.selectedKeys || [],
          comment: approvalForm.value.reason,
          attachmentIds: approvalForm.value.fileList,
        });
      }
      showToast({
        type: 'success',
        message: props.isRejecting ? t('workbench.result.UNAPPROVED') : t('workbench.result.APPROVED'),
      });
      setTimeout(() => {
        showApprovalPopup.value = false;
        emit('refresh');
      }, 2000);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  function handleApprovalSave() {
    if (approvalFormRef.value?.validate()) {
      if (props.selectedKeys?.length) {
        batchApproval();
      } else {
        approval();
      }
    }
  }

  const approvalConfig = ref<ApprovalProcessDetail>(); // 审批配置详情
  async function initApprovalConfig() {
    try {
      approvalConfig.value = await getApprovalConfigDetail(props.resourceType);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  watch(
    () => showApprovalPopup.value,
    (val) => {
      if (val) {
        initApprovalConfig();
      } else {
        approvalForm.value.reason = '';
        approvalForm.value.fileList = [];
      }
    }
  );
</script>
<style lang="less" scoped></style>
