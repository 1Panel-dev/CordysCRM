import { ModuleField } from '@lib/shared/models/customer';
import { AttachmentInfo } from '@cordys/web/src/components/business/crm-form-create/types';
import { QuotationStatusEnum } from '@lib/shared/enums/opportunityEnum';
import { ContractBusinessTitleStatusEnum, type ContractInvoiceStatusEnum } from '@lib/shared/enums/contractEnum';
import type { TableQueryParams } from './common';
import type { FormDesignConfigDetailParams } from './system/module';

// 合同列表项
export interface ContractItem {
  id: string;
  name: string;
  customerId: string;
  customerName: string;
  amount: number;
  approvalStatus: QuotationStatusEnum;
  stage: string;
  owner: string;
  ownerName: string;
  createUser: string;
  updateUser: string;
  createTime: number;
  updateTime: number;
  createUserName: string;
  updateUserName: string;
  moduleFields: ModuleField[]; // 自定义字段
  inCustomerPool: boolean;
  poolId: string;
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

export interface ApprovalContractParams {
  id: string;
  approvalStatus: string;
}

// 回款计划列表项
export interface PaymentPlanItem {
  id: string;
  name: string;
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
  contractId?: string;
  owner?: string;
  planStatus: string;
  planAmount?: number;
  planEndTime?: number;
}

// 更新回款计划参数
export interface UpdatePaymentPlanParams extends SavePaymentPlanParams {
  id: string;
}
// 回款记录列表项
export interface PaymentRecordItem {
  id: string;
  createUser: string;
  updateUser: string;
  createTime: number;
  updateTime: number;
  name: string;
  contractId: string;
  owner: string;
  amount: number;
  invoiceType: string;
  taxRate: number;
  businessTitleId: string;
  approvalStatus: string;
  organizationId: string;
  contractName: string;
  ownerName: string;
  createUserName: string;
  updateUserName: string;
  paymentPlanName: string;
  paymentPlanId: string;
  departmentId: string;
  departmentName: string;
  moduleFields: ModuleField[];
}

// 回款记录详情
export interface PaymentRecordDetail extends PaymentRecordItem {
  optionMap?: Record<string, any[]>;
}

// 添加回款记录参数
export interface SavePaymentRecordParams {
  contractId: string;
  owner: string;
  name: string;
  paymentPlanId?: string;
  recordAmount: number;
  recordEndTime: number;
  recordBank: string;
  recordBankNo: string;
}

// 更新回款记录参数
export interface UpdatePaymentRecordParams extends SavePaymentRecordParams {
  id: string;
}

export interface BusinessTitleItem {
  id: string;
  name: string;
  type: 'THIRD_PARTY' | 'CUSTOM';
  identificationNumber: string;
  openingBank: string;
  bankAccount: string;
  registrationAddress: string;
  phoneNumber: string;
  registeredCapital: string;
  companySize: string;
  registrationNumber: string;
  approvalStatus: ContractBusinessTitleStatusEnum;
  unapprovedReason: string;
  organizationId: string;
  createUserName: string;
  updateUserName: string;
  createUser: string;
  updateUser: string;
  createTime: number;
  updateTime: number;
}

export interface SaveBusinessTitleParams {
  id?: string;
  name: string; // 公司名称
  identificationNumber: string; // 纳税人识别号
  openingBank: string; // 开户银行
  bankAccount: string; // 银行账号
  registrationAddress: string; // 注册地址
  phoneNumber: string; // 注册电话
  registeredCapital: string; // 注册资本
  companySize: string; // 公司规模
  registrationNumber: string; //工商注册号
  type: string; // 来源类型
}

export interface BusinessTitleValidateConfig {
  id: string;
  field: keyof SaveBusinessTitleParams;
  title: string;
  required: boolean;
  disabled?: boolean;
  organizationId: string;
  rule?: Record<string, any>[];
}
export interface ContractInvoiceTableQueryParam extends TableQueryParams {
  contractId?: string;
  customerId?: string;
}

export interface ContractInvoiceItem {
  id: string;
  contractId: string;
  name: string;
  no: string;
  owner: string;
  businessTitleId: string;
  businessTitleName: string;
  organizationId: string;
  createUser: string;
  createUserName: string;
  updateUser: string;
  updateUserName: string;
  ownerName: string;
  departmentId: string;
  departmentName: string;
  contractName: string;
  paymentPlanId: string;
  recordBank: string;
  recordBankNo: string;
  paymentPlanName: string;
  planName: string;
  moduleFields: ModuleField[]; // 自定义字段
  recordAmount: number;
  recordEndTime: number;
  approvalStatus: ContractInvoiceStatusEnum;
}
export interface SaveContractInvoiceParams {
  name: string;
  contractId: string;
  owner: string;
  amount: number;
  invoiceType: string;
  taxRate: number;
  businessTitleId: string;
  moduleFormConfigDTO?: FormDesignConfigDetailParams;
}

export interface UpdateContractInvoiceParams extends SaveContractInvoiceParams {
  id: string;
}

export interface ContractInvoiceDetail extends ContractInvoiceItem {
  optionMap?: Record<string, any[]>;
  attachmentMap?: Record<string, AttachmentInfo[]>; // 附件信息映射
}
