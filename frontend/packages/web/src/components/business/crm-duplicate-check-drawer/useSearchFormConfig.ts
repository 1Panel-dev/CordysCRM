import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';

import { FilterFormItem } from '@/components/pure/crm-advance-filter/type';
import { getFormConfigApiMap } from '@/components/business/crm-form-create/config';

import useFormCreateAdvanceFilter from '@/hooks/useFormCreateAdvanceFilter';

export default function useSearchFormConfig() {
  // 客户公海共用表单
  const customerConfig = [FormDesignKeyEnum.SEARCH_ADVANCED_CUSTOMER, FormDesignKeyEnum.SEARCH_ADVANCED_PUBLIC];
  // 联系人表单
  const customerContactConfig = [FormDesignKeyEnum.SEARCH_ADVANCED_CONTACT];
  // 线索线索池共用表单
  const clueConfig = [FormDesignKeyEnum.SEARCH_ADVANCED_CLUE_POOL, FormDesignKeyEnum.SEARCH_ADVANCED_CLUE];
  // 商机表单
  const opportunityConfig = [FormDesignKeyEnum.SEARCH_ADVANCED_OPPORTUNITY];

  const formScopedConfig = [
    FormDesignKeyEnum.SEARCH_ADVANCED_OPPORTUNITY,
    FormDesignKeyEnum.SEARCH_ADVANCED_CUSTOMER,
    FormDesignKeyEnum.SEARCH_ADVANCED_CONTACT,
    FormDesignKeyEnum.SEARCH_ADVANCED_CLUE,
  ];

  const searchFieldConfigType = [
    FieldTypeEnum.INPUT,
    FieldTypeEnum.PHONE,
    FieldTypeEnum.DATA_SOURCE_MULTIPLE,
    FieldTypeEnum.DATA_SOURCE,
    FieldTypeEnum.SERIAL_NUMBER,
  ];

  // 表单类型配置映射表
  const configMap: Partial<Record<FormDesignKeyEnum, FormDesignKeyEnum[]>> = {
    [FormDesignKeyEnum.CUSTOMER]: customerConfig,
    [FormDesignKeyEnum.CUSTOMER_CONTACT]: customerContactConfig,
    [FormDesignKeyEnum.CLUE]: clueConfig,
    [FormDesignKeyEnum.BUSINESS]: opportunityConfig,
  };

  const { getFilterListConfig } = useFormCreateAdvanceFilter();

  // 最终自定义字段对应的类型Map
  const searchFieldMap = ref<Record<string, FilterFormItem[]>>({});

  async function initSearchFormConfig() {
    try {
      const uniqueConfigs = [...new Set(formScopedConfig)];

      const promises = uniqueConfigs.map(async (value) => {
        const configKey = Object.keys(configMap).find((key) =>
          (configMap[key as FormDesignKeyEnum] as FormDesignKeyEnum[]).includes(value)
        );

        if (!configKey) return;

        const res = await getFormConfigApiMap[configKey as FormDesignKeyEnum]();
        const result = getFilterListConfig(res);
        const customFieldsFilterConfig = result.filter((e) => searchFieldConfigType.includes(e.type));

        configMap[configKey as FormDesignKeyEnum]?.forEach((configValue) => {
          searchFieldMap.value[configValue] = customFieldsFilterConfig;
        });
      });

      await Promise.all(promises);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }
  return {
    initSearchFormConfig,
    searchFieldMap,
  };
}
