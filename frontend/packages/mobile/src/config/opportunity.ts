import { FailureReasonTypeEnum, OpportunityStatusEnum, StageResultEnum } from '@lib/shared/enums/opportunityEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';

import type { Options } from '@/components/business/crm-workflow-card/index.vue';

const { t } = useI18n();

export const baseStepList: Options[] = [
  {
    value: OpportunityStatusEnum.CREATE,
    label: t('opportunity.newCreate'),
  },
  {
    value: OpportunityStatusEnum.CLEAR_REQUIREMENTS,
    label: t('opportunity.clearRequirements'),
  },
  {
    value: OpportunityStatusEnum.SCHEME_VALIDATION,
    label: t('opportunity.schemeValidation'),
  },
  {
    value: OpportunityStatusEnum.PROJECT_PROPOSAL_REPORT,
    label: t('opportunity.projectProposalReport'),
  },
  {
    value: OpportunityStatusEnum.BUSINESS_PROCUREMENT,
    label: t('opportunity.businessProcurement'),
  },
  {
    value: OpportunityStatusEnum.END,
    label: t('opportunity.end'),
  },
];

export const opportunityResultSteps = [
  {
    value: StageResultEnum.SUCCESS,
    label: t('common.success'),
    bgColor: 'var(--success-5)',
    color: 'var(--success-green)',
  },
  {
    value: StageResultEnum.FAIL,
    label: t('common.fail'),
    bgColor: 'var(--error-5)',
    color: 'var(--error-red)',
  },
];

export const lastOpportunitySteps = [...baseStepList, ...opportunityResultSteps];

export const failureReasonOptions = [
  { value: FailureReasonTypeEnum.COMPETITOR_CHOSEN, text: t('opportunity.customerChooseCompetitor') },
  { value: FailureReasonTypeEnum.PROJECT_FAILED, text: t('opportunity.projectFailed') },
  { value: FailureReasonTypeEnum.COMPLEX_DECISION_CHAIN, text: t('opportunity.complexDecisionChain') },
  { value: FailureReasonTypeEnum.BUDGET_LIMITATION, text: t('opportunity.budgetLimit') },
  { value: FailureReasonTypeEnum.REQUIREMENT_CHANGE, text: t('opportunity.requirementChange') },
];

export default {};
