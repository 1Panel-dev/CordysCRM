import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';

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
