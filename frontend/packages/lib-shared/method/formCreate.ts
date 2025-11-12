import { FieldTypeEnum, FormLinkScenarioEnum } from '../enums/formDesignEnum';
import type { FormCreateField } from '@cordys/web/src/components/business/crm-form-create/types';

export const linkAllAcceptTypes = [FieldTypeEnum.INPUT, FieldTypeEnum.TEXTAREA];
export const dataSourceTypes = [FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.DATA_SOURCE_MULTIPLE];
export const hiddenTypes = [FieldTypeEnum.DIVIDER, FieldTypeEnum.PICTURE];
export const needSameTypes = [
  FieldTypeEnum.PHONE,
  FieldTypeEnum.LOCATION,
  FieldTypeEnum.DATE_TIME,
  FieldTypeEnum.INPUT_NUMBER,
];
export const multipleTypes = [FieldTypeEnum.CHECKBOX, FieldTypeEnum.SELECT_MULTIPLE, FieldTypeEnum.INPUT_MULTIPLE];
export const memberTypes = [FieldTypeEnum.MEMBER, FieldTypeEnum.MEMBER_MULTIPLE];
export const departmentTypes = [FieldTypeEnum.DEPARTMENT, FieldTypeEnum.DEPARTMENT_MULTIPLE];
export const singleTypes = [FieldTypeEnum.RADIO, FieldTypeEnum.SELECT];

export function getRuleType(item: FormCreateField) {
  if (
    item.type === FieldTypeEnum.SELECT_MULTIPLE ||
    item.type === FieldTypeEnum.CHECKBOX ||
    item.type === FieldTypeEnum.INPUT_MULTIPLE ||
    item.type === FieldTypeEnum.MEMBER_MULTIPLE ||
    item.type === FieldTypeEnum.DEPARTMENT_MULTIPLE ||
    item.type === FieldTypeEnum.DATA_SOURCE ||
    item.type === FieldTypeEnum.DATA_SOURCE_MULTIPLE ||
    item.type === FieldTypeEnum.PICTURE ||
    item.type === FieldTypeEnum.ATTACHMENT
  ) {
    return 'array';
  }
  if (item.type === FieldTypeEnum.DATE_TIME) {
    return 'date';
  }
  if (item.type === FieldTypeEnum.INPUT_NUMBER) {
    return 'number';
  }
  return 'string';
}

export function getNormalFieldValue(item: FormCreateField, value: any) {
  if (item.type === FieldTypeEnum.DATA_SOURCE && !value) {
    return '';
  }
  if (
    [
      FieldTypeEnum.SELECT_MULTIPLE,
      FieldTypeEnum.MEMBER_MULTIPLE,
      FieldTypeEnum.DEPARTMENT_MULTIPLE,
      FieldTypeEnum.DATA_SOURCE_MULTIPLE,
      FieldTypeEnum.INPUT_MULTIPLE,
    ].includes(item.type) &&
    !value
  ) {
    return [];
  }
  if (item.type === FieldTypeEnum.INPUT_MULTIPLE && !value) {
    return [];
  }
  if (item.multiple && !value) {
    return [];
  }
  return value;
}

/**
 * 格式化数字
 * @param value 数字
 * @param item
 */
export function formatNumberValue(value: string | number, item: FormCreateField) {
  if (value !== undefined && value !== null && value !== '') {
    if (item.numberFormat === 'percent') {
      return item.precision ? `${Number(value).toFixed(item.precision)}%` : `${value}%`;
    }
    if (item.showThousandsSeparator) {
      return (item.precision ? Number(Number(value).toFixed(item.precision)) : Number(value)).toLocaleString('en-US');
    }
    return item.precision ? Number(value).toFixed(item.precision) : value.toString();
  }
  return '-';
}
