export enum TableKeyEnum {
  ROLE_MEMBER = 'roleMember',
  AUTH = 'auth',
  SYSTEM_ORG_TABLE = 'systemOrgTable',
  SYSTEM_MESSAGE_TABLE = 'systemMessageTable',
  SYSTEM_ANNOUNCEMENT_TABLE = 'systemAnnouncementTable',
  MODULE_OPPORTUNITY_RULE_TABLE = 'moduleOpportunityRuleTable',
  MODULE_CLUE_POOL = 'moduleCluePool',
  MODULE_OPEN_SEA = 'moduleOpenSea',
  OPPORTUNITY_HEAD_LIST = 'opportunityHeadList',
  CUSTOMER = 'customer',
  CUSTOMER_CONTRACT = 'customerContract',
  BUSINESS_CONTRACT = 'businessContract',
  CUSTOMER_FOLLOW_RECORD = 'customerFollowRecord',
  CUSTOMER_FOLLOW_PLAN = 'customerFollowPlan',
  CUSTOMER_COLLABORATOR = 'customerCollaborator',
  CUSTOMER_OPEN_SEA = 'customerOpenSea',
  CLUE = 'clue',
  CLUE_CONVERT_CUSTOMER = 'clueConvertCustomer',
  CLUE_POOL = 'cluePool',
  PRODUCT = 'product',
  BUSINESS = 'business',
  LOG = 'log',
  LOGIN_LOG = 'loginLog',
  // 全局搜索
  SEARCH_GLOBAL_CLUE = 'searchGlobalClue', // 线索
  SEARCH_GLOBAL_CUSTOMER = 'searchGlobalCustomer', // 客户
  SEARCH_GLOBAL_CONTACT = 'searchGlobalContact', // 联系人
  SEARCH_GLOBAL_PUBLIC = 'searchGlobalPublic', // 公海
  SEARCH_GLOBAL_CLUE_POOL = 'searchGlobalCluePool', // 线索池
  SEARCH_GLOBAL_OPPORTUNITY = 'searchGlobalOpportunity', // 商机
}

// 具有特殊功能的列
export enum SpecialColumnEnum {
  // 选择框
  SELECTION = 'selection',
  // 操作列
  OPERATION = 'operation',
  // 拖拽列
  DRAG = 'drag',
  // 序号列
  ORDER = 'crmTableOrder',
}
