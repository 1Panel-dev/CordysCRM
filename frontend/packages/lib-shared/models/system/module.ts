import type { FormDesignKeyEnum } from '../../enums/formDesignEnum';
import type { TableQueryParams } from '../common';
import type { FormCreateField } from '@cordys/web/src/components/business/crm-form-create/types';
import { MemberSelectTypeEnum } from '@lib/shared/enums/moduleEnum';

// 模块首页-导航模块列表
export interface ModuleNavBaseInfoItem {
  id?: string;
  createUser?: string;
  updateUser?: string;
  createTime?: number;
  updateTime?: number;
  organizationId?: string;
  moduleKey: string; // 模块KEY
  enable: boolean;
  pos?: number; // 自定义排序
  disabled?: boolean; // 是否禁用
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

export interface SelectedUsersItem {
  id: string; // ID
  scope?: MemberSelectTypeEnum; // 范围
  name: string; // 名称
}

export interface ModuleConditionsItem {
  column: string;
  operator: string;
  value: string;
  scope?: string[];
}

export interface OpportunityBaseInfoItem {
  name: string;
  enable: boolean;
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
  members: SelectedUsersItem[]; // 成员集合
  owners: SelectedUsersItem[]; // 管理员集合
  createUserName: string;
  updateUserName: string;
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

// 线索池领取规则
export interface CluePoolPickRuleParams {
  limitOnNumber: boolean; // 是否限制领取数量
  pickNumber?: number; // 领取数量
  limitPreOwner: boolean; // 是否限制前归属人领取
  pickIntervalDays?: number; // 领取间隔天数
  limitNew: boolean; // 是否限制新数据领取
  newPickInterval?: number; // 新数据领取间隔天数
}

// 线索池回收规则
export interface CluePoolRecycleRuleParams {
  operator: string; // 操作符
  conditions: ModuleConditionsItem[]; // 规则条件集合
}

// 编辑线索池请求参数
export interface CluePoolParams {
  id?: string; // ID
  name: string; // 线索池名称
  scopeIds: string[]; // 范围ID集合
  ownerIds: string[]; // 管理员ID集合
  enable: boolean; // 启用/禁用
  auto: boolean; // 自动回收
  pickRule: CluePoolPickRuleParams; // 领取规则
  recycleRule: CluePoolRecycleRuleParams; // 回收规则
}

export interface CluePoolForm extends Omit<CluePoolParams, 'scopeIds' | 'ownerIds'> {
  adminIds: SelectedUsersItem[];
  userIds: SelectedUsersItem[]; // 成员ID
}

// 线索池列表项
export interface CluePoolItem {
  id: string;
  createUser: string;
  updateUser: string;
  updateUserName: string;
  createTime: number;
  updateTime: number;
  name: string;
  scopeId: string;
  organizationId: string;
  ownerId: string;
  enable: boolean;
  auto: boolean;
  members: SelectedUsersItem[];
  owners: SelectedUsersItem[];
  pickRule: CluePoolPickRuleParams; // 领取规则
  recycleRule: CluePoolRecycleRuleParams; // 回收规则
}

// 库容参数
export interface CapacityParams {
  scopeIds: string[]; // 范围ID集合
  capacity?: number; // 容量
}

// 库容列表项
export interface CapacityItem {
  id: string;
  createUser: string;
  updateUser: string;
  createTime: number;
  updateTime: number;
  organizationId: string;
  scopeId: string;
  capacity: number;
  members: SelectedUsersItem[];
  filters?: { column: 'string'; operator: 'string'; value: 'string' }[];
}

// 表单设计保存参数
export type FormFooterDirection = 'flex-row' | 'flex-row-reverse' | 'justify-center';
export interface FormActionButton {
  text: string;
  enable: boolean;
}
export interface FormConfig {
  layout: number;
  labelPos: 'left' | 'top';
  inputWidth: 'custom' | 'full';
  optBtnContent: FormActionButton[];
  optBtnPos: FormFooterDirection;
}

export interface SaveFormDesignConfigParams {
  formKey: FormDesignKeyEnum;
  fields: FormCreateField[];
  formProp: FormConfig;
}

export interface FormDesignConfigDetailParams {
  fields: FormCreateField[];
  formProp: FormConfig;
}

export interface FormDesignDataSourceTableQueryParams extends TableQueryParams {
  field: string;
}
