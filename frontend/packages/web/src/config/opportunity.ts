import { OpportunityStatusEnum, StageResultEnum } from '@lib/shared/enums/opportunityEnum';
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

export const lastOpportunitySteps = [...opportunityBaseSteps, ...opportunityResultSteps];
