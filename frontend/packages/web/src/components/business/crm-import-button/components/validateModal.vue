<template>
  <CrmModal
    v-model:show="showValidateModal"
    size="small"
    :title="t('crmImportButton.importInProgress')"
    @cancel="handleCancel"
  >
    <div class="flex items-center gap-[12px]">
      <div class="flex items-center justify-center bg-[var(--text-n9)] p-[4px]">
        <CrmIcon type="iconicon_file-excel_colorful1" :size="32" />
      </div>
      <div class="flex w-full flex-col">
        <div class="text-[var(--text-n1)]">
          {{ t('crmImportButton.validateInProgress') }}
        </div>
        <div class="flex w-full items-center gap-[12px] text-[var(--text-n4)]">
          <div class="flex-1">
            <n-progress
              :show-indicator="false"
              indicator-placement="outside"
              type="line"
              :height="6"
              :percentage="props.percent"
              processing
              color="var(--primary-8)"
            />
          </div>
          {{ props.percent }} %
        </div>
      </div>
    </div>
    <template #footer>
      <div class="flex items-center justify-end">
        <n-button quaternary class="text-btn-secondary" @click="handleCancel"> {{ t('common.cancel') }} </n-button>
      </div>
    </template>
  </CrmModal>
</template>

<script setup lang="ts">
  import { NButton, NProgress } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmModal from '@/components/pure/crm-modal/index.vue';

  const { t } = useI18n();

  const props = defineProps<{
    percent: number;
  }>();

  const emit = defineEmits<{
    (e: 'checkFinished'): void;
    (e: 'cancel'): void;
  }>();

  const showValidateModal = defineModel<boolean>('show', {
    required: true,
    default: false,
  });

  const handleCancel = () => {
    showValidateModal.value = false;
  };

  watch(
    () => props.percent,
    (val) => {
      if (val === 100) {
        handleCancel();
        emit('checkFinished');
      }
    }
  );
</script>

<style scoped></style>
