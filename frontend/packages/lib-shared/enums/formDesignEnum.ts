export enum FormDesignKeyEnum {
  CLUE = 'clue', // 线索
  CLUE_TRANSITION_CUSTOMER = 'clueTransitionCustomer', // 转为客户
  CLUE_POOL = 'cluePool', // 线索池
  FOLLOW_PLAN_CLUE = 'planClue', // 线索跟进计划
  FOLLOW_RECORD_CLUE = 'recordClue', // 线索跟进记录
  CUSTOMER = 'customer', // 客户
  CUSTOMER_OPEN_SEA = 'customerOpenSea', // 公海客户
  CONTACT = 'contact', // 联系人
  CUSTOMER_CONTACT = 'customerContact', // 客户下的联系人
  FOLLOW_RECORD_CUSTOMER = 'record', // 客户跟进记录
  FOLLOW_PLAN_CUSTOMER = 'plan', // 客户跟进计划
  BUSINESS = 'opportunity', // 商机
  FOLLOW_RECORD_BUSINESS = 'recordBusiness', // 商机跟进记录
  FOLLOW_PLAN_BUSINESS = 'planBusiness', // 商机跟进计划
  PRODUCT = 'product', // 产品
  BUSINESS_CONTACT = 'opportunityContact', // 商机联系人
  CUSTOMER_OPPORTUNITY = 'customerOpportunity', // 客户商机
  FOLLOW_PLAN = 'followPlan',
  FOLLOW_RECORD = 'followRecord',
  // 全局搜索
  SEARCH_ADVANCED_CLUE = 'searchAdvancedClue', // 线索
  SEARCH_ADVANCED_CUSTOMER = 'searchAdvancedCustomer', // 客户
  SEARCH_ADVANCED_CONTACT = 'searchAdvancedContact', // 联系人
  SEARCH_ADVANCED_PUBLIC = 'searchAdvancedPublic', // 公海
  SEARCH_ADVANCED_CLUE_POOL = 'searchAdvancedCluePool', // 线索池
  SEARCH_ADVANCED_OPPORTUNITY = 'searchAdvancedOpportunity', // 商机
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
  SELECT_MULTIPLE = 'SELECT_MULTIPLE',
  USER_TAG_SELECTOR = 'USER_TAG_SELECTOR',
  MEMBER = 'MEMBER',
  MEMBER_MULTIPLE = 'MEMBER_MULTIPLE',
  DEPARTMENT = 'DEPARTMENT',
  DEPARTMENT_MULTIPLE = 'DEPARTMENT_MULTIPLE',
  DIVIDER = 'DIVIDER',
  INPUT_MULTIPLE = 'INPUT_MULTIPLE',
  TREE_SELECT = 'TREE_SELECT',
  USER_SELECT = 'USER_SELECT',
  // 高级字段
  PICTURE = 'PICTURE',
  LOCATION = 'LOCATION',
  PHONE = 'PHONE',
  DATA_SOURCE = 'DATA_SOURCE',
  DATA_SOURCE_MULTIPLE = 'DATA_SOURCE_MULTIPLE',
  SERIAL_NUMBER = 'SERIAL_NUMBER', // 流水号
  LINK = 'LINK', // 链接
  ATTACHMENT = 'ATTACHMENT',
  INDUSTRY = 'INDUSTRY',
}

export enum FieldRuleEnum {
  REQUIRED = 'required',
  UNIQUE = 'unique',
  NUMBER_RANGE = 'numberRange',
}

export enum FieldDataSourceTypeEnum {
  CUSTOMER = 'CUSTOMER', // 客户
  CONTACT = 'CONTACT', // 联系人
  BUSINESS = 'OPPORTUNITY', // 商机
  PRODUCT = 'PRODUCT', // 产品
  CLUE = 'CLUE', // 线索
  CUSTOMER_OPTIONS = 'CUSTOMER_OPTIONS', // 客户选项
  USER_OPTIONS = 'USER_OPTIONS', // 成员选项
}

export enum FormLinkScenarioEnum {
  CLUE_TO_CUSTOMER = 'CLUE_TO_CUSTOMER', // 线索转客户
  CLUE_TO_OPPORTUNITY = 'CLUE_TO_OPPORTUNITY', // 线索转商机
  CUSTOMER_TO_OPPORTUNITY = 'CUSTOMER_TO_OPPORTUNITY', // 客户转商机
  CLUE_TO_RECORD = 'CLUE_TO_RECORD', // 线索转跟进记录
  CUSTOMER_TO_RECORD = 'CUSTOMER_TO_RECORD', // 客户转跟进记录
  OPPORTUNITY_TO_RECORD = 'OPPORTUNITY_TO_RECORD', // 商机转跟进记录
  PLAN_TO_RECORD = 'PLAN_TO_RECORD', // 跟进计划转跟进记录
}
