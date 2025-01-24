import { VNodeChild } from 'vue';

import type { TableKeyEnum } from '@lib/shared/enums/tableEnum';
import type {
  DataTableColumn,
  DataTableColumnKey,
  DataTableProps,
  DataTableRowData,
  DataTableRowKey,
  PaginationProps,
} from 'naive-ui';

export type CrmTableDataItem<T> = T & {
  updateTime?: string | number | null;
  createTime?: string | number | null;
  children?: CrmTableDataItem<T>[];
} & DataTableRowData;

export type CrmDataTableColumn = DataTableColumn & {
  showInTable?: boolean; // 是否展示在表格上
  key?: DataTableColumnKey; // 这一列的 key，不可重复
  title?: string | (() => VNodeChild);
  sorter?: boolean | 'default'; // true是只展示图标，'default'是使用内置排序
  filter?: boolean | ((optionValue: string | number, rowData: object) => boolean) | 'default'; // true是只展示图标
  sortOrder?: 'descend' | 'ascend' | false; // 受控状态下表格的排序方式
  render?: (rowData: object, rowIndex: number) => VNodeChild;
};

export interface CrmTableProps<T> extends DataTableProps {
  'columns': CrmDataTableColumn[];
  'tableKey'?: TableKeyEnum; // 表格key, 用于存储表格列配置,pageSize等
  'tableRowKey'?: string; // 表格行的key
  'data': CrmTableDataItem<T>[];
  'showSetting'?: boolean; // 是否显示表格配置
  'showPagination'?: boolean; // 是否显示分页
  'crmPagination'?: PaginationProps; // 分页配置
  'onUpdate:checkedRowKeys'?: (key: DataTableRowKey[]) => void; // 覆写类型防止报错
}

// 表格存储
export interface TableStorageConfigItem {
  column: CrmDataTableColumn[]; // 列配置
  pageSize?: number;
  columnBackup: CrmDataTableColumn[]; // 列配置的备份，用于比较当前定义的列配置是否和备份的列配置相同
}
