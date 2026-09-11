<template>
  <CrmTag
    plain
    :tag="processStatusMap[props.status].label"
    :text-color="processStatusMap[props.status].color"
    :bgColor="processStatusMap[props.status].bgColor"
  />
</template>
<script setup lang="ts">
  import { ProcessStatusEnum } from '@lib/shared/enums/process';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  const props = defineProps<{
    status: ProcessStatusEnum;
  }>();

  const { t } = useI18n();

  const processStatusMap: Record<ProcessStatusEnum, any> = {
    [ProcessStatusEnum.APPROVED]: {
      label: t('workbench.result.APPROVED'),
      icon: 'iconicon_succeed_filled',
      color: 'var(--success-green)',
      bgColor: 'var(--success-green)',
    },
    [ProcessStatusEnum.AUTO_APPROVED]: {
      label: t('workbench.result.AUTO_APPROVED'),
      icon: 'iconicon_succeed_filled',
      color: 'var(--success-green)',
      bgColor: 'var(--success-green)',
    },
    [ProcessStatusEnum.APPROVING]: {
      label: t('workbench.result.APPROVING'),
      icon: 'iconicon_wait',
      color: 'var(--warning-yellow)',
      bgColor: 'var(--warning-yellow)',
    },
    [ProcessStatusEnum.AUTO_UNAPPROVED]: {
      label: t('workbench.result.AUTO_UNAPPROVED'),
      icon: 'iconicon_close_circle_filled',
      color: 'var(--error-red)',
      bgColor: 'var(--error-red)',
    },
    [ProcessStatusEnum.UNAPPROVED]: {
      label: t('workbench.result.UNAPPROVED'),
      icon: 'iconicon_close_circle_filled',
      color: 'var(--error-red)',
      bgColor: 'var(--error-red)',
    },
    [ProcessStatusEnum.REVOKED]: {
      label: t('workbench.result.REVOKED'),
      icon: 'iconicon_skip_planarity',
      color: 'var(--text-n4)',
      bgColor: '',
    },
    [ProcessStatusEnum.PENDING]: {
      label: t('workbench.result.PENDING'),
      icon: 'iconicon_minus_circle_filled1',
      color: 'var(--text-n4)',
      bgColor: '',
    },

    [ProcessStatusEnum.NONE]: {
      label: '-',
      icon: '',
      color: '',
      bgColor: '',
    },
  };
</script>
<style lang="less" scoped></style>
