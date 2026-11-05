<template>
  <CrmModal
    v-model:show="show"
    class="approval-opinion-modal"
    :width="780"
    :positive-text="props.approvalType === 'approve' ? t('taskDrawer.confirmApprove') : t('taskDrawer.confirmReject')"
    :negative-text="t('common.cancel')"
    :ok-loading="props.loading"
    @confirm="handleConfirm"
    @cancel="emit('cancel')"
  >
    <template #title>
      <slot name="title">
        <div class="flex w-full items-center justify-start gap-[12px]">
          <div>{{ props.title }}</div>
          <CrmTag v-if="props.approvalType === 'reject'" theme="light" type="error">
            {{ t('crm.approval.reject') }}
          </CrmTag>
        </div>
      </slot>
    </template>
    <n-form ref="formRef" :model="form">
      <n-form-item
        :label="opinionLabel"
        path="reason"
        class="approval-opinion-field"
        :rule="[
          {
            required: props.required,
            message: t('common.notNull', { value: opinionLabel }),
            trigger: 'blur',
          },
        ]"
      >
        <CrmFileInput ref="fileInputRef" v-model:value="form.reason" v-model:file-list="fileList">
          <template #footer>
            <div class="flex flex-wrap gap-[8px]">
              <CrmTag
                v-for="quickOpinion of quickOpinions"
                :key="quickOpinion"
                theme="light"
                size="small"
                tooltip-disabled
                class="cursor-pointer"
                @click="form.reason = quickOpinion"
              >
                {{ quickOpinion }}
              </CrmTag>
            </div>
          </template>
        </CrmFileInput>
      </n-form-item>
      <section v-if="props.approvalType === 'reject' && props.showResubmitPath !== false" class="resubmit-section">
        <div class="resubmit-section-title">{{ t('crm.approval.resubmitPath') }}</div>
        <div class="resubmit-options" role="radiogroup" :aria-label="t('crm.approval.resubmitPath')">
          <div
            v-for="option of resubmitOptions"
            :key="option.value"
            class="resubmit-option"
            :class="{ 'resubmit-option--selected': resubmitMode === option.value }"
            role="radio"
            :aria-checked="resubmitMode === option.value"
            tabindex="0"
            @click="resubmitMode = option.value"
            @keydown="handleOptionKeydown($event, option.value)"
          >
            <span class="resubmit-option-radio" aria-hidden="true"></span>
            <span class="resubmit-option-copy">
              <span class="resubmit-option-title">{{ option.label }}</span>
              <span class="resubmit-option-description">{{ option.description }}</span>
            </span>
          </div>
        </div>
        <n-form-item
          v-if="resubmitMode === 'SPECIFIED_NODE'"
          path="resubmitStartNodeNumber"
          :label="t('crm.approval.resubmitSpecifiedNode')"
          :rule="[
            {
              required: true,
              message: t('common.pleaseSelect'),
              trigger: 'blur',
            },
          ]"
          class="resubmit-node-field"
        >
          <n-select
            v-model:value="form.resubmitStartNodeNumber"
            :options="props.resubmitNodeOptions"
            :render-label="renderResubmitNodeLabel"
            :placeholder="t('crm.approval.resubmitSelectNode')"
          />
        </n-form-item>
      </section>
    </n-form>
  </CrmModal>
</template>

<script setup lang="ts">
  import { h } from 'vue';
  import { type FormInst, NForm, NFormItem, NSelect, type UploadFileInfo } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import CrmFileInput from '@/components/business/crm-file-input/index.vue';

  import type { SelectOption } from 'naive-ui/es/select/src/interface';

  const props = withDefaults(
    defineProps<{
      title?: string;
      approvalType: 'approve' | 'reject';
      loading?: boolean;
      required?: boolean;
      showResubmitPath?: boolean;
      currentNodeName?: string;
      currentNodeIsSign?: boolean;
      resubmitNodeOptions?: Array<{ label: string; value: string; tag?: 'submitter' | 'current' }>;
    }>(),
    {
      showResubmitPath: true,
    }
  );

  const emit = defineEmits<{
    (e: 'confirm'): void;
    (e: 'cancel'): void;
  }>();

  const show = defineModel<boolean>('show', {
    required: true,
  });
  const opinion = defineModel<string>('opinion', {
    required: true,
  });
  const fileList = defineModel<UploadFileInfo[]>('fileList', {
    required: true,
  });
  const resubmitMode = defineModel<'SUBMITTER' | 'CURRENT_NODE' | 'SPECIFIED_NODE'>('resubmitMode', {
    default: 'SUBMITTER',
  });
  const resubmitStartNodeNumber = defineModel<string | null>('resubmitStartNodeNumber', {
    default: null,
  });
  const form = reactive({ reason: '', resubmitStartNodeNumber: null as string | null });
  const formRef = ref<FormInst>();
  const { t } = useI18n();
  const opinionLabel = computed(() =>
    props.approvalType === 'approve' ? t('taskDrawer.approveReason') : t('taskDrawer.rejectReason')
  );
  const currentNodeDescription = computed(() => {
    const key = props.currentNodeIsSign
      ? 'crm.approval.resubmitCurrentSignNodeDesc'
      : 'crm.approval.resubmitCurrentNodeDesc';
    return t(key, { nodeName: props.currentNodeName || '' });
  });
  const resubmitOptions = computed(() => [
    {
      value: 'SUBMITTER' as const,
      label: t('crm.approval.resubmitFromSubmitter'),
      description: t('crm.approval.resubmitFromSubmitterDesc'),
    },
    {
      value: 'CURRENT_NODE' as const,
      label: t('crm.approval.resubmitFromCurrentNode'),
      description: currentNodeDescription.value,
    },
    {
      value: 'SPECIFIED_NODE' as const,
      label: t('crm.approval.resubmitFromSpecifiedNode'),
      description: t('crm.approval.resubmitFromSpecifiedNodeDesc'),
    },
  ]);
  const quickOpinions = computed(() =>
    props.approvalType === 'approve'
      ? [t('crm.approval.quickOpinion.approve1'), t('crm.approval.quickOpinion.approve2')]
      : [t('crm.approval.quickOpinion.reject1'), t('crm.approval.quickOpinion.reject2')]
  );

  function getResubmitNodeTagLabel(tag?: 'submitter' | 'current') {
    switch (tag) {
      case 'submitter':
        return t('crm.approval.resubmitSubmitterTag');
      case 'current':
        return t('crm.approval.resubmitCurrentNodeTag');
      default:
        return '';
    }
  }

  function renderResubmitNodeLabel(option: SelectOption) {
    const nodeOption = option as SelectOption & { tag?: 'submitter' | 'current' };
    const tagLabel = getResubmitNodeTagLabel(nodeOption.tag);

    return h(
      'div',
      {
        style: {
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '12px',
          width: '100%',
        },
      },
      [
        h('span', String(option.label ?? '')),
        tagLabel
          ? h(
              CrmTag,
              {
                theme: 'light',
                type: nodeOption.tag === 'current' ? 'info' : 'default',
                size: 'small',
                tooltipDisabled: true,
              },
              { default: () => tagLabel }
            )
          : null,
      ]
    );
  }

  function handleConfirm() {
    form.reason = form.reason.trim();
    formRef.value?.validate((errors) => {
      if (!errors) {
        emit('confirm');
      }
    });
  }

  function handleOptionKeydown(event: KeyboardEvent, value: 'SUBMITTER' | 'CURRENT_NODE' | 'SPECIFIED_NODE') {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      resubmitMode.value = value;
    }
  }

  watch(
    resubmitStartNodeNumber,
    (value) => {
      form.resubmitStartNodeNumber = value;
    },
    { immediate: true }
  );

  watch(
    () => form.resubmitStartNodeNumber,
    (value) => {
      resubmitStartNodeNumber.value = value;
    }
  );

  watch(
    () => props.approvalType,
    (value) => {
      if (value === 'approve') {
        resubmitMode.value = 'SUBMITTER';
        form.resubmitStartNodeNumber = null;
      }
    }
  );

  watch(
    opinion,
    (value) => {
      form.reason = value;
      formRef.value?.validate();
    },
    { immediate: true }
  );

  watch(
    () => form.reason,
    (value) => {
      opinion.value = value;
    }
  );
</script>

<style lang="less" scoped>
  .approval-opinion-field {
    margin-bottom: 16px;
  }
  .resubmit-section-title {
    margin-bottom: 12px;
    font-weight: 600;
    color: var(--text-n1);
  }
  .resubmit-options {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }
  .resubmit-option {
    display: flex;
    align-items: center;
    padding: 8px;
    border: 1px solid var(--text-n8);
    border-radius: 4px;
    background: var(--text-n10);
    transition: border-color 120ms ease, background-color 120ms ease;
    gap: 12px;
    cursor: pointer;
    &:focus-visible {
      outline: 2px solid var(--primary-8);
      outline-offset: 2px;
    }
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
  .resubmit-option-radio {
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
    color: var(--text-n4);
    line-height: 20px;
  }
  .resubmit-node-field {
    margin-top: 16px;
    margin-bottom: 0;
  }
  :deep(.approval-opinion-modal .n-dialog) {
    max-width: calc(100vw - 32px);
  }
</style>
