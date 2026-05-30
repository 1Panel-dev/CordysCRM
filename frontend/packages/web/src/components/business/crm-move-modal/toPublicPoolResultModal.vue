<template>
  <CrmModal v-model:show="show" size="small" :footer="false" :title="props.title" @cancel="emit('cancel')">
    <div class="text-center">
      <CrmIcon :size="32" :type="getTipType?.icon" :class="`${getTipType?.color}`" />
      <div class="my-2 text-[16px] font-medium text-[var(--text-n1)]">{{ t(getTipType?.message ?? '') }}</div>
      <div class="leading-8 text-[var(--text-n4)]">
        <span>
          {{ t('common.transferSuccess') }}
          <span class="mx-1 text-[var(--success-green)]"> {{ props.successCount }} </span>
          {{ t('crmImportButton.countNumber') }}；
        </span>
        <span v-if="props.failCount">
          {{ t('common.transferFailed') }}
          <span class="mx-1 font-medium text-[var(--error-red)]">{{ props.failCount }}</span>
          {{ t('crmImportButton.countNumber') }}；
        </span>
      </div>
      <div v-if="props.failCount > 0" class="text-[var(--text-n4)]">
        <div v-if="props.reasonKey === ReasonTypeEnum.CUSTOMER_POOL_RS" class="flex items-center justify-center">
          {{ t('customer.moveToOpenSeaFailedContent1') }}
          <n-button type="primary" text @click="goConfig(props.reasonKey)">
            {{ t('customer.moveToOpenSeaFailedContent2') }}
          </n-button>
        </div>
        <div v-else class="flex items-center justify-center">
          {{ t('clue.moveIntoCluePoolFailedContent1') }}
          <n-button type="primary" text @click="goConfig(props.reasonKey)">
            {{ t('clue.moveIntoCluePoolFailedContent2') }}
          </n-button>
        </div>
      </div>
    </div>
  </CrmModal>
</template>

<script setup lang="ts">
  import { NButton } from 'naive-ui';

  import { ReasonTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmModal from '@/components/pure/crm-modal/index.vue';

  import useOpenNewPage from '@/hooks/useOpenNewPage';

  import { SystemRouteEnum } from '@/enums/routeEnum';

  import type { ReasonKey } from './index.vue';

  const props = defineProps<{
    failCount: number;
    successCount: number;
    title?: string;
    reasonKey: ReasonKey;
  }>();

  const emit = defineEmits<{
    (e: 'cancel'): void;
  }>();

  const { t } = useI18n();
  const { openNewPage } = useOpenNewPage();

  const show = defineModel<boolean>('show', {
    required: true,
    default: false,
  });

  const statusMap = ref<
    Record<
      'partial' | 'success' | 'error',
      {
        type: string;
        icon: string;
        color: string;
        message: string;
      }
    >
  >({
    partial: {
      type: 'warning',
      icon: 'iconicon_info_circle_filled',
      color: 'text-[var(--warning-yellow)]',
      message: 'clue.moveIntoCluePoolFailed',
    },
    success: {
      type: 'success',
      icon: 'iconicon_check_circle_filled',
      color: 'text-[var(--success-green)]',
      message: 'common.transferSuccess',
    },
    error: {
      type: 'error',
      icon: 'iconicon_close_circle_filled',
      color: 'text-[var(--error-red)]',
      message: 'common.transferFailed',
    },
  });

  const getTipType = computed(() => {
    const { successCount, failCount } = props;
    if (failCount && successCount) {
      return statusMap.value.partial;
    }
    if (!failCount) {
      return statusMap.value.success;
    }
    if (!successCount) {
      return statusMap.value.error;
    }
  });

  function goConfig(reasonKey: ReasonKey) {
    if (reasonKey === ReasonTypeEnum.CUSTOMER_POOL_RS) {
      openNewPage(SystemRouteEnum.SYSTEM_MODULE, {
        openOpenSeaDrawer: 'Y',
      });
    } else if (reasonKey === ReasonTypeEnum.CLUE_POOL_RS) {
      openNewPage(SystemRouteEnum.SYSTEM_MODULE, {
        openCluePoolDrawer: 'Y',
      });
    }
  }
</script>
