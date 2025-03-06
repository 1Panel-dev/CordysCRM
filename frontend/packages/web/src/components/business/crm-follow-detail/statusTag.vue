<template>
  <CrmTag v-if="props.status !== CustomerFollowPlanStatusEnum.ALL" :type="statusMap[props.status].type" theme="light">
    <template #icon>
      <CrmIcon :type="statusMap[props.status].icon" :size="16" :class="`text-[${statusMap[props.status].color}]`" />
    </template>
    <span :class="`text-[${statusMap[props.status].color}]`">{{ t(statusMap[props.status].label) }}</span>
  </CrmTag>
</template>

<script lang="ts" setup>
  import { CustomerFollowPlanStatusEnum } from '@lib/shared/enums/customerEnum';

  import CrmTag from '@/components/pure/crm-tag/index.vue';

  import { useI18n } from '@/hooks/useI18n';

  const { t } = useI18n();

  const props = defineProps<{
    status: CustomerFollowPlanStatusEnum;
  }>();

  const statusMap: Record<
    Exclude<CustomerFollowPlanStatusEnum, CustomerFollowPlanStatusEnum.ALL>,
    {
      label: string;
      value: CustomerFollowPlanStatusEnum;
      type: 'default' | 'primary' | 'info' | 'success' | 'warning' | 'error';
      icon: string;
      color: string;
    }
  > = {
    [CustomerFollowPlanStatusEnum.PREPARED]: {
      label: 'common.notStarted',
      value: CustomerFollowPlanStatusEnum.PREPARED,
      type: 'default',
      icon: 'iconicon_block_filled',
      color: 'var(--text-n4)',
    },
    [CustomerFollowPlanStatusEnum.UNDERWAY]: {
      label: 'common.inProgress',
      value: CustomerFollowPlanStatusEnum.UNDERWAY,
      type: 'info',
      icon: 'iconicon_testing',
      color: 'var(--info-blue)',
    },
    [CustomerFollowPlanStatusEnum.COMPLETED]: {
      label: 'common.completed',
      value: CustomerFollowPlanStatusEnum.COMPLETED,
      type: 'success',
      icon: 'iconicon_succeed_filled',
      color: 'var(--success-green)',
    },
    [CustomerFollowPlanStatusEnum.CANCELLED]: {
      label: 'common.canceled',
      value: CustomerFollowPlanStatusEnum.CANCELLED,
      type: 'default',
      icon: 'iconicon_block_filled',
      color: 'var(--text-n6)',
    },
  };
</script>

<style lang="less" scoped></style>
