import type { TableQueryParams } from '../common';

export interface LoginLogParams extends TableQueryParams {
  operator: string | null;
  startTime?: number;
  endTime?: number;
}

export interface LoginLogItem {
  id: string;
  createTime: number;
  operator: string;
  loginAddress: string;
  platform: 'WEB' | 'MOBILE'; // 平台,WEB\MOBILE
  operatorName: string;
}

export interface OperationLogParams extends LoginLogParams {
  type?: string | null;
  module?: string | null;
}

export interface OperationLogItem {
  id: string;
  operator: string;
  operatorName: string;
  createTime: number;
  module: string;
  type: string;
  resourceName: string;
  detail: string;
}

export interface OperationLogDetailDiffItem {
  column: string;
  oldValue: string;
  newValue: string;
  columnName: string;
  oldValueName: string | number;
  newValueName: string | number;
  type: string;
}

export interface OperationLogDetail extends OperationLogItem {
  diffs?: OperationLogDetailDiffItem[];
}

export type AiExecutionLogOperator = 'auto' | string;
export type AiExecutionLogOperationType = 'data_write' | 'data_read' | 'task_execution' | 'model_call';
export type AiExecutionLogStatus = 'success' | 'failed' | 'confirming';

export interface AiExecutionLogParams extends LoginLogParams {
  operationType?: AiExecutionLogOperationType | null;
  modelId?: string | null;
  status?: AiExecutionLogStatus | null;
  keyword?: string;
}

export interface AiExecutionLogItem {
  id: string;
  operator: AiExecutionLogOperator;
  operatorName: string;
  confirmerName?: string;
  modelId: string;
  modelName: string;
  tokenCost: number;
  operationType: AiExecutionLogOperationType;
  status: AiExecutionLogStatus;
  taskName: string;
  createTime: number;
  operatorIp: string;
  prompt: string;
  result: string;
  executionResult: string;
}
