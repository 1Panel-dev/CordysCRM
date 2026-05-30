<template>
  <CrmModal
    v-model:show="visible"
    size="small"
    :title="
      t('opportunity.operationResult', {
        name: props.name,
      })
    "
  >
    <div class="flex flex-col items-center gap-[8px]">
      <CrmIcon :size="32" :type="getTipType?.icon" :class="`${getTipType?.color}`" />
      <div class="text-[16px] font-medium text-[var(--text-n1)]">{{ t(getTipType?.message ?? '') }}</div>
      <div class="text-[var(--text-n4)]">
        <span>
          {{ t('common.success') }}
          <span class="mx-1 text-[var(--success-green)]"> {{ props.result.success }} </span>
          {{ t('crmImportButton.countNumber') }}；
        </span>
        <span v-if="props.result.fail">
          {{ t('common.fail') }}
          <span class="mx-1 font-medium text-[var(--error-red)]">{{ props.result.fail }}</span>
          {{ t('crmImportButton.countNumber') }}；
        </span>
        <span v-if="props.result.skip">
          {{ t('common.skipped') }}
          <span class="mx-1 text-[var(--info-blue)]"> {{ props.result?.skip ?? 0 }} </span>
          {{ t('crmImportButton.countNumber') }}；
        </span>
      </div>
      <div v-if="props.result.fail" class="text-[var(--text-n4)]">
        <span>{{ t('opportunity.failureReason') }}：</span> <span>{{ t(props.result?.errorMessages ?? '') }}</span>
      </div>
    </div>
    <template #footer>
      <n-button quaternary type="primary" class="text-btn-primary" @click="handleCancel">
        {{ t('crmImportButton.backList') }}
      </n-button>
    </template>
  </CrmModal>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NButton } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { BatchOperationResult } from '@lib/shared/models/opportunity';

  import CrmModal from '@/components/pure/crm-modal/index.vue';

  const { t } = useI18n();

  interface ResultStatus {
    type: string;
    icon: string;
    color: string;
    message: string;
  }

  const props = defineProps<{
    result: BatchOperationResult;
    name?: string;
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const resultStatusMap = ref<Record<'partial' | 'success' | 'error', ResultStatus>>({
    partial: {
      type: 'warning',
      icon: 'iconicon_info_circle_filled',
      color: 'text-[var(--warning-yellow)]',
      message: 'opportunity.partialFailed',
    },
    success: {
      type: 'success',
      icon: 'iconicon_check_circle_filled',
      color: 'text-[var(--success-green)]',
      message: 'opportunity.allSuccessful',
    },
    error: {
      type: 'error',
      icon: 'iconicon_close_circle_filled',
      color: 'text-[var(--error-red)]',
      message: 'opportunity.allError',
    },
  });

  const getTipType = computed(() => {
    const { success, fail } = props.result;
    if (fail && success) {
      return resultStatusMap.value.partial;
    }
    if (!fail) {
      return resultStatusMap.value.success;
    }
    if (!success) {
      return resultStatusMap.value.error;
    }
  });

  function handleCancel() {
    visible.value = false;
  }
</script>

<style scoped></style>
