export enum TableKeyEnum {
  ROLE_MEMBER = 'roleMember',
  AUTH = 'auth',
  SYSTEM_ORG_TABLE = 'systemOrgTable',
  MODULE_OPPORTUNITY_RULE_TABLE = 'moduleOpportunityRuleTable',
  MODULE_CLUE_POOL = 'moduleCluePool',
  OPPORTUNITY_HEAD_LIST = 'opportunityHeadList',
  CUSTOMER = 'customer',
  CUSTOMER_CONTRACT = 'customerContract',
  CUSTOMER_FOLLOW_RECORD = 'customerFollowRecord',
  CUSTOMER_FOLLOW_PLAN = 'customerFollowPlan',
  CUSTOMER_COLLABORATOR = 'customerCollaborator',
  CUSTOMER_OPEN_SEA = 'customerOpenSea',
  CLUE = 'clue',
  CLUE_POOL = 'cluePool',
  PRODUCT = 'product',
  BUSINESS = 'business',
}

// 具有特殊功能的列
export enum SpecialColumnEnum {
  // 选择框
  SELECTION = 'selection',
  // 操作列
  OPERATION = 'operation',
}
