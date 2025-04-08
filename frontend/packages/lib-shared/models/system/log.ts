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
  platform: string;
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
  oldValue: Record<string, any>;
  newValue: Record<string, any>;
  columnName: string;
  oldValueName: Record<string, any>;
  newValueName: Record<string, any>;
  type: string;
}

export interface OperationLogDetail extends OperationLogItem {
  diffs?: OperationLogDetailDiffItem[];
}
