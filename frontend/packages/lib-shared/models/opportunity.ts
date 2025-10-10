import type { TableQueryParams } from './common';
import { ModuleField } from '@lib/shared/models/customer';

export interface OpportunityItem {
  id: string; // 商机ID
  name: string;
  status: string;
  opportunityName: string; // 商机名称
  customerId: string;
  customerName: string; // 客户名称
  createUser: string; // 创建人ID
  updateUser: string; // 更新人ID
  createTime: number; // 创建时间
  updateTime: number; // 更新时间
  createUserName: string; // 创建人名称
  updateUserName: string; // 更新人名称
  reservedDays: number; // 归属天数
  stage: string;
  lastStage: string;
  inCustomerPool: boolean;
  poolId?: string;
  failureReason: string;
  hasPermission?: boolean;
  moduleFields: ModuleField[]; // 自定义字段
}

export interface SaveOpportunityParams {
  name: string;
  customerId: string; // 客户id
  amount: number; // 金额
  products: string[]; // 意向产品
  possible: number; // 可能性
  contactId: string; // 联系人ID
  owner: string; // 负责人
  moduleFields: ModuleField[]; // 自定义字段
}

export interface UpdateOpportunityParams extends SaveOpportunityParams {
  id: string;
}

export interface OpportunityDetail extends OpportunityItem {
  id: string;
  name: string;
  amount: number;
  possible: number;
  products: string[];
  contactId: string;
  contactName: string;
  stage: string; // 当前阶段
  status: string;
  owner: string;
  ownerName: string;
  lastStage: string; // 上一个阶段
}

export interface UpdateStageParams {
  id: string;
  stage: string;
  // expectedEndTime?: number; // 预计结束时间
  failureReason?: string | null; // 失败原因
}

export interface OpportunityPageQueryParams extends TableQueryParams {
  board?: boolean; // 是否是看板模式
}

export interface OpportunityBillboardDraggedParams {
  dragNodeId: string;
  dropNodeId: string;
  dropPosition: number;
  stage: string;
}
