import { ColumnTypeEnum } from '@lib/shared/enums/commonEnum';
import { FieldTypeEnum, type FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { ExportTableColumnItem } from '@lib/shared/models/common';

import { FilterFormItem } from '@/components/pure/crm-advance-filter/type';
import { CrmDataTableColumn } from '@/components/pure/crm-table/type';

import type { FormCreateField } from '@cordys/web/src/components/business/crm-form-create/types';

export function getColumnType(item: any, customFieldsFilterConfig?: FilterFormItem[], supportShowFields?: boolean) {
  if (customFieldsFilterConfig?.some((i) => i.dataIndex === item.key)) {
    return ColumnTypeEnum.CUSTOM;
  }
  if (supportShowFields && item.resourceFieldId) {
    return ColumnTypeEnum.SHOW_FIELD;
  }
  return ColumnTypeEnum.SYSTEM;
}

export function getExportColumns(
  allColumns: CrmDataTableColumn[],
  customFieldsFilterConfig?: FilterFormItem[],
  fieldList?: FormCreateField[],
  supportShowFields?: boolean
): ExportTableColumnItem[] {
  const result = allColumns
    .filter(
      (item: any) =>
        item.key !== 'operation' &&
        item.type !== 'selection' &&
        item.key !== 'crmTableOrder' &&
        item.filedType !== FieldTypeEnum.PICTURE &&
        (supportShowFields || !item.resourceFieldId) // 部分表单支持导出显示字段
    )
    .map((e) => {
      return {
        key: e.key?.toString() || '',
        title: (e.title as string) || '',
        columnType: getColumnType(e, customFieldsFilterConfig, supportShowFields),
      };
    });
  const subCol = fieldList?.filter((i) => [FieldTypeEnum.SUB_PRODUCT, FieldTypeEnum.SUB_PRICE].includes(i.type));
  subCol?.forEach((j) => {
    result.push({
      key: j.businessKey ?? j.id,
      title: j.name,
      columnType: ColumnTypeEnum.CUSTOM,
    });
  });
  return result;
}
