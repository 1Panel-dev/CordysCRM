import { OperatorEnum } from '@lib/shared/enums/commonEnum';
import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';

import { useI18n } from '@/hooks/useI18n';

const { t } = useI18n();

// 大于等于
export const GE = { label: 'advanceFilter.operator.ge', value: OperatorEnum.GE };
// 在范围内
export const IN = { label: 'advanceFilter.operator.belongTo', value: OperatorEnum.IN };
// 不在范围内
export const NOT_IN = { label: 'advanceFilter.operator.notBelongTo', value: OperatorEnum.NOT_IN };
// 大于
export const GT = { label: 'advanceFilter.operator.gt', value: OperatorEnum.GT };
// 小于
export const LT = { label: 'advanceFilter.operator.lt', value: OperatorEnum.LT };
// 小于等于
export const LE = { label: 'advanceFilter.operator.le', value: OperatorEnum.LE };
// 等于
export const EQUAL = { label: 'advanceFilter.operator.equal', value: OperatorEnum.EQUALS };
// 不等于
export const NOT_EQUAL = { label: 'advanceFilter.operator.notEqual', value: OperatorEnum.NOT_EQUALS };
// 介于
export const BETWEEN = { label: 'advanceFilter.operator.between', value: OperatorEnum.BETWEEN };
// 数量大下
export const COUNT_GT = { label: 'advanceFilter.operator.count.gt', value: OperatorEnum.COUNT_GT };
// 数量小于
export const COUNT_LT = { label: 'advanceFilter.operator.count.lt', value: OperatorEnum.COUNT_LT };
// 为空
export const EMPTY = { label: 'advanceFilter.operator.empty', value: OperatorEnum.EMPTY };
// 不为空
export const NOT_EMPTY = { label: 'advanceFilter.operator.not_empty', value: OperatorEnum.NOT_EMPTY };
// 包含
export const CONTAINS = { label: 'advanceFilter.operator.contains', value: OperatorEnum.CONTAINS };
// 不包含
export const NO_CONTAINS = { label: 'advanceFilter.operator.not_contains', value: OperatorEnum.NOT_CONTAINS };
// 动态
export const DYNAMICS = { value: OperatorEnum.DYNAMICS, label: 'common.dynamics' };
// 固定
export const FIXED = { value: OperatorEnum.FIXED, label: 'common.fixed' };
// 通用文本符号
const COMMON_TEXT_OPERATORS = [CONTAINS, NO_CONTAINS, EMPTY, NOT_EMPTY, EQUAL, NOT_EQUAL];
// 通用选择符号
const COMMON_SELECTION_OPERATORS = [IN, NOT_IN, EMPTY, NOT_EMPTY];

export const operatorOptionsMap: Record<string, { value: string; label: string }[]> = {
  [FieldTypeEnum.INPUT]: COMMON_TEXT_OPERATORS,
  [FieldTypeEnum.TEXTAREA]: COMMON_TEXT_OPERATORS,
  [FieldTypeEnum.INPUT_NUMBER]: [EQUAL, GT, LT],
  [FieldTypeEnum.SELECT]: COMMON_SELECTION_OPERATORS,
  [FieldTypeEnum.RADIO]: COMMON_SELECTION_OPERATORS,
  [FieldTypeEnum.CHECKBOX]: COMMON_SELECTION_OPERATORS,
  [FieldTypeEnum.MEMBER]: COMMON_SELECTION_OPERATORS,
  [FieldTypeEnum.USER_SELECT]: COMMON_SELECTION_OPERATORS,
  [FieldTypeEnum.MULTIPLE_INPUT]: [EMPTY, CONTAINS, NO_CONTAINS, COUNT_LT, COUNT_GT],
  [FieldTypeEnum.DATA_SOURCE]: COMMON_TEXT_OPERATORS,
  [FieldTypeEnum.TREE_SELECT]: [IN, NOT_IN],
  [FieldTypeEnum.DATE_TIME]: [BETWEEN, GT, LT, EMPTY, NOT_EMPTY],
  [FieldTypeEnum.TIME_RANGE_PICKER]: [DYNAMICS, FIXED],
};

export const scopeOptions = [
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
