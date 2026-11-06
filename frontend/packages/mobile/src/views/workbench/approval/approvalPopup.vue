<template>
  <van-popup
    v-model:show="showApprovalPopup"
    position="bottom"
    :style="{ height: '100vh' }"
    safe-area-inset-top
    safe-area-inset-bottom
  >
    <div class="flex flex-col">
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
      <div class="h-full flex-1 overflow-y-auto pb-[80px]">
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
            :required="isCommentRequired"
            :rules="[
              {
                required: isCommentRequired,
                message: t('common.notNull', {
                  value: isRejecting ? t('workbench.rejectReason') : t('workbench.approvedReason'),
                }),
              },
            ]"
          />
          <div class="mt-[8px] flex flex-wrap gap-[4px] px-[16px]">
            <CrmTag
              v-for="quickOpinion of quickOpinions"
              :key="quickOpinion"
              :tag="quickOpinion"
              class="cursor-pointer"
              @click="approvalForm.reason = quickOpinion"
            />
          </div>
          <CrmUploadFile
            :key="uploadKey"
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
              limitSize: '50MB',
            }"
          />
          <section v-if="isRejecting && !props.selectedKeys?.length" class="resubmit-section">
            <div class="resubmit-section-title">{{ t('workbench.resubmitPath') }}</div>
            <div class="resubmit-options" role="radiogroup" :aria-label="t('workbench.resubmitPath')">
              <div
                v-for="option of resubmitOptions"
                :key="option.value"
                class="resubmit-option"
                :class="{
                  'resubmit-option--selected': resubmitMode === option.value,
                  'resubmit-option--disabled': option.disabled,
                }"
                role="radio"
                :aria-checked="resubmitMode === option.value"
                :aria-disabled="option.disabled"
                @click="selectResubmitMode(option.value)"
              >
                <span class="resubmit-option-radio" aria-hidden="true"></span>
                <span class="resubmit-option-copy">
                  <span class="resubmit-option-title">{{ option.label }}</span>
                  <span class="resubmit-option-description">{{ option.description }}</span>
                </span>
              </div>
            </div>
            <div v-if="resubmitMode === 'SPECIFIED_NODE'" class="resubmit-node-field">
              <van-field
                name="resubmitStartNodeNumber"
                :model-value="selectedNodeLabel"
                is-link
                readonly
                label-align="left"
                input-align="right"
                :label="t('workbench.resubmitSpecifiedNode')"
                :placeholder="t('common.pleaseSelect')"
                required
                :rules="[
                  {
                    validator: () => selectedResubmitStartNodeNumber !== null,
                    message: t('common.pleaseSelect'),
                  },
                ]"
                class="!px-0"
                :border="false"
                @click="showNodePicker = true"
              />
            </div>
          </section>
        </van-form>
      </div>
    </div>
    <div
      class="fixed bottom-0 left-0 right-0 flex items-center justify-between gap-[16px] bg-[var(--text-n10)] p-[16px]"
    >
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
        :disabled="isCommentRequired && !approvalForm.reason.trim()"
        @click="handleApprovalSave"
      >
        {{ isRejecting ? t('workbench.operation.confirmRejected') : t('workbench.operation.confirmApproved') }}
      </van-button>
    </div>
    <van-popup
      v-model:show="showNodePicker"
      position="bottom"
      round
      :style="{ height: '70vh', maxHeight: '85vh' }"
      safe-area-inset-bottom
    >
      <div class="flex h-full flex-col bg-[var(--text-n10)]">
        <div class="relative shrink-0 p-[16px] text-center">
          <CrmTextButton
            icon="iconicon_chevron_left"
            icon-size="24px"
            color="var(--text-n1)"
            class="absolute left-[12px] top-[16px]"
            @click="showNodePicker = false"
          />
          <div class="text-[16px] font-semibold">{{ t('workbench.resubmitSpecifiedNode') }}</div>
        </div>
        <div class="min-h-0 flex-1 overflow-y-auto px-[24px]">
          <div
            v-for="option of resubmitNodeOptions"
            :key="option.value"
            class="resubmit-node-option"
            :class="{ 'resubmit-node-option--selected': selectedResubmitStartNodeNumber === option.value }"
            role="radio"
            :aria-checked="selectedResubmitStartNodeNumber === option.value"
            @click="selectResubmitNode(option.value)"
          >
            <span class="resubmit-node-radio" aria-hidden="true"></span>
            <span class="resubmit-node-name">{{ option.label }}</span>
            <span
              v-if="option.tag"
              class="resubmit-node-tag"
              :class="option.tag === 'current' ? 'resubmit-node-tag--current' : 'resubmit-node-tag--submitter'"
            >
              {{
                option.tag === 'current' ? t('workbench.resubmitCurrentNodeTag') : t('workbench.resubmitSubmitterTag')
              }}
            </span>
          </div>
        </div>
      </div>
    </van-popup>
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
    getApprovalResubmitNodes,
    getApprovalResourceDetail,
    rejectApproval,
  } from '@/api/modules';
  import { showToast, type FormInstance } from 'vant';
  import type { ApprovalDetail, ApprovalProcessDetail, ApprovalTodoItem } from '@lib/shared/models/system/process';

  type ResubmitMode = 'SUBMITTER' | 'CURRENT_NODE' | 'SPECIFIED_NODE';

  const props = defineProps<{
    approvingItem?: Partial<ApprovalTodoItem>;
    approvalItemKeys?: string[];
    isRejecting: boolean;
    selectedKeys?: string[];
    resourceType: string;
    approvalInfo?: ApprovalDetail;
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
  const uploadKey = ref(0);
  const loading = ref(false);
  const approvalFormRef = ref<FormInstance>();
  const approvalInfo = ref<ApprovalDetail>();
  const approvalDetailLoading = ref(false);
  const resubmitMode = ref<ResubmitMode>('SUBMITTER');
  const selectedResubmitStartNodeNumber = ref<string | null>(null);
  const showNodePicker = ref(false);

  const currentApprovalNode = computed(() =>
    approvalInfo.value?.nodes.find((node) => node.nodeId === approvalInfo.value?.currentNodeId)
  );
  const currentNodeIsSign = computed(
    () =>
      currentApprovalNode.value?.taskNodes?.find((taskNode) => taskNode.taskId === props.approvingItem?.approvalTaskId)
        ?.sign
  );
  const resubmitNodeOptions = ref<Array<{ label: string; value: string; tag?: 'submitter' | 'current' }>>([]);
  const selectedNodeLabel = computed(
    () =>
      resubmitNodeOptions.value.find((option) => option.value === selectedResubmitStartNodeNumber.value)?.label || ''
  );
  const currentNodeDescription = computed(() => {
    const key = currentNodeIsSign.value ? 'workbench.resubmitCurrentSignNodeDesc' : 'workbench.resubmitCurrentNodeDesc';
    return t(key, { nodeName: currentApprovalNode.value?.nodeName || '' });
  });
  const resubmitOptions = computed(() => [
    {
      value: 'SUBMITTER' as const,
      label: t('workbench.resubmitFromSubmitter'),
      description: t('workbench.resubmitFromSubmitterDesc'),
    },
    {
      value: 'CURRENT_NODE' as const,
      label: t('workbench.resubmitFromCurrentNode'),
      description: currentNodeDescription.value,
      disabled: !currentApprovalNode.value,
    },
    {
      value: 'SPECIFIED_NODE' as const,
      label: t('workbench.resubmitFromSpecifiedNode'),
      description: t('workbench.resubmitFromSpecifiedNodeDesc'),
      disabled: !resubmitNodeOptions.value.length,
    },
  ]);

  function selectResubmitMode(mode: ResubmitMode) {
    const option = resubmitOptions.value.find((item) => item.value === mode);
    if (option?.disabled) return;
    resubmitMode.value = mode;
    if (mode !== 'SPECIFIED_NODE') {
      selectedResubmitStartNodeNumber.value = null;
    }
  }

  function selectResubmitNode(nodeNumber: string) {
    selectedResubmitStartNodeNumber.value = nodeNumber;
    showNodePicker.value = false;
  }

  async function initResubmitNodeOptions(taskId: string) {
    try {
      const nodes = await getApprovalResubmitNodes(taskId);
      const currentNode = approvalInfo.value?.nodes.find((item) => item.nodeId === approvalInfo.value?.currentNodeId);
      resubmitNodeOptions.value = nodes.map((node, index) => {
        let tag: 'current' | 'submitter' | undefined;
        if (
          node.nodeNumber &&
          (node.nodeNumber === props.approvingItem?.approvalNodeNumber || currentNode?.nodeNumber === node.nodeNumber)
        ) {
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

  async function initApprovalInfo() {
    approvalInfo.value = props.approvalInfo;
    if (approvalInfo.value || !props.approvingItem?.resourceId) return;

    try {
      approvalDetailLoading.value = true;
      approvalInfo.value = await getApprovalResourceDetail(
        props.approvingItem.resourceId,
        props.approvingItem.commentMentionId
      );
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      approvalDetailLoading.value = false;
    }
  }

  function resetApprovalForm() {
    approvalForm.value = {
      reason: '',
      fileList: [],
    };
    approvalFormRef.value?.resetValidation();
    uploadKey.value += 1;
    resubmitMode.value = 'SUBMITTER';
    selectedResubmitStartNodeNumber.value = null;
    resubmitNodeOptions.value = [];
    showNodePicker.value = false;
  }

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
          resubmitMode: resubmitMode.value,
          resubmitStartNodeNumber:
            resubmitMode.value === 'SPECIFIED_NODE'
              ? selectedResubmitStartNodeNumber.value ?? undefined
              : resubmitMode.value === 'CURRENT_NODE'
              ? resubmitNodeOptions.value.find((option) => option.tag === 'current')?.value
              : undefined,
        });
      } else {
        await agreeApproval({
          id: props.approvingItem?.approvalTaskId || '',
          nodeId: props.approvingItem?.approvalNodeId || '',
          instanceId: props.approvingItem?.approvalInstanceId || '',
          attachmentIds: approvalForm.value.fileList,
          approverId: props.approvingItem?.approvalId!,
          comment: approvalForm.value.reason,
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
    approvalForm.value.reason = approvalForm.value.reason.trim();
    if (approvalFormRef.value?.validate()) {
      if (props.selectedKeys?.length) {
        batchApproval();
      } else {
        approval();
      }
    }
  }

  const approvalConfig = ref<ApprovalProcessDetail>(); // 审批配置详情
  const isCommentRequired = computed(() =>
    props.isRejecting ? approvalConfig.value?.requireRejectComment : approvalConfig.value?.requireApproveComment
  );
  const quickOpinions = computed(() =>
    props.isRejecting
      ? [t('workbench.approval.quickOpinion.reject1'), t('workbench.approval.quickOpinion.reject2')]
      : [t('workbench.approval.quickOpinion.approve1'), t('workbench.approval.quickOpinion.approve2')]
  );

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
    async (val) => {
      if (val) {
        resetApprovalForm();
        initApprovalConfig();
        if (props.isRejecting && !props.selectedKeys?.length) {
          await initApprovalInfo();
          if (props.approvingItem?.approvalTaskId) {
            await initResubmitNodeOptions(props.approvingItem.approvalTaskId);
          }
        } else {
          approvalInfo.value = props.approvalInfo;
          resubmitNodeOptions.value = [];
        }
      }
    }
  );
</script>
<style lang="less" scoped>
  .resubmit-section {
    padding: 16px 16px 0;
  }
  .resubmit-section-title {
    margin-bottom: 12px;
    font-weight: 600;
    color: var(--text-n1);
  }
  .resubmit-node-field {
    position: relative;
    &::after {
      position: absolute;
      right: 0;
      bottom: 0;
      left: 0;
      height: 1px;
      background: var(--text-n8);
      content: '';
      pointer-events: none;
      transform: scaleY(0.5);
      transform-origin: 0 100%;
    }
  }
  .resubmit-options {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }
  .resubmit-option {
    display: flex;
    align-items: flex-start;
    padding: 12px;
    border: 1px solid var(--text-n7);
    border-radius: 6px;
    background: var(--text-n10);
    gap: 10px;
  }
  .resubmit-option--selected {
    border-color: var(--primary-8);
    background: var(--primary-10, #f0fbfb);
    .resubmit-option-radio {
      border-color: var(--primary-8);
      &::after {
        transform: scale(1);
      }
    }
  }
  .resubmit-option--disabled {
    opacity: 0.5;
  }
  .resubmit-option-radio,
  .resubmit-node-radio {
    display: flex;
    justify-content: center;
    align-items: center;
    width: 20px;
    height: 20px;
    border: 1px solid var(--text-n6);
    border-radius: 50%;
    flex: 0 0 20px;
    &::after {
      width: 10px;
      height: 10px;
      border-radius: 50%;
      background: var(--primary-8);
      content: '';
      transform: scale(0);
      transition: transform 120ms ease;
    }
  }
  .resubmit-option-copy {
    display: flex;
    min-width: 0;
    flex-direction: column;
    gap: 4px;
  }
  .resubmit-option-title {
    color: var(--text-n1);
    line-height: 22px;
  }
  .resubmit-option-description {
    font-size: 12px;
    color: var(--text-n4);
    line-height: 18px;
  }
  .resubmit-node-option {
    display: flex;
    align-items: center;
    min-height: 58px;
    border-bottom: 1px solid var(--text-n8);
    gap: 12px;
  }
  .resubmit-node-option--selected {
    .resubmit-node-radio {
      border-color: var(--primary-8);
      &::after {
        transform: scale(1);
      }
    }
  }
  .resubmit-node-name {
    overflow: hidden;
    min-width: 0;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: var(--text-n1);
    flex: 1;
  }
  .resubmit-node-tag {
    padding: 4px 8px;
    font-size: 12px;
    border-radius: 3px;
    white-space: nowrap;
    line-height: 18px;
  }
  .resubmit-node-tag--submitter {
    color: var(--text-n2);
    background: var(--text-n8);
  }
  .resubmit-node-tag--current {
    color: var(--primary-8);
    background: var(--primary-7);
  }
</style>
