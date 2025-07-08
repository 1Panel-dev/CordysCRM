import { FailureReasonTypeEnum, OpportunityStatusEnum, StageResultEnum } from '@lib/shared/enums/opportunityEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';
import type { TransferParams } from '@lib/shared/models/customer/index';

const { t } = useI18n();

export const defaultTransferForm: TransferParams = {
  ids: [],
  owner: null,
};

export const opportunityBaseSteps = [
  {
    value: OpportunityStatusEnum.CREATE,
    label: t('common.newCreate'),
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
];

export const opportunityResultSteps = [
  {
    value: StageResultEnum.SUCCESS,
    label: t('common.success'),
  },
  {
    value: StageResultEnum.FAIL,
    label: t('common.fail'),
  },
];

export const failureReasonOptions = [
  { value: FailureReasonTypeEnum.COMPETITOR_CHOSEN, label: t('opportunity.customerChooseCompetitor') },
  { value: FailureReasonTypeEnum.PROJECT_FAILED, label: t('opportunity.projectFailed') },
  { value: FailureReasonTypeEnum.COMPLEX_DECISION_CHAIN, label: t('opportunity.complexDecisionChain') },
  { value: FailureReasonTypeEnum.BUDGET_LIMITATION, label: t('opportunity.budgetLimit') },
  { value: FailureReasonTypeEnum.REQUIREMENT_CHANGE, label: t('opportunity.requirementChange') },
];

export const lastOpportunitySteps = [...opportunityBaseSteps, ...opportunityResultSteps];
