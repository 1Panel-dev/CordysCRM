export enum OpportunitySearchTypeEnum {
  ALL = 'ALL',
  SELF = 'SELF',
  DEPARTMENT = 'DEPARTMENT',
  OPPORTUNITY_SUCCESS = 'OPPORTUNITY_SUCCESS',
}

export enum QuotationStatusEnum {
  SUCCESS = 'SUCCESS', // 通过
  FAIL = 'FAIL', // 未通过
  REVIEW = 'REVIEW', // 提审
  INVALID = 'INVALID', // 作废
  REVOKE = 'REVOKE', // 撤销
}

export default {};
