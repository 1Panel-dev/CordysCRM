import type { ModuleField, TableQueryParams } from './common';
import type { FormDesignConfigDetailParams } from '@lib/shared/models/system/module';
import { ProcessStatusType } from '@lib/shared/models/system/process';
import { QuotationStatusEnum } from '@lib/shared/enums/opportunityEnum';

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
  stageName: string;
  lastStage: string;
  inCustomerPool: boolean;
  poolId?: string;
  failureReason: string;
  hasPermission?: boolean;
  moduleFields: ModuleField[]; // 自定义字段
  amount: number; // 金额
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

export interface UpdateStageBaseParams {
  id: string;
  name: string;
}

export interface UpdateOpportunityStageParams {
  rate: string;
}

export interface UpdateOpportunityStageRollbackParams {
  afootRollBack: boolean;
  endRollBack: boolean;
}

export interface StageBaseParams{
  name: string;
  type: 'AFOOT' | 'END';
  dropPosition: number;
  targetId: string;
}

export interface AddOpportunityStageParams extends StageBaseParams {
  rate: string;
}

export interface StageConfigBaseItem{
  id: string;
  createUser: string;
  updateUser: string;
  createTime: number;
  updateTime: number;
  name: string;
  type: 'AFOOT' | 'END';
  afootRollBack: boolean;
  endRollBack: boolean;
  pos: number;
  organizationId: string;
}

export interface StageConfigItem extends StageConfigBaseItem {
  rate: string;
}

export interface OpportunityStageConfig {
  stageConfigList: StageConfigItem[];
  afootRollBack: boolean;
  endRollBack: boolean;
  stageHasData: boolean;
}

export interface QuotationQueryParams extends TableQueryParams {
  board?: boolean; // 是否是看板模式
}

export interface QuotationItem {
  id: string;
  name: string;
  approvalStatus: ProcessStatusType;
  status: QuotationStatusEnum;
  opportunityId: string;
  opportunityName: string;
  amount: number;
  createUser: string;
  updateUser: string;
  createTime: number;
  updateTime: number;
  createUserName: string;
  updateUserName: string;
  moduleFields: ModuleField[];
  products?: any[];
}

export interface SaveQuotationParams {
  name: string;
  opportunityId: string;
  amount: number;
  moduleFields: ModuleField[]; // 自定义字段
  moduleFormConfigDTO?: FormDesignConfigDetailParams;
}

export interface UpdateQuotationParams extends SaveQuotationParams {
  id: string;
  approvalStatus: ProcessStatusType;
}

export interface ApproveQuotation {
  id: string;
  name: string;
  opportunityId: string;
  approvalStatus: ProcessStatusType;
  moduleFormConfigDTO?: FormDesignConfigDetailParams;
  moduleFields: ModuleField[];
  products: any[];
}

export interface BatchUpdateQuotationStatusParams {
  ids: (string | number)[];
  approvalStatus: ProcessStatusType;
}

export interface  BatchVoidQuotationStatusParams {
  ids: (string | number)[];
  // todo 批量作废 
  status: QuotationStatusEnum;
}

export interface BatchOperationResult {
  success: number;
  fail: number;
  skip?: number;
  errorMessages?: string;
}
