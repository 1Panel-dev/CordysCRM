import { ContractPaymentPlanEnum, ContractStatusEnum } from '@lib/shared/enums/contractEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';

import { hasAllPermission } from '@/utils/permission';

const { t } = useI18n();

// 计划状态
export const contractPaymentPlanStatus = {
  [ContractPaymentPlanEnum.PENDING]: {
    label: t('contract.uncompleted'),
    icon: 'iconicon_close_circle_filled',
    color: 'var(--text-n4)',
  },
  [ContractPaymentPlanEnum.PARTIALLY_COMPLETED]: {
    label: t('contract.partialCompleted'),
    icon: 'iconicon_pie',
    color: 'var(--info-blue)',
  },
  [ContractPaymentPlanEnum.COMPLETED]: {
    label: t('common.completed'),
    icon: 'iconicon_check_circle_filled',
    color: 'var(--success-green)',
  },
};

// 合同状态
export const contractStatusMap = {
  [ContractStatusEnum.SIGNED]: {
    label: t('contract.signed'),
    icon: 'iconicon_check_circle_filled',
    color: 'var(--success-green)',
  },
  [ContractStatusEnum.IN_PROGRESS]: {
    label: t('contract.inProgress'),
    icon: 'iconicon_testing',
    color: 'var(--info-blue)',
  },
  [ContractStatusEnum.COMPLETED_PERFORMANCE]: {
    label: t('contract.completedPerformance'),
    icon: 'iconicon_check_circle_filled',
    color: 'var(--text-n4)',
  },
  [ContractStatusEnum.VOID]: {
    label: t('common.voided'),
    icon: 'iconicon_minus_circle_filled1',
    color: 'var(--warning-yellow)',
  },
};

export const contractPaymentPlanStatusOptions = Object.entries(contractPaymentPlanStatus).map(([key, value]) => ({
  label: value.label,
  value: key,
}));

export const contractStatusOptions = Object.entries(contractStatusMap).map(([key, value]) => ({
  label: value.label,
  value: key,
}));
