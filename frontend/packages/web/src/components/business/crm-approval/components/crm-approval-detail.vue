<template>
  <CrmCard auto-height hide-footer no-content-padding>
    <CrmSplitPanel :max="1" :min="0.7" :default-size="0.7" collapse-side="right" disabled>
      <template #1>
        <div class="flex h-full w-full p-[16px]">
          <CrmFormDescription
            :form-key="props.formKey"
            :source-id="props.sourceId"
            :refresh-key="refreshKey"
            class="p-[16px_24px]"
            :column="props.layout === 'vertical' ? 3 : undefined"
            :label-width="props.layout === 'vertical' ? 'auto' : undefined"
            :value-align="props.layout === 'vertical' ? 'start' : undefined"
            @init="handleDescriptionInit"
          />
        </div>
      </template>
      <template #2>
        <div class="flex h-full w-full overflow-hidden p-[16px]">
          <div class="flex-1 overflow-hidden p-[24px]">
            <div class="mb-[8px] font-semibold">{{ t('crm.approval.record') }}</div>
            <CrmApprovalLine />
          </div>
          <div class="border-t border-[var(--text-n8)] p-[16px]">
            <div class="mb-[8px] font-semibold">{{ t('crm.approval.opinion') }}</div>
            <CrmFileInput ref="CrmFileInputRef" v-model:value="approvalOpinion" v-model:file-list="fileList" required />
            <div class="mt-[12px] flex gap-[12px]">
              <n-button type="primary" class="w-[60%]" @click="handleApprove">{{ t('common.approve') }}</n-button>
              <n-button type="error" ghost @click="handleReject">{{ t('common.reject') }}</n-button>
              <CrmMoreAction :options="moreActions" trigger="click" @select="handleMoreActionSelect" />
            </div>
          </div>
        </div>
      </template>
    </CrmSplitPanel>
  </CrmCard>
  <CrmModal
    v-model:show="addSignModalVisible"
    :title="t('common.COUNTERSIGNATURE')"
    :ok-loading="addSignLoading"
    :positive-text="t('crm.approval.confirmAddSign')"
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
    :title="t('common.FALLBACK')"
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
  import { type FormInst, NButton, NForm, NFormItem, NRadio, NRadioGroup, NSelect, useMessage } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { CollaborationType } from '@lib/shared/models/customer';
  import type { FormConfig } from '@lib/shared/models/system/module';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import CrmMoreAction from '@/components/pure/crm-more-action/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmSplitPanel from '@/components/pure/crm-split-panel/index.vue';
  import CrmFileInput from '@/components/business/crm-file-input/index.vue';
  import CrmFormDescription from '@/components/business/crm-form-description/index.vue';
  import CrmMemberSelect from '@/components/business/crm-user-tag-selector/index.vue';
  import CrmApprovalLine from './crm-approval-line.vue';

  import useModal from '@/hooks/useModal';

  const props = defineProps<{
    sourceId: string;
    formKey: FormDesignKeyEnum;
    layout?: 'horizontal' | 'vertical';
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

  const refreshKey = ref(0);

  function handleDescriptionInit(
    _collaborationType?: CollaborationType,
    _sourceName?: string,
    detail?: Record<string, any>,
    config?: FormConfig
  ) {
    emit('descriptionInit', _collaborationType, _sourceName, detail, config);
  }

  const approvalOpinion = ref('');
  const fileList = ref([]);
  const CrmFileInputRef = ref<InstanceType<typeof CrmFileInput>>();

  function handleApprove() {
    // 审批通过
    message.success(t('common.approved'));
  }

  function handleReject() {
    // 审批驳回
    openModal({
      title: t('crm.approval.rejectConfirm'),
      content: t('crm.approval.rejectTip'),
      type: 'error',
      positiveText: t('crm.approval.confirmReject'),
      onPositiveClick: async () => {
        message.success(t('common.rejected'));
      },
    });
  }

  const moreActions: ActionsItem[] = [
    {
      key: 'addSign',
      label: t('common.COUNTERSIGNATURE'),
    },
    {
      key: 'fallback',
      label: t('taskDrawer.result.FALLBACK'),
    },
  ];

  const addSignModalVisible = ref(false);
  const addSignLoading = ref(false);
  const addSignForm = ref({
    type: 'before',
    reviewer: undefined,
    reason: '',
    fileList: [],
  });
  const addSignFormRef = ref<FormInst>();

  function handleAddSign() {
    addSignFormRef.value?.validate((errors) => {
      if (!errors) {
        addSignLoading.value = true;
        setTimeout(() => {
          addSignLoading.value = false;
          addSignModalVisible.value = false;
          message.success(t('crm.approval.addSignSuccess'));
          addSignForm.value = {
            type: 'before',
            reviewer: undefined,
            reason: '',
            fileList: [],
          };
        }, 1000);
      }
    });
  }

  const fallbackModalVisible = ref(false);
  const fallbackLoading = ref(false);
  const fallbackForm = ref({
    node: undefined,
    reason: '',
  });
  const fallbackOptions = ref([
    {
      label: '审批节点1',
      value: 'node1',
    },
    {
      label: '审批节点2',
      value: 'node2',
    },
  ]);
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

  function handleMoreActionSelect(item: ActionsItem) {
    if (item.key === 'addSign') {
      // 添加会签
      addSignModalVisible.value = true;
    } else if (item.key === 'fallback') {
      // 退回
      fallbackModalVisible.value = true;
    }
  }
</script>

<style lang="less" scoped></style>
