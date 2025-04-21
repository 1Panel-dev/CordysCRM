import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import type { FormDesignConfigDetailParams } from '@lib/shared/models/system/module';

import type { FilterFormItem } from '@/components/pure/crm-advance-filter/type';
import type { FormCreateField } from '@/components/business/crm-form-create/types';

const internalFilterKeyMap: Record<FormDesignKeyEnum, string[]> = {
  [FormDesignKeyEnum.BUSINESS]: ['opportunityName', 'opportunityCustomer', 'opportunitySource'],
  [FormDesignKeyEnum.CLUE]: ['clueName', 'clueSource', 'clueProgress'],
  [FormDesignKeyEnum.CLUE_TRANSITION_BUSINESS]: [],
  [FormDesignKeyEnum.CLUE_TRANSITION_CUSTOMER]: [],
  [FormDesignKeyEnum.CLUE_POOL]: ['clueName', 'clueSource', 'clueProgress'],
  [FormDesignKeyEnum.CONTACT]: [],
  [FormDesignKeyEnum.CUSTOMER_CONTACT]: [],
  [FormDesignKeyEnum.CUSTOMER]: ['customerName', 'customerLevel', 'customerSource', 'customerType'],
  [FormDesignKeyEnum.FOLLOW_RECORD_CLUE]: [],
  [FormDesignKeyEnum.FOLLOW_PLAN_CLUE]: [],
  [FormDesignKeyEnum.FOLLOW_RECORD_CUSTOMER]: [],
  [FormDesignKeyEnum.FOLLOW_PLAN_CUSTOMER]: [],
  [FormDesignKeyEnum.FOLLOW_RECORD_BUSINESS]: [],
  [FormDesignKeyEnum.FOLLOW_PLAN_BUSINESS]: [],
  [FormDesignKeyEnum.PRODUCT]: [],
  [FormDesignKeyEnum.CUSTOMER_OPEN_SEA]: ['customerName', 'customerLevel', 'customerSource', 'customerType'],
};

export default function useFormCreateFilter(formKey: FormDesignKeyEnum) {
  const customFieldsFilterConfig = ref<FilterFormItem[]>([]);
  // 获取配置属性
  function getFilterListConfig(res: FormDesignConfigDetailParams) {
    const getConfigProps = (field: FormCreateField) => {
      if (field.type === FieldTypeEnum.SELECT) {
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
      if (internalFilterKeyMap[formKey].includes(field.internalKey ?? '')) {
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
