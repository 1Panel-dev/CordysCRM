import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import type { FormDesignConfigDetailParams } from '@lib/shared/models/system/module';

import type { FilterFormItem } from '@/components/pure/crm-advance-filter/type';
import type { FormCreateField } from '@/components/business/crm-form-create/types';

export default function useFormCreateFilter() {
  const customFieldsFilterConfig = ref<FilterFormItem[]>([]);
  // 获取配置属性
  function getFilterListConfig(res: FormDesignConfigDetailParams) {
    const getConfigProps = (field: FormCreateField) => {
      if ([FieldTypeEnum.SELECT, FieldTypeEnum.SELECT_MULTIPLE].includes(field.type)) {
        return {
          selectProps: {
            options: field.options,
            multiple: true,
          },
        };
      }
      // TODO: 其他类型
      return {};
    };
    return (res.fields || []).reduce((acc: FilterFormItem[], field: FormCreateField) => {
      if (![FieldTypeEnum.TEXTAREA, FieldTypeEnum.PICTURE, FieldTypeEnum.DIVIDER].includes(field.type)) {
        acc.push({
          title: field.name,
          dataIndex: field.businessKey ?? field.id,
          type: field.type,
          ...getConfigProps(field),
        });
      }
      return acc;
    }, []);
  }

  return {
    getFilterListConfig,
    customFieldsFilterConfig,
  };
}
