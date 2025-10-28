import { OperatorEnum } from '@lib/shared/enums/commonEnum';
import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';

import { operatorOptionsMap } from './index';
import { FilterFormItem } from './type';

// 第二列默认：包含/属于/等于
export function getDefaultOperator(list: string[]) {
  if (list.includes(OperatorEnum.CONTAINS)) {
    return OperatorEnum.CONTAINS;
  }
  if (list.includes(OperatorEnum.DYNAMICS)) {
    return OperatorEnum.DYNAMICS;
  }
  if (list.includes(OperatorEnum.IN)) {
    return OperatorEnum.IN;
  }
  if (list.includes(OperatorEnum.EQUALS)) {
    return OperatorEnum.EQUALS;
  }
  return OperatorEnum.BETWEEN;
}

export function getOperator(type: FieldTypeEnum, value?: string | null) {
  if (!value) return OperatorEnum.EMPTY;
  const options = operatorOptionsMap[type] || [];
  const optionsValueList = options.map((optionItem: { value: string; label: string }) => optionItem.value);
  return getDefaultOperator(optionsValueList);
}

export function valueIsArray(listItem: FilterFormItem) {
  return (
    [
      FieldTypeEnum.SELECT_MULTIPLE,
      FieldTypeEnum.DEPARTMENT_MULTIPLE,
      FieldTypeEnum.MEMBER_MULTIPLE,
      FieldTypeEnum.DATA_SOURCE_MULTIPLE,
      FieldTypeEnum.DATA_SOURCE,
    ].includes(listItem.type) ||
    [
      FieldTypeEnum.DEPARTMENT,
      FieldTypeEnum.DEPARTMENT_MULTIPLE,
      FieldTypeEnum.MEMBER,
      FieldTypeEnum.MEMBER_MULTIPLE,
      FieldTypeEnum.SELECT,
      FieldTypeEnum.SELECT_MULTIPLE,
      FieldTypeEnum.RADIO,
      FieldTypeEnum.CHECKBOX,
    ].includes(listItem.type) ||
    listItem.selectProps?.multiple ||
    listItem.cascaderProps?.multiple ||
    listItem.type === FieldTypeEnum.USER_SELECT ||
    (listItem.type === FieldTypeEnum.INPUT_MULTIPLE &&
      ![OperatorEnum.COUNT_LT, OperatorEnum.COUNT_GT].includes(listItem.operator as OperatorEnum)) ||
    (listItem.type === FieldTypeEnum.DATE_TIME && listItem.operator === OperatorEnum.BETWEEN)
  );
}
