import { VNodeChild } from 'vue';

import type { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
import type { TableKeyEnum } from '@lib/shared/enums/tableEnum';

import type { ActionsItem } from '@/components/pure/crm-more-action/type';
import type { CrmTagGroupProps } from '@/components/pure/crm-tag-group/index.vue';

import type { CrmPaginationProps } from '../crm-pagination/index.vue';
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
  showInTable?: boolean; // 是否展示在表格上、
  columnSelectorDisabled?: boolean; // 表头字段不可拖拽排序
  key?: DataTableColumnKey; // 这一列的 key，不可重复
  title?: string | (() => VNodeChild);
  sorter?: boolean | 'default'; // true是只展示图标，'default'是使用内置排序
  filter?: boolean | ((optionValue: string | number, rowData: object) => boolean) | 'default'; // true是只展示图标
  sortOrder?: 'descend' | 'ascend' | false; // 受控状态下表格的排序方式
  render?: (rowData: T, rowIndex: number) => VNodeChild;
  renderFilterMenu?: RenderFilterMenu;
  isTag?: boolean; // 标签列
  remoteFilterApiKey?: string; // filter筛选请求API_KEY
  tagGroupProps?: Omit<CrmTagGroupProps, 'tags'>; // 标签列属性
  filterOptions?: {
    value: string | number | boolean;
    label: string;
  }[];
  filterMultipleValue?: boolean; // 给后端传的筛选参数
  disabled?: (rowData: T) => boolean;
  selectTooltip?: {
    tooltipText?: string;
    showTooltip?: (rowData: T) => boolean;
  };
  filedType?: FieldTypeEnum; // 字段类型
  fieldId?: string; // 字段ID
};

export type CrmTableProps<T> = Omit<DataTableProps, 'columns'> & {
  'columns': CrmDataTableColumn[];
  'tableKey'?: TableKeyEnum; // 表格key, 用于存储表格列配置,pageSize等
  'tableRowKey'?: string; // 表格行的key
  'data': CrmTableDataItem<T>[];
  'showSetting'?: boolean; // 是否显示表格配置
  'showPagination'?: boolean; // 是否显示分页
  'crmPagination'?: PaginationProps & CrmPaginationProps; // 分页配置
  'onUpdate:checkedRowKeys'?: (key: DataTableRowKey[]) => void; // 覆写类型防止报错
  'isReturnNativeResponse'?: boolean;
  'permission'?: string[];
  'hiddenTotal'?: Ref<boolean>;
  'hiddenRefresh'?: boolean;
  'hiddenAllScreen'?: boolean;
  'containerClass': string; // 容器类名
  'notVirtualScroll'?: boolean;
};

// 表格存储
export interface TableStorageConfigItem {
  column: CrmDataTableColumn[]; // 列配置
  pageSize?: number;
  columnBackup: CrmDataTableColumn[]; // 列配置的备份，用于比较当前定义的列配置是否和备份的列配置相同
  layout?: string;
}

export interface BatchActionConfig {
  baseAction: ActionsItem[];
  moreAction?: ActionsItem[];
}
