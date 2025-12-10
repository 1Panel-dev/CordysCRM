import type { CommonList } from '../models/common';
import { FieldTypeEnum } from '../enums/formDesignEnum';
import type { FormCreateField, FormDetail } from '@cordys/web/src/components/business/crm-form-create/types';
import { formatTimeValue, getCityPath, getIndustryPath } from './index';
import type { ModuleField } from '../models/customer';
import { useI18n } from '../hooks/useI18n';

export const linkAllAcceptTypes = [FieldTypeEnum.INPUT, FieldTypeEnum.TEXTAREA];
export const dataSourceTypes = [FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.DATA_SOURCE_MULTIPLE];
export const hiddenTypes = [FieldTypeEnum.DIVIDER, FieldTypeEnum.PICTURE, FieldTypeEnum.ATTACHMENT, FieldTypeEnum.LINK];
export const needSameTypes = [
  FieldTypeEnum.PHONE,
  FieldTypeEnum.LOCATION,
  FieldTypeEnum.DATE_TIME,
  FieldTypeEnum.INPUT_NUMBER,
  FieldTypeEnum.INDUSTRY,
  FieldTypeEnum.SUB_PRICE,
  FieldTypeEnum.SUB_PRODUCT,
];
export const multipleTypes = [FieldTypeEnum.CHECKBOX, FieldTypeEnum.SELECT_MULTIPLE, FieldTypeEnum.INPUT_MULTIPLE];
export const memberTypes = [FieldTypeEnum.MEMBER, FieldTypeEnum.MEMBER_MULTIPLE];
export const departmentTypes = [FieldTypeEnum.DEPARTMENT, FieldTypeEnum.DEPARTMENT_MULTIPLE];
export const singleTypes = [FieldTypeEnum.RADIO, FieldTypeEnum.SELECT];
export const specialBusinessKeyMap: Record<string, string> = {
  customerId: 'customerName',
  contactId: 'contactName',
  clueId: 'clueName',
  businessId: 'businessName',
  contractId: 'contractName',
  owner: 'ownerName',
};

export function getRuleType(item: FormCreateField) {
  if (
    item.type === FieldTypeEnum.SELECT_MULTIPLE ||
    item.type === FieldTypeEnum.CHECKBOX ||
    item.type === FieldTypeEnum.INPUT_MULTIPLE ||
    item.type === FieldTypeEnum.MEMBER_MULTIPLE ||
    item.type === FieldTypeEnum.DEPARTMENT_MULTIPLE ||
    item.type === FieldTypeEnum.DATA_SOURCE ||
    item.type === FieldTypeEnum.DATA_SOURCE_MULTIPLE ||
    item.type === FieldTypeEnum.PICTURE ||
    item.type === FieldTypeEnum.ATTACHMENT
  ) {
    return 'array';
  }
  if (item.type === FieldTypeEnum.DATE_TIME) {
    return 'date';
  }
  if (item.type === FieldTypeEnum.INPUT_NUMBER) {
    return 'number';
  }
  return 'string';
}

export function getNormalFieldValue(item: FormCreateField, value: any) {
  if (item.type === FieldTypeEnum.DATA_SOURCE && !value) {
    return '';
  }
  if (
    [
      FieldTypeEnum.SELECT_MULTIPLE,
      FieldTypeEnum.MEMBER_MULTIPLE,
      FieldTypeEnum.DEPARTMENT_MULTIPLE,
      FieldTypeEnum.DATA_SOURCE_MULTIPLE,
      FieldTypeEnum.INPUT_MULTIPLE,
    ].includes(item.type) &&
    !value
  ) {
    return [];
  }
  if (item.type === FieldTypeEnum.INPUT_MULTIPLE && !value) {
    return [];
  }
  if (item.multiple && !value) {
    return [];
  }
  return value;
}

/**
 * 格式化数字
 * @param value 数字
 * @param item
 */
export function formatNumberValue(value: string | number, item: FormCreateField) {
  if (value !== undefined && value !== null && value !== '') {
    if (item.numberFormat === 'percent') {
      return item.precision ? `${Number(value).toFixed(item.precision)}%` : `${value}%`;
    }
    if (item.showThousandsSeparator) {
      return (item.precision ? Number(Number(value).toFixed(item.precision)) : Number(value)).toLocaleString('en-US');
    }
    return item.precision ? Number(value).toFixed(item.precision) : value.toString();
  }
  return '-';
}

export function initFieldValue(field: FormCreateField, value: string | number | (string | number)[]) {
  if (
    [FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.DATA_SOURCE_MULTIPLE].includes(field.type) &&
    typeof value === 'string'
  ) {
    return value ? [value] : [];
  }
  return value;
}

export function parseModuleFieldValue(item: FormCreateField, fieldValue: string | string[], options?: any[]) {
  const { t } = useI18n();
  let value: string | string[] = fieldValue || '';
  if (options) {
    // 若字段值是选项值，则取选项值的name
    if (Array.isArray(fieldValue)) {
      value = fieldValue.map((e) => {
        const option = options.find((opt) => opt.id === e);
        if (option) {
          return option.name || t('common.optionNotExist');
        }
        return t('common.optionNotExist');
      });
    } else {
      value = options.find((e) => e.id === fieldValue)?.name || t('common.optionNotExist');
    }
  } else if (item.type === FieldTypeEnum.LOCATION) {
    const addressArr: string[] = (fieldValue as string).split('-') || [];
    value = addressArr.length ? `${getCityPath(addressArr[0])}-${addressArr.filter((e, i) => i > 0).join('-')}` : '-';
  } else if (item.type === FieldTypeEnum.INDUSTRY) {
    value = fieldValue ? getIndustryPath(fieldValue as string) : '-';
  } else if (item.type === FieldTypeEnum.INPUT_NUMBER) {
    value = formatNumberValue(fieldValue as string, item);
  } else if (item.type === FieldTypeEnum.DATE_TIME) {
    value = formatTimeValue(fieldValue as string, item.dateType);
  }
  if (Array.isArray(value) && item.resourceFieldId) {
    value = value.join(',');
  }
  return value;
}

export function parseFormDetailValue(item: FormCreateField, form: FormDetail, sourceName?: Ref<string>) {
  const { t } = useI18n();
  if (item.businessKey && !item.resourceFieldId) {
    // 引用数据源字段使用 id 读取数据，而不是 businessKey
    const options = form.optionMap?.[item.businessKey];
    // 业务标准字段读取最外层，读取form[item.businessKey]取到 id 值，然后去 options 里取 name
    let name: string | string[] = '';
    const value = form[item.businessKey];
    // 若字段值是选项值，则取选项值的name
    if (options) {
      if (Array.isArray(value)) {
        name = value.map((e) => {
          const option = options.find((opt) => opt.id === e);
          if (option) {
            return option.name || t('common.optionNotExist');
          }
          return t('common.optionNotExist');
        });
      } else {
        name = options.find((e) => e.id === value)?.name || t('common.optionNotExist');
      }
    }
    if (item.type === FieldTypeEnum.DATE_TIME) {
      return formatTimeValue(name || form[item.businessKey], item.dateType);
    }
    if (item.type === FieldTypeEnum.INPUT_NUMBER) {
      return formatNumberValue(name || form[item.businessKey], item);
    }
    if (item.type === FieldTypeEnum.ATTACHMENT) {
      return form.attachmentMap?.[item.businessKey] || [];
    }
    if (item.businessKey === 'name' && sourceName) {
      sourceName.value = name || form[item.businessKey];
    }
    return name || form[item.businessKey];
  }
  const options = form.optionMap?.[item.id];
  // 其他的字段读取moduleFields
  const field = form.moduleFields?.find((moduleField: ModuleField) => moduleField.fieldId === item.id);
  if (item.type === FieldTypeEnum.ATTACHMENT) {
    return form.attachmentMap?.[item.id] || [];
  }
  if (field) {
    return parseModuleFieldValue(item, field.fieldValue, options);
  }
}

/**
 * 表单配置表格回显数据
 */
export function transformData({
  item,
  fields,
  originalData,
  excludeFieldIds,
  needParseSubTable = false,
}: {
  fields: FormCreateField[];
  item: any;
  originalData?: CommonList<any>;
  excludeFieldIds?: string[];
  needParseSubTable?: boolean;
}) {
  const { t } = useI18n();
  const businessFieldAttr: Record<string, any> = {};
  const customFieldAttr: Record<string, any> = {};
  const addressFieldIds: string[] = [];
  const industryFieldIds: string[] = [];
  const dataSourceFieldIds: string[] = [];
  const timeFieldIds: string[] = [];
  let subTableFieldInfo: Record<string, any> = {};

  fields.forEach((field) => {
    const fieldId = field.businessKey || field.id;
    if (field.type === FieldTypeEnum.LOCATION) {
      addressFieldIds.push(fieldId);
    } else if (field.type === FieldTypeEnum.INDUSTRY) {
      industryFieldIds.push(fieldId);
    } else if (field.type === FieldTypeEnum.DATA_SOURCE) {
      dataSourceFieldIds.push(fieldId);
    } else if (field.type === FieldTypeEnum.DATE_TIME) {
      timeFieldIds.push(fieldId);
    } else if ([FieldTypeEnum.SUB_PRICE, FieldTypeEnum.SUB_PRODUCT].includes(field.type) && needParseSubTable) {
      field.subFields?.forEach((subField) => {
        item[fieldId]?.forEach((subItem: Record<string, any>) => {
          if (subField.resourceFieldId) {
            subItem[subField.id] = parseModuleFieldValue(
              subField,
              subItem[subField.businessKey || subField.id],
              originalData?.optionMap?.[subField.id]
            );
          } else {
            // 不使用业务 key，直接使用字段 id 去取值
            subItem[subField.id] = parseModuleFieldValue(
              subField,
              subItem[subField.businessKey || subField.id],
              originalData?.optionMap?.[subField.businessKey || subField.id]
            );
          }
        });
      });
    }
    if (field.businessKey) {
      const fieldId = field.businessKey;
      const options = originalData?.optionMap?.[fieldId]?.map((e: any) => ({
        ...e,
        name: e.name || t('common.optionNotExist'),
      }));
      if (addressFieldIds.includes(fieldId)) {
        // 地址类型字段，解析代码替换成省市区
        const addressArr: string[] = item[fieldId]?.split('-') || [];
        const value = addressArr.length
          ? `${getCityPath(addressArr[0])}-${addressArr.filter((e, i) => i > 0).join('-')}`
          : '-';
        businessFieldAttr[fieldId] = value;
      } else if (industryFieldIds.includes(fieldId)) {
        // 行业类型字段，解析代码替换成行业名称
        businessFieldAttr[fieldId] = item[fieldId] ? getIndustryPath(item[fieldId] as string) : '-';
      } else if (timeFieldIds.includes(fieldId)) {
        // 时间类型字段，格式化时间显示
        businessFieldAttr[fieldId] = formatTimeValue(item[fieldId], field.dateType);
      } else if (options && options.length > 0) {
        let name: string | string[] = '';
        if (dataSourceFieldIds.includes(fieldId)) {
          // 处理数据源字段，需要赋值为数组
          if (typeof item[fieldId] === 'string' || typeof item[fieldId] === 'number') {
            // 单选
            name = [options?.find((e) => e.id === item[fieldId])?.name || t('common.optionNotExist')];
          } else {
            // 多选
            name = options?.filter((e) => item[fieldId]?.includes(e.id)).map((e) => e.name) || [
              t('common.optionNotExist'),
            ];
          }
        } else if (typeof item[fieldId] === 'string' || typeof item[fieldId] === 'number') {
          // 若值是单个字符串/数字
          name = options?.find((e) => e.id === item[fieldId])?.name;
        } else {
          // 若值是数组
          name = options?.filter((e) => item[fieldId]?.includes(e.id)).map((e) => e.name) || [
            t('common.optionNotExist'),
          ];
          if (Array.isArray(name) && name.length === 0) {
            name = [t('common.optionNotExist')];
          }
        }
        if (!excludeFieldIds?.includes(field.businessKey)) {
          businessFieldAttr[fieldId] = name || t('common.optionNotExist');
        }
        if (fieldId === 'owner') {
          businessFieldAttr.ownerId = item.owner;
        }
      } else if (specialBusinessKeyMap[fieldId]) {
        // 处理特殊业务 key 映射关系
        businessFieldAttr[specialBusinessKeyMap[fieldId]] = item[specialBusinessKeyMap[fieldId]];
      }
    }
  });

  item.moduleFields?.forEach((field: ModuleField) => {
    const options = originalData?.optionMap?.[field.fieldId]?.map((e) => ({
      ...e,
      name: e.name || t('common.optionNotExist'),
    }));
    if (addressFieldIds.includes(field.fieldId)) {
      // 地址类型字段，解析代码替换成省市区
      const addressArr = (field?.fieldValue as string).split('-') || [];
      const value = addressArr.length
        ? `${getCityPath(addressArr[0])}-${addressArr.filter((e, i) => i > 0).join('-')}`
        : '-';
      customFieldAttr[field.fieldId] = value;
    } else if (industryFieldIds.includes(field.fieldId)) {
      // 行业类型字段，解析代码替换成行业名称
      customFieldAttr[field.fieldId] = field.fieldValue ? getIndustryPath(field.fieldValue as string) : '-';
    } else if (timeFieldIds.includes(field.fieldId)) {
      // 时间类型字段，格式化时间显示
      customFieldAttr[field.fieldId] = formatTimeValue(
        field.fieldValue as string,
        fields.find((f) => f.id === field.fieldId)?.dateType
      );
    } else if (options && options.length > 0) {
      let name: string | string[] = '';
      if (dataSourceFieldIds.includes(field.fieldId)) {
        // 处理数据源字段，需要赋值为数组
        if (typeof field.fieldValue === 'string' || typeof field.fieldValue === 'number') {
          // 单选
          name = [options.find((e) => e.id === field.fieldValue)?.name || t('common.optionNotExist')];
        } else {
          // 多选
          name = options.filter((e) => field.fieldValue?.includes(e.id)).map((e) => e.name);
        }
      } else if (typeof field.fieldValue === 'string' || typeof field.fieldValue === 'number') {
        // 若值是单个字符串/数字
        name = options.find((e) => e.id === field.fieldValue)?.name || t('common.optionNotExist');
      } else {
        // 若值是数组
        name = options.filter((e) => field.fieldValue?.includes(e.id)).map((e) => e.name);
        if (Array.isArray(name) && name.length === 0) {
          name = [t('common.optionNotExist')];
        }
      }
      customFieldAttr[field.fieldId] = name || [t('common.optionNotExist')];
    } else {
      // 其他类型字段，直接赋值
      customFieldAttr[field.fieldId] = field.fieldValue;
    }
  });

  return {
    ...item,
    ...customFieldAttr,
    ...businessFieldAttr,
    ...subTableFieldInfo,
  };
}
