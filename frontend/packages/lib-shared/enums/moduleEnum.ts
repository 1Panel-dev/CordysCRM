export enum ModuleConfigEnum {
  /** 首页 */
  HOME = 'home',

  /** 客户管理 */
  CUSTOMER_MANAGEMENT = 'customer',

  /** 线索管理 */
  CLUE_MANAGEMENT = 'lead',

  /** 商机管理 */
  BUSINESS_MANAGEMENT = 'business',

  /** 数据管理 TODO 先不做 */
  // DATA_MANAGEMENT = 'data',

  /** 产品管理 */
  PRODUCT_MANAGEMENT = 'product',

  /** 系统设置 */
  SYSTEM_SETTINGS = 'setting',
}

// 添加员工API
export enum MemberApiTypeEnum {
  SYSTEM_ROLE = 'SYSTEM_ROLE',
  MODULE_ROLE = 'MODULE_ROLE',
}

// 选择添加
export enum MemberSelectTypeEnum {
  ORG = 'DEPARTMENT', // 组织架构
  ROLE = 'ROLE', // 角色
  MEMBER = 'USER', // 成员
}
