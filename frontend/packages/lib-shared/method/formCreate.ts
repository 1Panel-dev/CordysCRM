import { FieldTypeEnum } from '../enums/formDesignEnum';
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
    item.type === FieldTypeEnum.PICTURE
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
