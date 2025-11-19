<template>
  <div class="flex items-center gap-[8px]">
    <CrmIcon :type="statusMap[status]?.icon" :size="16" :class="`text-[${statusMap[status]?.color}]`" />
    {{ statusMap[status]?.label }}
  </div>
</template>
<script setup lang="ts">
  import { ref } from 'vue';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { useMessage } from 'naive-ui';
  import { QuotationStatusEnum } from '@lib/shared/enums/opportunityEnum';
  const { t } = useI18n();
  const Message = useMessage();

  const props = defineProps<{
    status: QuotationStatusEnum;
  }>();

  const statusMap = {
    [QuotationStatusEnum.SUCCESS]: {
      label: t('common.pass'),
      icon: 'icon-icon-icon_succeed_filled',
      color: 'var(--success-green)',
    },
    [QuotationStatusEnum.FAIL]: {
      label: t('common.unPass'),
      icon: 'icon-icon_close_filled',
      color: 'var(--error-red)',
    },
    [QuotationStatusEnum.REVIEW]: {
      label: t('common.review'),
      icon: 'icon-icon_testing',
      color: 'var(--info-blue)',
    },
    [QuotationStatusEnum.INVALID]: {
      label: t('module.quotationStatusInvalid'),
      icon: 'icon-icon_block_filled',
      color: 'var(--warning-yellow)',
    },
    [QuotationStatusEnum.REVOKE]: {
      label: t('common.revoke'),
      icon: 'icon-icon_skip_planarity',
      color: 'var(--text-n4)',
    },
  };
</script>
<style scoped></style>
