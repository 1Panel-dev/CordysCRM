import dayjs from 'dayjs';

import { OperatorEnum } from '@lib/shared/enums/commonEnum';
import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';
import { getSessionStorageTempState } from '@lib/shared/method/local-storage';

import { FilterFormItem, FilterResult } from '@/components/pure/crm-advance-filter/type';

const { t } = useI18n();

export const baseFilterConfigList: FilterFormItem[] = [
  {
    title: t('common.creator'),
    dataIndex: 'createUser',
    type: FieldTypeEnum.USER_SELECT,
  },
  {
    title: t('common.createTime'),
    dataIndex: 'createTime',
    type: FieldTypeEnum.TIME_RANGE_PICKER,
  },
  {
    title: t('common.updateUserName'),
    dataIndex: 'updateUser',
    type: FieldTypeEnum.USER_SELECT,
  },
  {
    title: t('common.updateTime'),
    dataIndex: 'updateTime',
    type: FieldTypeEnum.TIME_RANGE_PICKER,
  },
];

export const getLeadHomeConditions = (dim: string, homeDetailKey: string): FilterResult => {
  const depIds = getSessionStorageTempState<Record<string, string[]>>('homeData', true)?.[homeDetailKey];
  return {
    searchMode: 'AND',
    conditions: [
      {
        value: dim,
        operator: OperatorEnum.DYNAMICS,
        name: 'createTime',
        multipleValue: false,
        type: FieldTypeEnum.TIME_RANGE_PICKER,
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
