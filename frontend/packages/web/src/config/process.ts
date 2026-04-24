import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { ProcessStatusEnum } from '@lib/shared/enums/process';
import { useI18n } from '@lib/shared/hooks/useI18n';
import { BasicFormParams, MoreSettingsParams, ProcessStatusType } from '@lib/shared/models/system/process';

import { StatusInfo } from '@/components/business/crm-approval-status/index.vue';

const { t } = useI18n();

export const processStatusMap: Record<ProcessStatusType, StatusInfo> = {
  [ProcessStatusEnum.APPROVED]: {
    label: t('common.approved'),
    icon: 'iconicon_succeed_filled',
    color: 'var(--success-green)',
    tagBgColor: 'var(--success-5)',
    tagColor: 'var(--success-green)',
  },
  [ProcessStatusEnum.APPROVING]: {
    label: t('common.reviewing'),
    icon: 'iconicon_wait',
    color: 'var(--info-blue)',
    tagBgColor: 'var(--warning-5)',
    tagColor: 'var(--warning-yellow)',
  },
  [ProcessStatusEnum.UNAPPROVED]: {
    label: t('common.rejected'),
    icon: 'iconicon_close_circle_filled',
    color: 'var(--error-red)',
    tagBgColor: 'var(--error-5)',
    tagColor: 'var(--error-red)',
  },
  [ProcessStatusEnum.REVOKED]: {
    label: t('common.revoked'),
    icon: 'iconicon_skip_planarity',
    color: 'var(--text-n4)',
    tagBgColor: '',
    tagColor: '',
  },
  [ProcessStatusEnum.PENDING]: {
    label: t('common.pending'),
    icon: 'iconicon_minus_circle_filled1',
    color: 'var(--text-n4)',
    tagBgColor: '',
    tagColor: '',
  },

  [ProcessStatusEnum.NONE]: {
    label: '-',
    icon: '',
    color: '',
    tagBgColor: '',
    tagColor: '',
  },
};

export const processStatusOptions = Object.entries(processStatusMap).map(([key, value]) => ({
  label: value.label,
  value: key,
}));

export const defaultBasicForm: BasicFormParams = {
  formType: FormDesignKeyEnum.OPPORTUNITY_QUOTATION,
  name: '',
  executeTiming: ['CREATE'],
  description: '',
};

export const executionTimingList = [
  {
    value: 'CREATE',
    label: t('common.create'),
  },
  {
    value: 'EDIT',
    label: t('common.edit'),
  },
];

export const defaultMoreConfig: MoreSettingsParams = {
  submitterCanRevoke: true,
  allowBatchProcess: false,
  allowWithdraw: false,
  allowAddSign: false,
  duplicateApproverRule: 'FIRST_ONLY',
  requireComment: false,
  statusPermissions: [],
};

export const businessTypeOptions = [
  {
    label: t('crmFormCreate.drawer.quotation'),
    value: FormDesignKeyEnum.OPPORTUNITY_QUOTATION,
  },
  {
    label: t('module.contract'),
    value: FormDesignKeyEnum.CONTRACT,
  },
  {
    label: t('module.invoiceApproval'),
    value: FormDesignKeyEnum.INVOICE,
  },
  {
    label: t('module.order'),
    value: FormDesignKeyEnum.ORDER,
  },
];

export type ApprovalType = 'manual' | 'auto-approve' | 'auto-reject';

export const approvalTypeOptions: Array<{ label: string; value: ApprovalType }> = [
  {
    label: t('process.process.flow.manualApproval'),
    value: 'manual',
  },
  {
    label: t('process.process.flow.autoApprove'),
    value: 'auto-approve',
  },
  {
    label: t('process.process.flow.autoReject'),
    value: 'auto-reject',
  },
];

export function resolveApprovalActionNodeDefaults(approvalType: ApprovalType) {
  if (approvalType === 'auto-approve') {
    return {
      name: t('process.process.flow.approver'),
      description: t('process.process.flow.autoApprove'),
    };
  }

  if (approvalType === 'auto-reject') {
    return {
      name: t('process.process.flow.approver'),
      description: t('process.process.flow.autoReject'),
    };
  }

  return {
    name: t('process.process.flow.approver'),
    description: t('process.process.flow.selectApprover'),
  };
}
