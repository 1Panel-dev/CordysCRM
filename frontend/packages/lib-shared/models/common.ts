import { ColumnTypeEnum, OperatorEnum } from '@lib/shared/enums/commonEnum';

// 请求返回结构
export default interface CommonResponse<T> {
  code: number;
  message: string;
  messageDetail: string;
  data: T;
}

export interface SortParams {
  name?: string;
  type?: string; // asc或desc
}

// 表格查询
export interface TableQueryParams {
  // 当前页
  current?: number;
  // 每页条数
  pageSize?: number;
  // 排序仅针对单个字段
  sort?: SortParams;
  // 表头筛选
  filter?: object;
  // 查询条件
  keyword?: string;
  [key: string]: any;
}

export interface CommonList<T> {
  [x: string]: any;
  pageSize: number;
  total: number;
  current: number;
  list: T[];
  optionMap?: Record<string, any[]>;
}

export interface FilterConditionItem {
  name: string;
  value: any; // 期望值，若操作符为 BETWEEN, IN, NOT_IN 时为数组，其他操作符为单个值
  operator: OperatorEnum;
  multipleValue?: boolean;
}

export interface ExportTableColumnItem {
  key: string;
  title: string;
  columnType: ColumnTypeEnum;
}

export interface TableExportParams extends TableQueryParams {
  fileName: string; // 导出文件名
  headList: ExportTableColumnItem[]; // 导出表头
}

export interface TableExportSelectedParams {
  fileName: string;
  headList: ExportTableColumnItem[];
  ids: string[];
}

export interface TableDraggedParams {
  moveId: string;
  moveMode: 'BEFORE' | 'AFTER';
  orgId: string;
  targetId: string;
  oldIndex: number;
  newIndex: number;
}

export interface SystemVersion {
  currentVersion: string; // 当前版本
  releaseDate: string; // 发行日期
  latestVersion: string; // 最新版本
  architecture: string; // 系统架构
  copyright: string; // 版权信息
  hasNewVersion: boolean; // 是否有新版本
}

export interface ModuleDragParams {
  dragNodeId: string;
  dropNodeId: string;
  dropPosition: number;
}
