export enum FieldTypeEnum {
  INPUT = 'input',
  TEXTAREA = 'textarea',
  INPUT_NUMBER = 'inputNumber',
  DATE_TIME = 'dateTime',
  RADIO = 'radio',
  CHECKBOX = 'checkbox',
  SELECT = 'select',
  USER_TAG_SELECTOR = 'userTagSelector',
  SELECT_SINGLE = 'selectSingle',
  SELECT_MULTIPLE = 'selectMultiple',
  MEMBER_SINGLE = 'memberSingle',
  MEMBER_MULTIPLE = 'memberMultiple',
  DEPARTMENT_SINGLE = 'departmentSingle',
  DEPARTMENT_MULTIPLE = 'departmentMultiple',
  DIVIDER = 'divider',
  // 高级字段
  PICTURE = 'picture',
  LOCATION = 'location',
  PHONE = 'phone',
}

export enum FieldRuleEnum {
  REQUIRED = 'required',
  UNIQUE = 'unique',
  NUMBER_RANGE = 'numberRange',
}
