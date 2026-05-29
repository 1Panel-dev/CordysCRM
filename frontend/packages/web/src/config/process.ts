import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { ApprovalTypeEnum, ApproverTypeEnum, ProcessStatusEnum } from '@lib/shared/enums/process';
import { useI18n } from '@lib/shared/hooks/useI18n';
import { BasicFormParams, MoreSettingsParams } from '@lib/shared/models/system/process';

import { StatusInfo } from '@/components/business/crm-approval/components/crm-approval-status.vue';

const { t } = useI18n();

// 审批资源状态
export const processStatusMap: Record<ProcessStatusEnum, StatusInfo> = {
  [ProcessStatusEnum.APPROVED]: {
    label: t('common.approved'),
    icon: 'iconicon_succeed_filled',
    color: 'var(--success-green)',
    tagBgColor: 'var(--success-5)',
    tagColor: 'var(--success-green)',
  },
  [ProcessStatusEnum.AUTO_APPROVED]: {
    label: t('common.autoApproved'),
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
  [ProcessStatusEnum.AUTO_UNAPPROVED]: {
    label: t('common.autoRejected'),
    icon: 'iconicon_close_circle_filled',
    color: 'var(--error-red)',
    tagBgColor: 'var(--error-5)',
    tagColor: 'var(--error-red)',
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

// 审批记录状态
export const approvalRecordStatusMap: Record<ProcessStatusEnum, StatusInfo> = {
  [ProcessStatusEnum.APPROVED]: {
    label: t('common.approved'),
    icon: 'iconicon_succeed_filled',
    color: 'var(--success-green)',
    tagBgColor: 'var(--success-5)',
    tagColor: 'var(--success-green)',
  },
  [ProcessStatusEnum.AUTO_APPROVED]: {
    label: t('common.autoApproved'),
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
  [ProcessStatusEnum.AUTO_UNAPPROVED]: {
    label: t('common.autoRejected'),
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
    label: t('taskDrawer.result.PENDING'),
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

export const processStatusOptions = Object.entries(processStatusMap)
  .filter(
    ([key]) => ![ProcessStatusEnum.AUTO_APPROVED, ProcessStatusEnum.AUTO_UNAPPROVED].includes(key as ProcessStatusEnum)
  )
  .map(([key, value]) => ({
    label: value.label,
    value: key,
  }));

export const defaultBasicForm: BasicFormParams = {
  formType: FormDesignKeyEnum.OPPORTUNITY_QUOTATION,
  name: '',
  createExecute: true,
  updateExecute: false,
  description: '',
};

export const executionTimingList: {
  label: string;
  value: 'createExecute' | 'updateExecute';
}[] = [
  {
    value: 'createExecute',
    label: t('common.create'),
  },
  {
    value: 'updateExecute',
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
  permissions: [],
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

const approvalTypeOptionConfigs: Array<{
  label: string;
  value: ApprovalTypeEnum;
  icon: string;
  iconBgClass: string;
}> = [
  {
    label: t('process.process.flow.manualApproval'),
    value: ApprovalTypeEnum.MANUAL,
    icon: 'iconicon_contract',
    iconBgClass: 'bg-[var(--warning-yellow)]',
  },
  {
    label: t('process.process.flow.autoApprove'),
    value: ApprovalTypeEnum.AUTO_PASS,
    icon: 'iconicon_contract',
    iconBgClass: 'bg-[var(--warning-yellow)]',
  },
  {
    label: t('process.process.flow.autoReject'),
    value: ApprovalTypeEnum.AUTO_REJECT,
    icon: 'iconicon_contract',
    iconBgClass: 'bg-[var(--warning-yellow)]',
  },
];

export const approvalTypeOptions: Array<{ label: string; value: ApprovalTypeEnum }> = approvalTypeOptionConfigs.map(
  ({ label, value }) => ({
    label,
    value,
  })
);

export type FlowAddNodeType = 'action' | 'condition-group';

export interface FlowAddNodeOption {
  label: string;
  type: FlowAddNodeType;
  icon: string;
  iconBgClass: string;
  actionApprovalType?: ApprovalTypeEnum;
}

export interface FlowAddNodeGroup {
  key: string;
  title: string;
  options: FlowAddNodeOption[];
}

export const approvalFlowAddNodeGroups: FlowAddNodeGroup[] = [
  {
    key: 'approval',
    title: t('process.process.flow.approver'),
    options: approvalTypeOptionConfigs.map(({ label, value, icon, iconBgClass }) => ({
      label,
      type: 'action',
      actionApprovalType: value,
      icon,
      iconBgClass,
    })),
  },
  {
    key: 'condition',
    title: t('crmFlow.triggerCondition'),
    options: [
      {
        label: t('process.process.flow.conditionRule'),
        type: 'condition-group',
        icon: 'iconicon_fork',
        iconBgClass: 'bg-[var(--info-blue)]',
      },
    ],
  },
];

export const approverTypeOptions: Array<{ label: string; value: ApproverTypeEnum }> = [
  {
    label: t('process.process.flow.approverType.specifiedMember'),
    value: ApproverTypeEnum.SPECIFIED_MEMBER,
  },
  {
    label: t('process.process.flow.approverType.specifiedSupervisor'),
    value: ApproverTypeEnum.DIRECT_SUPERVISOR,
  },
  {
    label: t('process.process.flow.approverType.continuousSupervisor'),
    value: ApproverTypeEnum.CONTINUOUS_SUPERVISOR,
  },
  {
    label: t('process.process.flow.approverType.specifiedDepartmentLeader'),
    value: ApproverTypeEnum.SPECIFIED_DEPARTMENT_LEADER,
  },
  {
    label: t('process.process.flow.approverType.continuousDepartmentLeader'),
    value: ApproverTypeEnum.CONTINUOUS_DEPARTMENT_LEADER,
  },
  {
    label: t('role.role'),
    value: ApproverTypeEnum.ROLE,
  },
];

export function resolveApprovalActionNodeDescription(
  approvalType: ApprovalTypeEnum,
  approverType?: ApproverTypeEnum | null
) {
  if (approvalType === ApprovalTypeEnum.AUTO_PASS) {
    return t('process.process.flow.autoApprove');
  }

  if (approvalType === ApprovalTypeEnum.AUTO_REJECT) {
    return t('process.process.flow.autoReject');
  }

  return approverType
    ? approverTypeOptions.find((item) => item.value === approverType)?.label ?? t('process.process.flow.selectApprover')
    : t('process.process.flow.selectApprover');
}

export function resolveApprovalActionNodeDefaults(approvalType: ApprovalTypeEnum) {
  return {
    name: t('process.process.flow.approver'),
    description: resolveApprovalActionNodeDescription(approvalType),
  };
}

export const approverLevelOptions = [
  {
    label: t('org.directSuperior'),
    value: '1',
  },
  {
    label: t('process.process.flow.approverLevel.second'),
    value: '2',
  },
  {
    label: t('process.process.flow.approverLevel.third'),
    value: '3',
  },
  {
    label: t('process.process.flow.approverLevel.fourth'),
    value: '4',
  },
  {
    label: t('process.process.flow.approverLevel.fifth'),
    value: '5',
  },
  {
    label: t('process.process.flow.approverLevel.sixth'),
    value: '6',
  },
  {
    label: t('process.process.flow.approverLevel.seventh'),
    value: '7',
  },
  {
    label: t('process.process.flow.approverLevel.eighth'),
    value: '8',
  },
  {
    label: t('process.process.flow.approverLevel.ninth'),
    value: '9',
  },
  {
    label: t('process.process.flow.approverLevel.tenth'),
    value: '10',
  },
];

export const continuousApproverLevelOptions = [
  {
    ...approverLevelOptions[0],
    label: t('process.process.flow.approverLevel.first'),
  },
  ...approverLevelOptions.slice(1),
];

export const departmentLevelOptions = [
  {
    label: t('process.process.flow.departmentLevel.direct'),
    value: '1',
  },
  {
    label: t('process.process.flow.departmentLevel.second'),
    value: '2',
  },
  {
    label: t('process.process.flow.departmentLevel.third'),
    value: '3',
  },
  {
    label: t('process.process.flow.departmentLevel.fourth'),
    value: '4',
  },
  {
    label: t('process.process.flow.departmentLevel.fifth'),
    value: '5',
  },
  {
    label: t('process.process.flow.departmentLevel.sixth'),
    value: '6',
  },
  {
    label: t('process.process.flow.departmentLevel.seventh'),
    value: '7',
  },
  {
    label: t('process.process.flow.departmentLevel.eighth'),
    value: '8',
  },
  {
    label: t('process.process.flow.departmentLevel.ninth'),
    value: '9',
  },
  {
    label: t('process.process.flow.departmentLevel.tenth'),
    value: '10',
  },
];

export const continuousDepartmentLevelOptions = [
  {
    ...departmentLevelOptions[0],
    label: t('process.process.flow.departmentLevel.first'),
  },
  ...departmentLevelOptions.slice(1),
];
