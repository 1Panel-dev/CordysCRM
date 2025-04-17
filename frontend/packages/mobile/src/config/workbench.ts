import { useI18n } from '@lib/shared/hooks/useI18n';
import { RepeatClueItem, RepeatCustomerItem, RepeatOpportunityItem } from '@lib/shared/models/system/business';

import { clueBaseSteps } from '@/config/clue';
import { lastOpportunitySteps, opportunityResultSteps } from '@/config/opportunity';

const { t } = useI18n();

const statusOption = [
  { label: t('workbench.duplicateCheck.duplicate'), value: 'ALL' },
  { label: t('workbench.duplicateCheck.similar'), value: 'PART' },
];
export const customerDescriptionList = [
  {
    label: t('common.head'),
    key: 'ownerName',
  },
  {
    label: t('workbench.duplicateCheck.status'),
    key: 'repeatType',
    valueSlotName: 'render',
    render: (row: RepeatCustomerItem) => {
      const statusOptionItem = statusOption.find((e) => e.value === row.repeatType);
      return statusOptionItem ? statusOptionItem.label : '-';
    },
  },
  {
    label: t('workbench.duplicateCheck.relatedOpportunity'),
    key: 'opportunityCount',
    valueSlotName: 'count',
  },
  {
    label: t('workbench.duplicateCheck.relatedClue'),
    key: 'clueCount',
    valueSlotName: 'count',
  },
  {
    label: t('common.createTime'),
    key: 'createTime',
    valueSlotName: 'createTime',
  },
];

export const clueDescriptionList = [
  {
    label: t('workbench.duplicateCheck.clueStage'),
    key: 'stage',
    valueSlotName: 'render',
    render: (row: RepeatClueItem) => {
      const step = [...clueBaseSteps, ...opportunityResultSteps].find((e: any) => e.value === row.stage);
      return step ? step.label : '-';
    },
  },
  {
    label: t('common.head'),
    key: 'ownerName',
  },
];

export const opportunityDescriptionList = [
  {
    label: t('opportunity.name'),
    key: 'name',
  },
  {
    label: t('opportunity.intendedProducts'),
    key: 'productNames',
    valueSlotName: 'render',
    render: (row: RepeatOpportunityItem) => {
      return row.productNames.join('；');
    },
  },
  {
    label: t('opportunity.stage'),
    key: 'stage',
    valueSlotName: 'render',
    render: (row: RepeatOpportunityItem) => {
      const step = lastOpportunitySteps.find((e: any) => e.value === row.stage);
      return step ? step.label : '-';
    },
  },
  {
    label: t('common.head'),
    key: 'ownerName',
  },
];
