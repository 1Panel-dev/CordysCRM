import {
  ContractBusinessNameStatusEnum,
  ContractPaymentPlanEnum,
  ContractStatusEnum,
} from '@lib/shared/enums/contractEnum';
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

export const contractStatusOptions = [
  {
    value: ContractStatusEnum.PENDING_SIGNING,
    label: t('contract.toBeSigned'),
  },
  {
    value: ContractStatusEnum.SIGNED,
    label: t('contract.signed'),
  },
  {
    value: ContractStatusEnum.IN_PROGRESS,
    label: t('contract.inProgress'),
  },
  {
    value: ContractStatusEnum.COMPLETED_PERFORMANCE,
    label: t('contract.completedPerformance'),
  },
  {
    value: ContractStatusEnum.ARCHIVED,
    label: t('common.archive'),
  },
  {
    value: ContractStatusEnum.VOID,
    label: t('common.voided'),
  },
];

export const contractPaymentPlanStatusOptions = Object.entries(contractPaymentPlanStatus).map(([key, value]) => ({
  label: value.label,
  value: key,
}));

export const contractBusinessNameStatusMap = {
  [ContractBusinessNameStatusEnum.APPROVED]: {
    label: t('common.pass'),
    icon: 'iconicon_succeed_filled',
    color: 'var(--success-green)',
  },
  [ContractBusinessNameStatusEnum.UNAPPROVED]: {
    label: t('common.unPass'),
    icon: 'iconicon_close_circle_filled',
    color: 'var(--error-red)',
  },
  [ContractBusinessNameStatusEnum.APPROVING]: {
    label: t('common.review'),
    icon: 'iconicon_wait',
    color: 'var(--info-blue)',
  },
  [ContractBusinessNameStatusEnum.REVOKED]: {
    label: t('common.revoke'),
    icon: 'iconicon_skip_planarity',
    color: 'var(--text-n4)',
  },
};

export const contractBusinessNameStatusOptions = Object.entries(contractBusinessNameStatusMap).map(([key, value]) => ({
  label: value.label,
  value: key,
}));
