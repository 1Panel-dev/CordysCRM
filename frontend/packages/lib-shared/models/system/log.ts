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
