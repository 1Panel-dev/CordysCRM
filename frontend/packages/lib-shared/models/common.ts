import { OperatorEnum } from '@lib/shared/enums/commonEnum';

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
