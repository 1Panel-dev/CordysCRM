import { ColumnTypeEnum } from '@lib/shared/enums/commonEnum';
import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
import { ExportTableColumnItem } from '@lib/shared/models/common';

import { FilterFormItem } from '@/components/pure/crm-advance-filter/type';
import { CrmDataTableColumn } from '@/components/pure/crm-table/type';

export function getExportColumns(
  allColumns: CrmDataTableColumn[],
  customFieldsFilterConfig?: FilterFormItem[]
): ExportTableColumnItem[] {
  return allColumns
    .filter(
      (item: any) =>
        item.key !== 'operation' &&
        item.type !== 'selection' &&
        item.key !== 'crmTableOrder' &&
        item.filedType !== FieldTypeEnum.PICTURE
    )
    .map((e) => {
      return {
        key: e.key?.toString() || '',
        title: (e.title as string) || '',
        columnType: customFieldsFilterConfig?.some((i) => i.dataIndex === e.key)
          ? ColumnTypeEnum.CUSTOM
          : ColumnTypeEnum.SYSTEM,
      };
    });
}

export default {};
