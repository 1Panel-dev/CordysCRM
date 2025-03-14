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
