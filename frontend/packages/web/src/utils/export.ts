import { ColumnTypeEnum } from '@lib/shared/enums/commonEnum';
import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
import { ExportTableColumnItem } from '@lib/shared/models/common';

import { FilterFormItem } from '@/components/pure/crm-advance-filter/type';
import { CrmDataTableColumn } from '@/components/pure/crm-table/type';

import type { FormCreateField } from '@cordys/web/src/components/business/crm-form-create/types';

export function getExportColumns(
  allColumns: CrmDataTableColumn[],
  customFieldsFilterConfig?: FilterFormItem[],
  fieldList?: FormCreateField[]
): ExportTableColumnItem[] {
  const result = allColumns
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
  const subCol = fieldList?.find((i) => [FieldTypeEnum.SUB_PRODUCT, FieldTypeEnum.SUB_PRICE].includes(i.type));
  if (subCol) {
    result.push({
      key: subCol.businessKey ?? subCol.id,
      title: subCol.name,
      columnType: ColumnTypeEnum.CUSTOM,
    });
  }
  return result;
}

export default {};
