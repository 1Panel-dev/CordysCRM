import dayjs from 'dayjs';

import { OperatorEnum } from '@lib/shared/enums/commonEnum';
import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
import { FailureReasonTypeEnum, OpportunityStatusEnum, StageResultEnum } from '@lib/shared/enums/opportunityEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';
import { getSessionStorageTempState } from '@lib/shared/method/local-storage';
import type { TransferParams } from '@lib/shared/models/customer/index';

import { FilterResult } from '@/components/pure/crm-advance-filter/type';

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

export const getOptHomeConditions = (
  dim: string,
  status: string,
  timeField: string,
  homeDetailKey: string
): FilterResult => {
  let start;
  let end;
  if (dim === 'YEAR') {
    start = dayjs().startOf('year').valueOf();
    end = dayjs().endOf('year').valueOf();
  }
  const depIds = getSessionStorageTempState<Record<string, string[]>>('homeData', true)?.[homeDetailKey];

  const timeFieldKey = timeField === 'CREATE_TIME' ? 'createTime' : 'expectedEndTime';
  return {
    searchMode: 'AND',
    conditions: [
      {
        value: dim !== 'YEAR' ? dim : [start, end],
        operator: dim !== 'YEAR' ? OperatorEnum.DYNAMICS : OperatorEnum.BETWEEN,
        name: status === 'SUCCESS' ? 'expectedEndTime' : timeFieldKey,
        multipleValue: false,
        type: FieldTypeEnum.TIME_RANGE_PICKER,
      },
      {
        value:
          status === 'SUCCESS'
            ? [StageResultEnum.SUCCESS]
            : [
                OpportunityStatusEnum.CREATE,
                OpportunityStatusEnum.CLEAR_REQUIREMENTS,
                OpportunityStatusEnum.SCHEME_VALIDATION,
                OpportunityStatusEnum.PROJECT_PROPOSAL_REPORT,
                OpportunityStatusEnum.BUSINESS_PROCUREMENT,
              ],
        operator: OperatorEnum.IN,
        name: 'stage',
        multipleValue: true,
        type: FieldTypeEnum.SELECT_MULTIPLE,
      },
      ...(depIds?.length
        ? [
            {
              value: depIds,
              operator: OperatorEnum.IN,
              name: 'departmentId',
              multipleValue: false,
              type: FieldTypeEnum.TREE_SELECT,
            },
          ]
        : []),
    ],
  };
};
