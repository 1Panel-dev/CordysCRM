export enum ModuleConfigEnum {
  /** 首页 */
  HOME = 'home',

  /** 客户管理 */
  CUSTOMER_MANAGEMENT = 'customer',

  /** 线索管理 */
  CLUE_MANAGEMENT = 'clue',

  /** 商机管理 */
  BUSINESS_MANAGEMENT = 'business',

  /** 数据管理 TODO 先不做 */
  // DATA_MANAGEMENT = 'data',

  /** 产品管理 */
  PRODUCT_MANAGEMENT = 'product',

  /** 系统设置 */
  SYSTEM_SETTINGS = 'setting',

  /** 仪表板 */
  DASHBOARD = 'dashboard',

  /** 智能体 */
  AGENT = 'agent',
}

// 添加员工API
export enum MemberApiTypeEnum {
  SYSTEM_ROLE = 'SYSTEM_ROLE',
  MODULE_ROLE = 'MODULE_ROLE',
  FORM_FIELD = 'FORM_FIELD',
  SYSTEM_ORG_USER = 'SYSTEM_ORG_USER',
}

// 选择添加
export enum MemberSelectTypeEnum {
  ORG = 'DEPARTMENT', // 组织架构
  ROLE = 'ROLE', // 角色
  MEMBER = 'USER', // 成员
  ONLY_ORG = 'ONLY_DEPARTMENT', // 组织架构
}

// 原因类型
export enum ReasonTypeEnum {
  OPPORTUNITY_FAIL_RS = 'OPPORTUNITY_FAIL_RS', // 商机失败原因
  CUSTOMER_POOL_RS = 'CUSTOMER_POOL_RS', //  公海原因
  CLUE_POOL_RS = 'CLUE_POOL_RS', // 线索移入原因
}
