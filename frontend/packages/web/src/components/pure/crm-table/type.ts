import { VNodeChild } from 'vue';

import type { TableKeyEnum } from '@lib/shared/enums/tableEnum';

import type { ActionsItem } from '@/components/pure/crm-more-action/type';
import type { CrmTagGroupProps } from '@/components/pure/crm-tag-group/index.vue';

import type { DataTableColumnKey, DataTableProps, DataTableRowData, DataTableRowKey, PaginationProps } from 'naive-ui';
import type {
  RenderFilterMenu,
  TableBaseColumn,
  TableColumnGroup,
  TableExpandColumn,
  TableSelectionColumn,
} from 'naive-ui/es/data-table/src/interface';

export type CrmTableDataItem<T> = {
  updateTime?: string | number | null;
  createTime?: string | number | null;
  children?: CrmTableDataItem<T>[];
} & DataTableRowData &
  T;

export type CrmDataTableColumn<T = any> = (
  | Omit<TableBaseColumn<T>, 'filterOptions'>
  | TableColumnGroup<T>
  | TableSelectionColumn<T>
  | TableExpandColumn<T>
) & {
  showInTable?: boolean; // 是否展示在表格上
  key?: DataTableColumnKey; // 这一列的 key，不可重复
  title?: string | (() => VNodeChild);
  sorter?: boolean | 'default'; // true是只展示图标，'default'是使用内置排序
  filter?: boolean | ((optionValue: string | number, rowData: object) => boolean) | 'default'; // true是只展示图标
  sortOrder?: 'descend' | 'ascend' | false; // 受控状态下表格的排序方式
  render?: (rowData: T, rowIndex: number) => VNodeChild;
  renderFilterMenu?: RenderFilterMenu;
  isTag?: boolean; // 标签列
  tagGroupProps?: Omit<CrmTagGroupProps, 'tags'>; // 标签列属性
  filterOptions?: {
    value: string | number | boolean;
    label: string;
  }[];
};

export type CrmTableProps<T> = Omit<DataTableProps, 'columns'> & {
  'columns': CrmDataTableColumn[];
  'tableKey'?: TableKeyEnum; // 表格key, 用于存储表格列配置,pageSize等
  'tableRowKey'?: string; // 表格行的key
  'data': CrmTableDataItem<T>[];
  'showSetting'?: boolean; // 是否显示表格配置
  'showPagination'?: boolean; // 是否显示分页
  'crmPagination'?: PaginationProps; // 分页配置
  'onUpdate:checkedRowKeys'?: (key: DataTableRowKey[]) => void; // 覆写类型防止报错
  'isReturnNativeResponse'?: boolean;
};

// 表格存储
export interface TableStorageConfigItem {
  column: CrmDataTableColumn[]; // 列配置
  pageSize?: number;
  columnBackup: CrmDataTableColumn[]; // 列配置的备份，用于比较当前定义的列配置是否和备份的列配置相同
}

export interface BatchActionConfig {
  baseAction: ActionsItem[];
  moreAction?: ActionsItem[];
}
