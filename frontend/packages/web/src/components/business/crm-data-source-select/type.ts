import type { FieldDataSourceTypeEnum } from '@lib/shared/enums/formDesignEnum';

import type { FilterResult } from '../../pure/crm-advance-filter/type';
import type { RowData } from 'naive-ui/es/data-table/src/interface';

export interface DataSourceProps {
  dataSourceType: FieldDataSourceTypeEnum;
  multiple?: boolean;
  disabled?: boolean;
  disabledSelection?: (row: RowData) => boolean;
  maxTagCount?: number | 'responsive';
  filterParams?: FilterResult;
}
