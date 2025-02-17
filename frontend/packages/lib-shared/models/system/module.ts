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
