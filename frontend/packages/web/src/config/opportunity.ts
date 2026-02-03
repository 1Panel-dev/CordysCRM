import dayjs from 'dayjs';

import { OperatorEnum } from '@lib/shared/enums/commonEnum';
import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
import { QuotationStatusEnum } from '@lib/shared/enums/opportunityEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';
import { getSessionStorageTempState } from '@lib/shared/method/local-storage';
import type { TransferParams } from '@lib/shared/models/customer/index';
import type { OpportunityStageConfig } from '@lib/shared/models/opportunity';

import { ConditionsItem, FilterResult } from '@/components/pure/crm-advance-filter/type';

import { getOpportunityStageConfig } from '@/api/modules';

const { t } = useI18n();

export const defaultTransferForm: TransferParams = {
  ids: [],
  owner: null,
};

export const getOptHomeConditions = async (
  dim: string,
  status: string,
  timeField: string,
  homeDetailKey: string
): Promise<FilterResult> => {
  const depIds = getSessionStorageTempState<Record<string, string[]>>('homeData', true)?.[homeDetailKey];
  const stageConfig = ref<OpportunityStageConfig>();

  const timeFiledKeyMap: Record<string, string> = {
    CREATE_TIME: 'createTime',
    EXPECTED_END_TIME: 'expectedEndTime',
    ACTUAL_END_TIME: 'actualEndTime',
  };
  async function initStageConfig() {
    try {
      stageConfig.value = await getOpportunityStageConfig();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  await initStageConfig();
  const successStage = stageConfig.value?.stageConfigList?.find((i) => i.type === 'END' && i.rate === '100');
  const isSuccess = computed(() => status === successStage?.id);
  const afootStageValues = stageConfig.value?.stageConfigList?.filter((i) => i.type === 'AFOOT').map((i) => i.id) || [];
  const isAfoot = computed(() => status === 'AFOOT');
  // 构建过滤条件
  const conditions: ConditionsItem[] = [
    {
      value: dim,
      operator: OperatorEnum.DYNAMICS,
      name: timeFiledKeyMap[timeField],
      multipleValue: false,
      type: FieldTypeEnum.TIME_RANGE_PICKER,
    },
  ];

  // 添加阶段过滤条件
  if (isSuccess.value) {
    // 成功阶段
    conditions.push({
      value: [successStage?.id],
      operator: OperatorEnum.IN,
      name: 'stage',
      multipleValue: true,
      type: FieldTypeEnum.SELECT_MULTIPLE,
    });
  } else if (isAfoot.value && afootStageValues.length > 0) {
    // 进行中阶段
    conditions.push({
      value: afootStageValues,
      operator: OperatorEnum.IN,
      name: 'stage',
      multipleValue: true,
      type: FieldTypeEnum.SELECT_MULTIPLE,
    });
  }

  // 添加部门过滤条件
  if (depIds?.length) {
    conditions.push({
      value: depIds,
      operator: OperatorEnum.IN,
      name: 'departmentId',
      multipleValue: false,
      type: FieldTypeEnum.TREE_SELECT,
    });
  }

  return {
    searchMode: 'AND',
    conditions,
  };
};

export const quotationStatusOptions = [
  {
    value: QuotationStatusEnum.APPROVED,
    label: t('common.pass'),
  },
  {
    value: QuotationStatusEnum.UNAPPROVED,
    label: t('common.unPass'),
  },
  {
    value: QuotationStatusEnum.APPROVING,
    label: t('common.review'),
  },
  {
    value: QuotationStatusEnum.VOIDED,
    label: t('common.voided'),
  },
  {
    value: QuotationStatusEnum.REVOKED,
    label: t('common.revoke'),
  },
];
