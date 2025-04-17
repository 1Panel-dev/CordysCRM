import { OpportunityStatusEnum, StageResultEnum } from '@lib/shared/enums/opportunityEnum';
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
  },
  {
    value: StageResultEnum.FAIL,
    label: t('common.fail'),
  },
];

export const lastOpportunitySteps = [...baseStepList, ...opportunityResultSteps];

export default {};
