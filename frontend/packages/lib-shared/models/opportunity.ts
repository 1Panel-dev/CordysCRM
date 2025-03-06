import { ModuleField } from '@lib/shared/models/customer';

export interface OpportunityItem {
  id: string; // 商机ID
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
  moduleFields: ModuleField[]; // 自定义字段
}

// TODO ts 类型字段等待补充 工作流详情
export interface WorkflowStepItem {
  value: string;
  label: string;
  isError: boolean;
}
