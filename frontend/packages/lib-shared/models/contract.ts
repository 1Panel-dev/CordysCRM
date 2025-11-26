import { ModuleField } from '@lib/shared/models/customer';
import { AttachmentInfo } from '@cordys/web/src/components/business/crm-form-create/types';

// 合同列表项
export interface ContractItem {
  id: string;
  name: string;
  customerId: string;
  customerName: string;
  amount: number;
  archivedStatus: string;
  status: string;
  owner: string;
  ownerName: string;
  createUser: string;
  updateUser: string;
  createTime: number;
  updateTime: number;
  createUserName: string;
  updateUserName: string;
  moduleFields: ModuleField[]; // 自定义字段
}

// 合同详情
export interface ContractDetail extends ContractItem {
  optionMap?: Record<string, any[]>;
  attachmentMap?: Record<string, AttachmentInfo[]>; // 附件信息映射
}

// 添加合同参数
export interface SaveContractParams {
  name: string;
  customerId: string; // 客户id
  amount?: number; // 金额
  owner: string; // 负责人
  moduleFields: ModuleField[]; // 自定义字段
}

// 更新合同参数
export interface UpdateContractParams extends SaveContractParams {
  id: string;
}

// 回款计划列表项
export interface PaymentPlanItem {
  id: string;
  createUser: string;
  updateUser: string;
  createTime: number;
  updateTime: number;
  contractId: string;
  owner: string;
  planStatus: string;
  planAmount: number;
  planEndTime: number;
  organizationId: string;
  createUserName: string;
  updateUserName: string;
  ownerName: string;
  departmentId: string;
  departmentName: string;
  contractName: string;
  moduleFields: ModuleField[]; // 自定义字段
}

// 回款计划详情
export interface PaymentPlanDetail extends PaymentPlanItem {
  optionMap?: Record<string, any[]>;
}

// 添加回款计划参数
export interface SavePaymentPlanParams {
  contractId: string;
  owner: string;
  planStatus: string;
  planAmount?: number;
  planEndTime?: number;
}

// 更新回款计划参数
export interface UpdatePaymentPlanParams extends SavePaymentPlanParams {
  id: string;
}
