// 模块首页-导航模块列表
export interface ModuleNavBaseInfoItem {
  id: string;
  createUser: string;
  updateUser: string;
  createTime: number;
  updateTime: number;
  organizationId: string;
  moduleKey: string; // 模块KEY
  enable: boolean;
  pos: number; // 自定义排序
}

export interface ModuleNavItem extends ModuleNavBaseInfoItem {
  icon: string;
  key: string;
  label: string;
}

// 模块首页-导航模块排序入参
export interface ModuleSortParams {
  start: number;
  end: number;
  dragModuleId: string; // 拖拽模块ID
}

export interface ModuleUserScopedItem {
  id: string;
  scope: string;
  name: string;
}

export interface ModuleConditionsItem {
  column: string;
  operator: string;
  value: string;
}

export interface OpportunityBaseInfoItem {
  name: string;
  enable: boolean;
  expireNotice: boolean; // 到期提醒
  noticeDays: number; // 提前提醒天数
  operator: string; // 操作符
  auto: boolean; // 自动回收
}

// 模块商机列表
export interface OpportunityItem extends OpportunityBaseInfoItem {
  id: string;
  organizationId: string;
  ownerId: string; // 管理员ID
  scopeId: string; // 范围ID
  condition: string; // 回收条件
  createUser: string;
  updateUser: string;
  createTime: number;
  updateTime: number;
  members: ModuleUserScopedItem[]; // 成员集合
  owners: ModuleUserScopedItem[]; // 管理员集合
}

// 模块商机详情
export interface OpportunityDetail extends OpportunityBaseInfoItem {
  id?: string;
  conditions: ModuleConditionsItem[]; // 规则条件集合
}

export interface OpportunityParams extends OpportunityDetail {
  scopeIds: string[];
  ownerIds: string[];
}
