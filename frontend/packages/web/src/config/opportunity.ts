import dayjs from 'dayjs';

import { OperatorEnum } from '@lib/shared/enums/commonEnum';
import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';
import { getSessionStorageTempState } from '@lib/shared/method/local-storage';
import type { TransferParams } from '@lib/shared/models/customer/index';
import type { OpportunityStageConfig } from '@lib/shared/models/opportunity';

import { FilterResult } from '@/components/pure/crm-advance-filter/type';

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
  let start;
  let end;
  if (dim === 'YEAR') {
    start = dayjs().startOf('year').valueOf();
    end = dayjs().endOf('year').valueOf();
  }
  const depIds = getSessionStorageTempState<Record<string, string[]>>('homeData', true)?.[homeDetailKey];
  const stageConfig = ref<OpportunityStageConfig>();
  async function initStageConfig() {
    try {
      stageConfig.value = await getOpportunityStageConfig();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }
  const timeFieldKey = timeField === 'CREATE_TIME' ? 'createTime' : 'expectedEndTime';
  await initStageConfig();
  const successStage = stageConfig.value?.stageConfigList?.find((i) => i.type === 'END' && i.rate === '100');
  const isSuccess = computed(() => status === successStage?.id);

  return {
    searchMode: 'AND',
    conditions: [
      {
        value: dim !== 'YEAR' ? dim : [start, end],
        operator: dim !== 'YEAR' ? OperatorEnum.DYNAMICS : OperatorEnum.BETWEEN,
        name: isSuccess.value ? 'expectedEndTime' : timeFieldKey,
        multipleValue: false,
        type: FieldTypeEnum.TIME_RANGE_PICKER,
      },
      {
        value: isSuccess.value
          ? [successStage?.id]
          : stageConfig.value?.stageConfigList?.filter((i) => i.type === 'END').map((i) => i.id),
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
