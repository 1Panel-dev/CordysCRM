<template>
  <div class="flex items-center gap-[8px]">
    <CrmIcon :type="statusMap[status]?.icon" :size="16" :class="`text-[${statusMap[status]?.color}]`" />
    {{ statusMap[status]?.label }}
  </div>
</template>

<script setup lang="ts">
  import { useMessage } from 'naive-ui';

  import { QuotationStatusEnum } from '@lib/shared/enums/opportunityEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  const { t } = useI18n();
  const Message = useMessage();

  const props = defineProps<{
    status: QuotationStatusEnum;
  }>();

  const statusMap = {
    [QuotationStatusEnum.APPROVED]: {
      label: t('common.pass'),
      icon: 'iconicon_succeed_filled',
      color: 'var(--success-green)',
    },
    [QuotationStatusEnum.UNAPPROVED]: {
      label: t('common.unPass'),
      icon: 'iconicon_close_circle_filled',
      color: 'var(--error-red)',
    },
    [QuotationStatusEnum.APPROVING]: {
      label: t('common.review'),
      icon: 'iconicon_wait',
      color: 'var(--info-blue)',
    },
    [QuotationStatusEnum.VOIDED]: {
      label: t('common.voided'),
      icon: 'iconicon_block_filled',
      color: 'var(--warning-yellow)',
    },
    [QuotationStatusEnum.REVOKED]: {
      label: t('common.revoke'),
      icon: 'iconicon_skip_planarity',
      color: 'var(--text-n4)',
    },
  };
</script>

<style scoped></style>
