import type { ModuleField } from './common';
import type { FormDesignConfigDetailParams } from '@lib/shared/models/system/module';

export interface SaveOrderParams {
  name: string;
  customerId: string; // 客户id
  contractId: string; 
  amount?: number; // 金额
  owner: string; // 负责人
  moduleFields?: ModuleField[]; // 自定义字段
  moduleFormConfigDTO?: FormDesignConfigDetailParams;
}

export interface UpdateOrderParams extends SaveOrderParams {
  id: string;
}

export interface OrderItem {
  id: string;
  name: string;
  contractName: string;
  contractId: string;
  moduleFields: ModuleField[]; // 自定义字段
  createUser: string;
  updateUser: string;
  customerId: string;
  owner: string;
  number: string;
  stage: string;
  stageName: string;
  organizationId: string;
  customerName: string;
  createUserName: string;
  updateUserName: string;
  departmentId: string;
  departmentName: string;
  createTime:number;
  updateTime:number;
  amount:number;
  inCustomerPool: boolean;
  poolId: string;
  optionMap?: Record<string, any>;
  attachmentMap?: Record<string, any>;
}
