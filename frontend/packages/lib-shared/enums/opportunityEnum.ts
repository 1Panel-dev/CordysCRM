export enum OpportunityStatusEnum {
  /** 新建 */
  CREATE = 'CREATE',

  /** 需求明确 */
  CLEAR_REQUIREMENTS = 'CLEAR_REQUIREMENTS',

  /** 方案验证 */
  SCHEME_VALIDATION = 'SCHEME_VALIDATION',

  /** 项目汇报 */
  PROJECT_PROPOSAL_REPORT = 'PROJECT_PROPOSAL_REPORT',

  /** 商务采购 */
  BUSINESS_PROCUREMENT = 'BUSINESS_PROCUREMENT',

  /** 完结 */
  END = 'END',
}

export enum StageResultEnum {
  /** 成功 */
  SUCCESS = 'SUCCESS',

  /** 失败 */
  FAIL = 'FAIL',
}

export enum OpportunitySearchTypeEnum {
  ALL = 'ALL',
  SELF = 'SELF',
  DEPARTMENT = 'DEPARTMENT',
  DEAL = 'DEAL',
}

export enum FailureReasonTypeEnum {
  COMPETITOR_CHOSEN = 'COMPETITOR_CHOSEN', // 客户选择竞品
  PROJECT_FAILED = 'PROJECT_FAILED', // 立项失败
  COMPLEX_DECISION_CHAIN = 'COMPLEX_DECISION_CHAIN', // 决策链复杂
  BUDGET_LIMITATION = 'BUDGET_LIMITATION', // 预算限制
  REQUIREMENT_CHANGE = 'REQUIREMENT_CHANGE', // 需求变化
}
