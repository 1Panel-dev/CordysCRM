import { OperatorEnum } from '@lib/shared/enums/commonEnum';
import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';

import { useI18n } from '@/hooks/useI18n';

const { t } = useI18n();

export const EQUAL = { value: OperatorEnum.EQUALS, label: t('common.equal') }; // 等于
export const NOT_EQUAL = { value: OperatorEnum.NOT_EQUALS, label: t('common.notEqual') }; // 不等于
export const GT = { value: OperatorEnum.GT, label: t('common.gt') }; // 大于
export const GE = { value: OperatorEnum.GE, label: t('common.ge') }; // 大于等于
export const LT = { value: OperatorEnum.LT, label: t('common.lt') }; // 小于
export const LE = { value: OperatorEnum.LE, label: t('common.le') }; // 小于等于

export const DYNAMICS = { value: OperatorEnum.DYNAMICS, label: t('common.dynamics') }; // 动态
export const FIXED = { value: OperatorEnum.FIXED, label: t('common.fixed') }; // 固定

export const conditionsTypeOptions = [
  {
    value: 'Created',
    label: t('common.newCreate'),
  },
  {
    value: 'Picked',
    label: t('common.claim'),
    disabled: true,
  },
];

export const timeFormItem = [
  {
    path: 'operator',
    type: FieldTypeEnum.SELECT,
    rule: [
      {
        required: true,
        message: t('common.pleaseSelect'),
      },
    ],
    selectProps: {
      options: [DYNAMICS, FIXED],
    },
  },
  {
    path: 'value',
    type: FieldTypeEnum.TIME_RANGE_PICKER,
    formItemClass: 'w-[250px] flex-initial',
    rule: [
      {
        required: true,
        message: t('common.pleaseInput'),
      },
    ],
    crmTimeRangePickerProps: {
      typePath: 'operator',
    },
  },
  {
    path: 'scope',
    type: FieldTypeEnum.SELECT,
    defaultValue: ['Created', 'Picked'],
    rule: [
      {
        required: true,
        message: t('common.pleaseSelect'),
      },
    ],
    formItemClass: 'w-[160px] flex-initial',
    selectProps: {
      options: conditionsTypeOptions,
      multiple: true,
    },
  },
];
