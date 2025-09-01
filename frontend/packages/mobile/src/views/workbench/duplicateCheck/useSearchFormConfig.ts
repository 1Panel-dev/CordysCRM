import { cloneDeep } from 'lodash-es';

import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { DefaultSearchSetFormModel } from '@lib/shared/models/system/module';

import { getSearchConfig } from '@/api/modules';
import { hasAnyPermission } from '@/utils/permission';

import { defaultSearchSetFormModel, ScopedOptions, scopedOptions } from './config';
import { getFormConfigApiMap } from '@cordys/web/src/components/business/crm-form-create/config';

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

  const configPermissionMap: Partial<Record<FormDesignKeyEnum, string[]>> = {
    [FormDesignKeyEnum.SEARCH_ADVANCED_OPPORTUNITY]: ['OPPORTUNITY_MANAGEMENT:READ'],
    [FormDesignKeyEnum.SEARCH_ADVANCED_CUSTOMER]: ['CUSTOMER_MANAGEMENT:READ', 'CUSTOMER_MANAGEMENT_POOL:READ'],
    [FormDesignKeyEnum.SEARCH_ADVANCED_CONTACT]: ['CUSTOMER_MANAGEMENT_CONTACT:READ'],
    [FormDesignKeyEnum.SEARCH_ADVANCED_CLUE]: ['CLUE_MANAGEMENT:READ', 'CLUE_MANAGEMENT_POOL:READ'],
  };

  // 最终自定义字段对应的类型Map
  const searchFieldMap = ref<Record<string, any[]>>({});
  const allFieldMap = ref<Record<string, any[]>>({});
  const configList = ref<ScopedOptions[]>([]);
  const formModel = ref<DefaultSearchSetFormModel>(cloneDeep(defaultSearchSetFormModel));
  async function initSearchFormConfig() {
    try {
      const uniqueConfigs = [...new Set(formScopedConfig)];

      const promises = uniqueConfigs.map(async (value) => {
        const configKey = Object.keys(configMap).find((key) =>
          (configMap[key as FormDesignKeyEnum] as FormDesignKeyEnum[]).includes(value)
        );

        if (!configKey) return;
        if (!hasAnyPermission(configPermissionMap[value as FormDesignKeyEnum])) return;

        const res = await getFormConfigApiMap[configKey as FormDesignKeyEnum]();
        const customFieldsFilterConfig = res.fields.filter((e) => searchFieldConfigType.includes(e.type));

        configMap[configKey as FormDesignKeyEnum]?.forEach((configValue) => {
          searchFieldMap.value[configValue] = customFieldsFilterConfig;
          allFieldMap.value[configValue] = res.fields;
        });
      });

      await Promise.all(promises);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  async function initSearchDetail() {
    try {
      const res = await getSearchConfig();
      const { sortSetting } = res;

      const optionsMap = new Map(scopedOptions.map((item) => [item.value, item]));
      configList.value = sortSetting.map((val: any) => optionsMap.get(val)).filter(Boolean) as ScopedOptions[];

      formModel.value = cloneDeep(res);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  return {
    initSearchFormConfig,
    initSearchDetail,
    searchFieldMap,
    allFieldMap,
    configList,
    formModel,
  };
}
