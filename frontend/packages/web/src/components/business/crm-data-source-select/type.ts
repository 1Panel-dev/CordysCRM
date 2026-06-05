import type { FilterResult } from '../../pure/crm-advance-filter/type';
import { DataSourceType } from '../crm-form-create/types';
import type { RowData } from 'naive-ui/es/data-table/src/interface';

export interface DataSourceProps {
  dataSourceType: DataSourceType;
  multiple?: boolean;
  disabled?: boolean;
  disabledSelection?: (row: RowData) => boolean;
  maxTagCount?: number | 'responsive';
  filterParams?: FilterResult;
}
