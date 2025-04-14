import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';
import { getCityPath } from '@lib/shared/method';
import type { ModuleField } from '@lib/shared/models/customer';

import { getFormConfigApiMap } from '@cordys/web/src/components/business/crm-form-create/config';
import type { FormCreateField } from '@cordys/web/src/components/business/crm-form-create/types';

export default async function useFormCreateTransform(formKey: FormDesignKeyEnum) {
  const { t } = useI18n();

  const fieldList = ref<FormCreateField[]>([]);
  // 存储地址类型字段集合
  const addressFieldIds = ref<string[]>([]);
  // 业务字段集合
  const businessFieldIds = ref<string[]>([]);
  // 数据源字段集合
  const dataSourceFieldIds = ref<string[]>([]);

  async function initFormConfig() {
    try {
      const res = await getFormConfigApiMap[formKey]();
      fieldList.value = res.fields.map((field) => {
        if (field.type === FieldTypeEnum.LOCATION) {
          addressFieldIds.value.push(field.businessKey || field.id);
        } else if (field.type === FieldTypeEnum.DATA_SOURCE) {
          dataSourceFieldIds.value.push(field.businessKey || field.id);
        }
        if (field.businessKey) {
          businessFieldIds.value.push(field.businessKey);
        }
        return {
          ...field,
        };
      });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  await initFormConfig();

  const descriptionMap: Partial<Record<FormDesignKeyEnum, string[]>> = {
    [FormDesignKeyEnum.CUSTOMER]: ['owner', 'reservedDays'],
    [FormDesignKeyEnum.CLUE]: ['owner', 'reservedDays'],
  };
  const descriptionKeys = descriptionMap[formKey] || [];
  // 系统内置字段
  const systemFieldMap: Record<string, any> = {
    reservedDays: t('common.remainingBelong'),
  };

  /**
   * 转换特殊字段值
   * @param key 字段 id 标识
   * @param value 字段值
   */
  function specialDescriptionValueTransform(key: string, value: string | number) {
    if (key === 'reservedDays') {
      // 处理剩余归属天数
      return Number.isNaN(Number(value)) ? '-' : `${value}${t('common.dayUnit')}`;
    }
    return value;
  }

  function transformFormData(data: Record<string, any>, optionMap?: Record<string, any[]>) {
    let formData: Record<string, any> = { ...data, description: [] };
    const businessFieldAttr: Record<string, any> = {};
    const customFieldAttr: Record<string, any> = {};
    businessFieldIds.value.forEach((fieldId) => {
      const options = optionMap?.[fieldId];
      let name: string | string[] = '';
      if (dataSourceFieldIds.value.includes(fieldId)) {
        if (typeof data[fieldId] === 'string') {
          name = [options?.find((e) => e.id === data[fieldId])?.name];
        } else {
          name = options?.filter((e) => data[fieldId].includes(e.id)).map((e) => e.name) || [];
        }
      } else {
        name = options?.find((e) => e.id === data[fieldId])?.name;
      }
      if (name) {
        businessFieldAttr[fieldId] = name;
      } else if (addressFieldIds.value.includes(fieldId)) {
        // 地址类型字段，解析代码替换成省市区
        const address = data[fieldId]?.split('-');
        const value = `${getCityPath(address[0])}-${address[1]}`;
        businessFieldAttr[fieldId] = value;
      }
    });
    data.moduleFields?.forEach((field: ModuleField) => {
      const options = optionMap?.[field.fieldId];
      if (options) {
        // 若字段值是选项值，则取选项值的name
        if (Array.isArray(field.fieldValue)) {
          customFieldAttr[field.fieldId] = options.filter((e) => field.fieldValue.includes(e.id)).map((e) => e.name);
        } else {
          customFieldAttr[field.fieldId] = options.find((e) => e.id === field.fieldValue)?.name;
        }
      } else if (addressFieldIds.value.includes(field.fieldId)) {
        // 地址类型字段，解析代码替换成省市区
        const address = (field?.fieldValue as string)?.split('-');
        const value = `${getCityPath(address[0])}-${address[1]}`;
        customFieldAttr[field.fieldId] = value;
      } else {
        customFieldAttr[field.fieldId] = field.fieldValue;
      }
      // 数据源字段赋值
      if (dataSourceFieldIds.value.includes(field.fieldId)) {
        customFieldAttr[field.fieldId] = Array.isArray(field.fieldValue) ? field.fieldValue : [field.fieldValue];
      }
    });
    formData = {
      ...formData,
      ...businessFieldAttr,
      ...customFieldAttr,
    };
    descriptionKeys.forEach((key) => {
      const item = fieldList.value.find((field) => field.businessKey === key || field.id === key);
      if (item) {
        formData.description.push({
          label: item.name,
          value: formData[key] || '-',
        });
      } else if (systemFieldMap[key]) {
        formData.description.push({
          label: systemFieldMap[key],
          value: specialDescriptionValueTransform(key, formData[key]),
        });
      }
    });

    return formData;
  }

  return {
    fieldList,
    transformFormData,
  };
}
