import dayjs from 'dayjs';

import { OperatorEnum } from '@lib/shared/enums/commonEnum';
import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
import { getSessionStorageTempState } from '@lib/shared/method/local-storage';
import type { TransferParams } from '@lib/shared/models/customer/index';
import type { OpportunityStageConfig } from '@lib/shared/models/opportunity';

import { FilterResult } from '@/components/pure/crm-advance-filter/type';

import { getOpportunityStageConfig } from '@/api/modules';

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

  return {
    searchMode: 'AND',
    conditions: [
      {
        value: dim,
        operator: OperatorEnum.DYNAMICS,
        name: timeFiledKeyMap[timeField],
        multipleValue: false,
        type: FieldTypeEnum.TIME_RANGE_PICKER,
      },
      ...(isSuccess.value
        ? [
            {
              value: [successStage?.id],
              operator: OperatorEnum.IN,
              name: 'stage',
              multipleValue: true,
              type: FieldTypeEnum.SELECT_MULTIPLE,
            },
          ]
        : []),
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
