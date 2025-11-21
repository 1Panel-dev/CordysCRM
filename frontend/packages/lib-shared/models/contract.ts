import { ModuleField } from '@lib/shared/models/customer';

// TODO lmy 等后端
// 合同列表查询参数
export interface ContractPageQueryParams {}

// 合同列表项
export interface ContractItem {
  id: string;
  name: string;
}

// 合同详情
export interface ContractDetail {
  id: string;
  name: string;
  moduleFields: ModuleField[]; // 自定义字段
}

// 添加合同参数
export interface SaveContractParams {
  name: string;
}

// 更新合同参数
export interface UpdateContractParams extends SaveContractParams {
  id: string;
}

// 回款计划列表查询参数
export interface PaymentPlanPageQueryParams {}

// 回款计划列表项
export interface PaymentPlanItem {
  id: string;
  name: string;
}

// 回款计划详情
export interface PaymentPlanDetail {
  id: string;
  name: string;
  moduleFields: ModuleField[]; // 自定义字段
}

// 添加回款计划参数
export interface SavePaymentPlanParams {
  name: string;
}

// 更新回款计划参数
export interface UpdatePaymentPlanParams extends SavePaymentPlanParams {
  id: string;
}
