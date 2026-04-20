import {
  ContractBusinessTitleStatusEnum,
  ContractPaymentPlanEnum,
  ContractStatusEnum,
} from '@lib/shared/enums/contractEnum';
import { ProcessStatusEnum } from '@lib/shared/enums/process';
import { useI18n } from '@lib/shared/hooks/useI18n';
import { SaveBusinessTitleParams } from '@lib/shared/models/contract';
import { ProcessStatusType } from '@lib/shared/models/system/process';

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
    value: ContractStatusEnum.CHANGE,
    label: t('contract.change'),
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
    label: t('contract.archive'),
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

export const contractBusinessTitleStatusMap = {
  [ContractBusinessTitleStatusEnum.APPROVED]: {
    label: t('common.pass'),
    icon: 'iconicon_succeed_filled',
    color: 'var(--success-green)',
  },
  [ContractBusinessTitleStatusEnum.UNAPPROVED]: {
    label: t('common.unPass'),
    icon: 'iconicon_close_circle_filled',
    color: 'var(--error-red)',
  },
  [ContractBusinessTitleStatusEnum.APPROVING]: {
    label: t('common.review'),
    icon: 'iconicon_wait',
    color: 'var(--info-blue)',
  },
  [ContractBusinessTitleStatusEnum.REVOKED]: {
    label: t('common.revoke'),
    icon: 'iconicon_skip_planarity',
    color: 'var(--text-n4)',
  },
};

export const contractBusinessTitleStatusOptions = Object.entries(contractBusinessTitleStatusMap).map(
  ([key, value]) => ({
    label: value.label,
    value: key,
  })
);

export const businessTitleFormConfigList: {
  label: string;
  value: keyof SaveBusinessTitleParams;
}[] = [
  {
    label: t('contract.businessTitle.taxpayerNumber'),
    value: 'identificationNumber',
  },
  {
    label: t('contract.businessTitle.address'),
    value: 'registrationAddress',
  },
  {
    label: t('contract.businessTitle.bank'),
    value: 'openingBank',
  },
  {
    label: t('contract.businessTitle.bankAccount'),
    value: 'bankAccount',
  },
  {
    label: t('contract.businessTitle.phone'),
    value: 'phoneNumber',
  },
  {
    label: t('contract.businessTitle.capital'),
    value: 'registeredCapital',
  },
  {
    label: t('contract.businessTitle.companyScale'),
    value: 'companySize',
  },
  {
    label: t('contract.businessTitle.registrationAccount'),
    value: 'registrationNumber',
  },
  {
    label: t('contract.businessTitle.province'),
    value: 'province',
  },
  {
    label: t('contract.businessTitle.city'),
    value: 'city',
  },
  {
    label: t('contract.businessTitle.scale'),
    value: 'scale',
  },
  {
    label: t('contract.businessTitle.industry'),
    value: 'industry',
  },
  {
    label: t('common.remark'),
    value: 'remark',
  },
];

export const allBusinessTitleFormConfigList: {
  label: string;
  value: keyof SaveBusinessTitleParams;
}[] = [
  {
    label: t('contract.businessTitle.companyName'),
    value: 'name',
  },
  ...businessTitleFormConfigList,
];

export const deleteInvoiceContentMap: Record<ProcessStatusType, string> = {
  [ProcessStatusEnum.APPROVING]: t('contract.deleteInvoiceUnderReviewContent'),
  [ProcessStatusEnum.APPROVED]: t('contract.deleteInvoiceApprovedContent'),
  [ProcessStatusEnum.REVOKED]: t('contract.deleteInvoiceRevokedContent'),
  [ProcessStatusEnum.UNAPPROVED]: t('contract.deleteInvoiceRejectedContent'),
  [ProcessStatusEnum.PENDING]: t('contract.deleteInvoiceNoneContent'),
  [ProcessStatusEnum.NONE]: t('contract.deleteInvoiceNoneContent'),
};
