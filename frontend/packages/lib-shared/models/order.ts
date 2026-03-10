import type { ModuleField } from './common';

// TODO lmy
export interface SaveOrderParams {}

export interface UpdateOrderParams extends SaveOrderParams {
  id: string;
}

export interface OrderItem {
  id: string;
  name: string;
  contractName: string;
  contractId: string;
  moduleFields: ModuleField[]; // 自定义字段
}
