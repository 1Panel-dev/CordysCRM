import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { ProcessStatusEnum } from '@lib/shared/enums/process';
import { useI18n } from '@lib/shared/hooks/useI18n';
import { ProcessStatusType } from '@lib/shared/models/system/process';

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
  [ProcessStatusEnum.UNAPPROVED]: {
    label: t('common.rejected'),
    icon: 'iconicon_close_circle_filled',
    color: 'var(--error-red)',
    tagBgColor: 'var(--error-5)',
    tagColor: 'var(--error-red)',
  },
  [ProcessStatusEnum.APPROVING]: {
    label: t('common.reviewing'),
    icon: 'iconicon_wait',
    color: 'var(--info-blue)',
    tagBgColor: 'var(--warning-5)',
    tagColor: 'var(--warning-yellow)',
  },
  [ProcessStatusEnum.PENDING]: {
    label: t('common.pending'),
    icon: 'iconicon_minus_circle_filled1',
    color: 'var(--text-n4)',
    tagBgColor: '',
    tagColor: '',
  },
  [ProcessStatusEnum.REVOKED]: {
    label: t('common.revoked'),
    icon: 'iconicon_skip_planarity',
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

export const defaultBasicForm = {
  businessType: FormDesignKeyEnum.OPPORTUNITY_QUOTATION,
  name: '',
  executionTiming: ['CREATE'],
  description: '',
};

export const defaultMoreConfig = {
  submitterAuthority: true,
  approverAuthority: [],
  autoApproval: 'firstNodeApproval',
  approvalOpinion: false,
};
