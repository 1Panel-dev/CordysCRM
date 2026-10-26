<template>
  <CrmModal
    v-model:show="show"
    :positive-text="props.approvalType === 'approve' ? t('taskDrawer.confirmApprove') : t('taskDrawer.confirmReject')"
    :negative-text="t('common.cancel')"
    :ok-loading="props.loading"
    @confirm="handleConfirm"
    @cancel="emit('cancel')"
  >
    <template #title>
      <slot name="title">
        <div class="crm-modal-title">{{ props.title }}</div>
      </slot>
    </template>
    <n-form ref="formRef" :model="form">
      <n-form-item
        :label="opinionLabel"
        path="reason"
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
    </n-form>
  </CrmModal>
</template>

<script setup lang="ts">
  import { type FormInst, NForm, NFormItem, type UploadFileInfo } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import CrmFileInput from '@/components/business/crm-file-input/index.vue';

  const props = defineProps<{
    title?: string;
    approvalType: 'approve' | 'reject';
    loading?: boolean;
    required?: boolean;
  }>();

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
  const form = reactive({ reason: '' });
  const formRef = ref<FormInst>();
  const { t } = useI18n();
  const opinionLabel = computed(() =>
    props.approvalType === 'approve' ? t('taskDrawer.approveReason') : t('taskDrawer.rejectReason')
  );
  const quickOpinions = computed(() =>
    props.approvalType === 'approve'
      ? [t('crm.approval.quickOpinion.approve1'), t('crm.approval.quickOpinion.approve2')]
      : [t('crm.approval.quickOpinion.reject1'), t('crm.approval.quickOpinion.reject2')]
  );

  function handleConfirm() {
    form.reason = form.reason.trim();
    formRef.value?.validate((errors) => {
      if (!errors) {
        emit('confirm');
      }
    });
  }

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
