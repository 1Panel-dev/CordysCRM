import { ColumnTypeEnum } from '@lib/shared/enums/commonEnum';
import { FieldTypeEnum, type FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { ExportTableColumnItem } from '@lib/shared/models/common';

import { FilterFormItem } from '@/components/pure/crm-advance-filter/type';
import { CrmDataTableColumn } from '@/components/pure/crm-table/type';

import type { FormCreateField } from '@cordys/web/src/components/business/crm-form-create/types';

// 跟进记录、跟进计划列表将「客户名称」「公司名称」合并为一列展示，导入导出仍按两个字段处理
const FOLLOW_RESOURCE_NAME_COLUMN_KEY = 'name';
const FOLLOW_RESOURCE_NAME_BUSINESS_KEYS = ['customerId', 'clueId'];

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

/**
 * 跟进记录、跟进计划列表的导出字段还原为「客户名称」「公司名称」两个字段。
 * 列表展示时才将两者合并为一列，导出选择字段及导出结果仍保持两个独立字段，
 * 表头使用表单字段名称，保证导出文件可直接用于导入。
 * @param exportColumns 由表格列生成的导出字段
 * @param fieldList 表单字段配置
 * @returns 拆分后的导出字段
 */
export function splitFollowResourceNameColumns(
  exportColumns: ExportTableColumnItem[],
  fieldList?: FormCreateField[]
): ExportTableColumnItem[] {
  if (!exportColumns.some((column) => column.key === FOLLOW_RESOURCE_NAME_COLUMN_KEY)) {
    return exportColumns;
  }

  const resourceNameColumns =
    fieldList
      ?.filter((field) => field.readable && FOLLOW_RESOURCE_NAME_BUSINESS_KEYS.includes(field.businessKey ?? ''))
      .map((field) => ({
        key: field.businessKey as string,
        title: field.name,
        columnType: ColumnTypeEnum.SYSTEM,
      })) ?? [];

  if (!resourceNameColumns.length) {
    return exportColumns;
  }

  const resourceNameKeys = new Set(resourceNameColumns.map((column) => column.key));
  return exportColumns.flatMap((column) => {
    if (column.key === FOLLOW_RESOURCE_NAME_COLUMN_KEY) {
      return resourceNameColumns;
    }
    return resourceNameKeys.has(column.key) ? [] : [column];
  });
}
