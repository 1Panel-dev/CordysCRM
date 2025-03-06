export enum CompanyTypeEnum {
  WECOM = 'WECOM', // 企业微信
  DINGTALK = 'DINGTALK', // 钉钉
  LARK = 'LARK', // 飞书
  INTERNAL = 'INTERNAL', // 国际飞书
}

// 操作符号
export enum OperatorEnum {
  GE = 'GE', // 大于等于
  LE = 'LE', // 小于等于

  LT = 'LT', // 小于
  GT = 'GT', // 大于
  IN = 'IN', // 在范围内
  NOT_IN = 'NOT_IN', // 不在范围内
  BETWEEN = 'BETWEEN', // 在两个值之间
  COUNT_GT = 'COUNT_GT', // 大于
  COUNT_LT = 'COUNT_LT', // 小于
  EQUALS = 'EQUALS', // 等于
  NOT_EQUALS = 'NOT_EQUALS', // 不等于
  CONTAINS = 'CONTAINS', // 包含
  NOT_CONTAINS = 'NOT_CONTAINS', // 不包含
  EMPTY = 'EMPTY', // 为空
  NOT_EMPTY = 'NOT_EMPTY', // 不为空

  DYNAMICS = 'DYNAMICS',
  FIXED = 'FIXED',
}
