import { ColumnTypeEnum } from '@lib/shared/enums/commonEnum';
import { ExportTableColumnItem } from '@lib/shared/models/common';

import { FilterFormItem } from '@/components/pure/crm-advance-filter/type';

export function getExportColumns(
  filterConfigList: FilterFormItem[],
  customFieldsFilterConfig?: FilterFormItem[]
): ExportTableColumnItem[] {
  const system = filterConfigList.map((item) => {
    return {
      key: item.dataIndex as string,
      title: item.title as string,
      columnType: ColumnTypeEnum.SYSTEM,
    };
  });
  const custom =
    customFieldsFilterConfig?.map((item) => {
      return {
        key: item.dataIndex as string,
        title: item.title as string,
        columnType: ColumnTypeEnum.CUSTOM,
      };
    }) ?? [];
  return [...system, ...custom];
}

export default {};
