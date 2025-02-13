export enum ModuleConfigEnum {
  /** 首页 */
  HOME = 'home',

  /** 客户管理 */
  CUSTOMER_MANAGEMENT = 'customer',

  /** 线索管理 */
  CLUE_MANAGEMENT = 'clue',

  /** 商机管理 */
  BUSINESS_MANAGEMENT = 'businessManage',

  /** 数据管理 */
  DATA_MANAGEMENT = 'data',

  /** 产品管理 */
  PRODUCT_MANAGEMENT = 'product',

  /** 系统设置 */
  SYSTEM_SETTINGS = 'systemSettings',
}

// 添加员工API
export enum MemberApiTypeEnum {
  SYSTEM_ROLE = 'SYSTEM_ROLE',
}

// 选择添加
export enum MemberSelectTypeEnum {
  ORG = 'org', // 组织架构
  ROLE = 'role', // 角色
  MEMBER = 'member', // 成员
}
