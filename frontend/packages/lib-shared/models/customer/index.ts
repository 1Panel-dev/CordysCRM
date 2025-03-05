import type { CustomerFollowPlanStatusEnum, CustomerSearchTypeEnum } from '../../enums/customerEnum';
import type { TableQueryParams } from '../common';

export interface ModuleField {
  fieldId: string;
  fieldValue: string | number | boolean | (string | number)[];
}

export interface SaveCustomerParams {
  name: string;
  owner: string; // 负责人
  moduleFields: ModuleField[];
}

export interface UpdateCustomerParams extends SaveCustomerParams {
  id: string;
}

export interface CustomerTableParams extends TableQueryParams {
  searchType: CustomerSearchTypeEnum; // 搜索类型(ALL/SELF/DEPARTMENT/VISIBLE)
}

export interface CustomerListItem {
  id: string;
  name: string;
  owner: string; // 负责人
  inSharedPool: boolean; // 是否在公海池
  dealStatus: string; // 最终成交状态
  createUser: string;
  updateUser: string;
  createTime: number;
  updateTime: number;
  createUserName: string;
  updateUserName: string;
  departmentId: string;
  departmentName: string;
  latestFollowUpTime: number;
  collectionTime: number;
  reservedDays: number; // 剩余归属天数
  moduleFields: ModuleField[];
}

export interface CustomerDetail {
  id: string;
  name: string;
  owner: string;
  createUser: string;
  updateUser: string;
  createTime: number;
  updateTime: number;
  createUserName: string;
  updateUserName: string;
  moduleFields: ModuleField[];
}

export interface SaveCustomerFollowRecordParams {
  customerId: string;
  opportunityId: string;
  type: string;
  leadId: string;
  content: string;
  owner: string;
  contactId: string;
  moduleFields: ModuleField[];
}

export interface UpdateCustomerFollowRecordParams extends SaveCustomerFollowRecordParams {
  id: string;
}

export interface CustomerFollowRecordTableParams extends TableQueryParams {
  sourceId: string; // 客户ID/商机ID/线索ID
}

export interface CustomerFollowRecordListItem {
  id: string;
  customerId: string;
  opportunityId: string;
  type: string;
  leadId: string;
  content: string; // 跟进内容
  organizationId: string;
  owner: string;
  ownerName: string;
  contactId: string;
  contactName: string;
  createUser: string;
  createUserName: string;
  updateUser: string;
  updateUserName: string;
  createTime: number;
  updateTime: number;
  moduleFields: ModuleField[];
}

export interface SaveCustomerFollowPlanParams extends SaveCustomerFollowRecordParams {
  estimatedTime: number;
}

export interface UpdateCustomerFollowPlanParams extends SaveCustomerFollowPlanParams {
  id: string;
}

export interface CustomerFollowPlanTableParams extends TableQueryParams {
  sourceId: string; // 客户ID/商机ID/线索ID
  status: CustomerFollowPlanStatusEnum; // 状态: ALL/PREPARED/UNDERWAY/COMPLETED/CANCELLED
}

export interface CustomerFollowPlanListItem extends CustomerFollowRecordListItem {
  estimatedTime: number;
  status: string;
}

export interface SaveCustomerContractParams {
  customerId: string;
  name: string;
  owner: string;
  enable: boolean;
  moduleFields: ModuleField[];
}

export interface UpdateCustomerContractParams extends SaveCustomerContractParams {
  id: string;
}

export interface CustomerContractTableParams extends TableQueryParams {
  sourceId: string; // 客户ID
}

export interface CustomerContractListItem {
  id: string;
  createUser: string;
  updateUser: string;
  createTime: number;
  updateTime: number;
  customerId: string;
  owner: string;
  name: string;
  enable: boolean; // 是否启用
  disableReason: string; // 停用原因
  organizationId: string;
  departmentId: string;
  departmentName: string;
  customerName: string;
  createUserName: string;
  updateUserName: string;
  moduleFields: ModuleField[];
}

export interface Condition {
  column: string;
  operator: string;
  value: string;
}

export interface RecycleRule {
  expireNotice: boolean; // 到期提醒
  noticeDays: number; // 提前提醒天数
  operator: string; // 操作符
  conditions: Condition[]; // 规则条件集合
}

export interface PickRule {
  limitOnNumber: boolean; // 是否限制每日领取数量
  pickNumber: number; // 领取数量
  limitPreOwner: boolean; // 是否限制前归属人领取
  pickIntervalDays: number; // 领取间隔天数
}

export interface SaveCustomerOpenSeaParams {
  name: string;
  scopeIds: string[]; // 范围ID集合
  ownerIds: string[]; // 管理员ID集合
  enable: boolean;
  auto: boolean; // 是否自动回收
  pickRule: PickRule; // 领取规则
  recycleRule: RecycleRule; // 回收规则
}

export interface UpdateCustomerOpenSeaParams extends SaveCustomerOpenSeaParams {
  id: string;
}

export interface Member {
  id: string;
  scope: string;
  name: string;
}

export interface CustomerOpenSeaListItem {
  id: string;
  createUser: string;
  updateUser: string;
  createTime: number;
  updateTime: number;
  organizationId: string;
  name: string;
  scopeId: string;
  ownerId: string;
  enable: boolean;
  auto: boolean;
  members: Member[];
  owners: Member[];
  createUserName: string;
  updateUserName: string;
  pickRule: PickRule;
  recycleRule: RecycleRule;
}
