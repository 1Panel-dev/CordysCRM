export enum FormDesignKeyEnum {
  CLUE = 'clue', // 线索
  FOLLOW_PLAN_CLUE = 'planClue', // 线索跟进计划
  FOLLOW_RECORD_CLUE = 'recordClue', // 线索跟进记录
  CUSTOMER = 'customer', // 客户
  CONTACT = 'contact', // 联系人
  FOLLOW_RECORD_CUSTOMER = 'recordCustomer', // 客户跟进记录
  FOLLOW_PLAN_CUSTOMER = 'planCustomer', // 客户跟进计划
  BUSINESS = 'business', // 商机
  FOLLOW_RECORD_BUSINESS = 'recordBusiness', // 商机跟进记录
  FOLLOW_PLAN_BUSINESS = 'planBusiness', // 商机跟进计划
  PRODUCT = 'product', // 产品
}

export enum FieldTypeEnum {
  TIME_RANGE_PICKER = 'TIME_RANGE_PICKER',
  INPUT = 'INPUT',
  TEXTAREA = 'TEXTAREA',
  INPUT_NUMBER = 'INPUT_NUMBER',
  DATE_TIME = 'DATE_TIME',
  RADIO = 'RADIO',
  CHECKBOX = 'CHECKBOX',
  SELECT = 'SELECT',
  USER_TAG_SELECTOR = 'USER_TAG_SELECTOR',
  MEMBER = 'MEMBER',
  DEPARTMENT = 'DEPARTMENT',
  DIVIDER = 'DIVIDER',
  MULTIPLE_INPUT = 'MULTIPLE_INPUT',
  // 高级字段
  PICTURE = 'PICTURE',
  LOCATION = 'LOCATION',
  PHONE = 'PHONE',
  DATA_SOURCE = 'DATA_SOURCE',
}

export enum FieldRuleEnum {
  REQUIRED = 'required',
  UNIQUE = 'unique',
  NUMBER_RANGE = 'numberRange',
}

export enum FieldDataSourceTypeEnum {
  CUSTOMER = 'CUSTOMER', // 客户
  CONTACT = 'CONTACT', // 联系人
  BUSINESS = 'BUSINESS', // 商机
  PRODUCT = 'PRODUCT', // 产品
  CLUE = 'CLUE', // 线索
}
