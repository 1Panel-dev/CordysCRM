import { cloneDeep } from 'lodash-es';
import dayjs from 'dayjs';

import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';
import { CommonList } from '@lib/shared/models/common';
import { DefaultSearchSetFormModel } from '@lib/shared/models/system/module';

import {
  getGlobalCluePoolList,
  getGlobalCustomerContactList,
  getGlobalCustomerList,
  getGlobalOpenSeaCustomerList,
  getGlobalSearchClueList,
  getSearchConfig,
  globalSearchOptPage,
} from '@/api/modules';
import { lastOpportunitySteps } from '@/config/opportunity';
import { hasAnyPermission } from '@/utils/permission';

import { defaultSearchSetFormModel, lastScopedOptions, ScopedOptions, scopedOptions, SearchTableKey } from './config';
import { getFormConfigApiMap } from '@cordys/web/src/components/business/crm-form-create/config';

export const getSearchListApiMap: Record<SearchTableKey, (data: any) => Promise<CommonList<any>>> = {
  [FormDesignKeyEnum.SEARCH_ADVANCED_CLUE]: getGlobalSearchClueList,
  [FormDesignKeyEnum.SEARCH_ADVANCED_CUSTOMER]: getGlobalCustomerList,
  [FormDesignKeyEnum.SEARCH_ADVANCED_CONTACT]: getGlobalCustomerContactList,
  [FormDesignKeyEnum.SEARCH_ADVANCED_PUBLIC]: getGlobalOpenSeaCustomerList,
  [FormDesignKeyEnum.SEARCH_ADVANCED_CLUE_POOL]: getGlobalCluePoolList,
  [FormDesignKeyEnum.SEARCH_ADVANCED_OPPORTUNITY]: globalSearchOptPage,
};

// 固定展示字段
export const fixedFieldKeyListMap: Record<SearchTableKey, string[]> = {
  [FormDesignKeyEnum.SEARCH_ADVANCED_CLUE]: ['name', 'owner', 'departmentId', 'products'], // 公司名称 、负责人、部门、意向产品
  [FormDesignKeyEnum.SEARCH_ADVANCED_CLUE_POOL]: ['name', 'products', 'poolName'], // 公司名称 、意向产品、线索池名称
  [FormDesignKeyEnum.SEARCH_ADVANCED_CUSTOMER]: ['name', 'owner', 'departmentId'], // 客户名称、负责人、部门
  [FormDesignKeyEnum.SEARCH_ADVANCED_CONTACT]: ['customerId', 'name', 'phone', 'owner', 'departmentId'], // 客户名称、姓名、手机号、负责人、部门
  [FormDesignKeyEnum.SEARCH_ADVANCED_PUBLIC]: ['name', 'poolName'], // 客户名称、公海名称
  [FormDesignKeyEnum.SEARCH_ADVANCED_OPPORTUNITY]: ['name', 'customerId', 'owner', 'departmentId', 'products', 'stage'], // 商机名称、客户名称、负责人、部门、意向产品、商机阶段
};

export default function useSearchFormConfig() {
  const { t } = useI18n();

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
  const searchResultMap = ref<Record<SearchTableKey, Record<string, any>>>({
    [FormDesignKeyEnum.SEARCH_ADVANCED_CLUE]: {
      describe: [],
    },
    [FormDesignKeyEnum.SEARCH_ADVANCED_CUSTOMER]: {
      describe: [],
    },
    [FormDesignKeyEnum.SEARCH_ADVANCED_CONTACT]: {
      describe: [],
    },
    [FormDesignKeyEnum.SEARCH_ADVANCED_PUBLIC]: {
      describe: [],
    },
    [FormDesignKeyEnum.SEARCH_ADVANCED_CLUE_POOL]: {
      describe: [],
    },
    [FormDesignKeyEnum.SEARCH_ADVANCED_OPPORTUNITY]: {
      describe: [],
    },
  });

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
          allFieldMap.value[configValue] = res.fields.map((field) => {
            return {
              id: field.id,
              key: field.businessKey ?? field.id,
              label: field.name,
            };
          });
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

      configList.value = sortSetting
        .map((val: any) => optionsMap.get(val))
        .filter((option) => option && lastScopedOptions.value.includes(option)) as ScopedOptions[];
      formModel.value = cloneDeep(res);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  const systemDesc = computed(() => {
    return (searchTableKey: SearchTableKey) => {
      return [
        {
          label: t('workbench.duplicateCheck.department'),
          key: 'departmentId',
          type: FieldTypeEnum.INPUT,
        },
        {
          label: t('workbench.duplicateCheck.opportunityStage'),
          key: 'stage',
          type: FieldTypeEnum.INPUT,
        },
        {
          label:
            searchTableKey === FormDesignKeyEnum.SEARCH_ADVANCED_PUBLIC
              ? t('workbench.duplicateCheck.openSeaName')
              : t('workbench.duplicateCheck.leadPoolName'),
          key: 'poolName',
          type: FieldTypeEnum.INPUT,
        },
      ];
    };
  });

  const displayedDescList = computed(() => {
    return (searchTableKey: SearchTableKey): any[] => {
      if (!searchTableKey) return [];
      // 配置选择的id
      const selectedFieldIdList = formModel.value.searchFields[searchTableKey];

      // 全量fields列表
      const fieldList = allFieldMap.value[searchTableKey];

      // 转换成同属性后查重
      const selectedFieldKeyList =
        selectedFieldIdList?.map((id: string) => fieldList.find((i) => i.id === id)?.key) || [];

      const displayedColumnKeyList = [...new Set([...fixedFieldKeyListMap[searchTableKey], ...selectedFieldKeyList])];
      return displayedColumnKeyList
        .map((item) => [...fieldList, ...systemDesc.value(searchTableKey)].find((i) => i.key === item))
        .filter(Boolean);
    };
  });

  const createTimeDesc = {
    label: t('common.createTime'),
    key: 'createTime',
    valueSlotName: 'render',
    render: (row: any) => {
      return dayjs(row.createTime).format('YYYY-MM-DD');
    },
  };

  function initSearchListConfig() {
    configList.value.forEach((e) => {
      const resultDesc = displayedDescList.value(e.value as SearchTableKey).map((field) => {
        // TODO  不知道是否展示 这个位置和上边的大标题重复
        if (field.key === 'name' && e.value !== FormDesignKeyEnum.SEARCH_ADVANCED_CONTACT) {
          return {
            label: field.label,
            key: field.key,
            valueSlotName: 'render',
          };
        }

        // 部门
        if (field.key === 'departmentId') {
          return {
            label: field.label,
            key: 'departmentName',
          };
        }

        // 负责人
        if (field.key === 'owner') {
          return {
            label: field.label,
            key: 'ownerName',
          };
        }

        if (field.key === 'stage') {
          return {
            label: field.label,
            key: field.key,
            valueSlotName: 'render',
            render: (row: any) => {
              const step = lastOpportunitySteps.find((item: any) => item.value === row.stage);
              return step ? step.label : '-';
            },
          };
        }

        if (field.type === FieldTypeEnum.DATA_SOURCE_MULTIPLE) {
          return {
            label: field.label,
            key: field.key,
            valueSlotName: 'render',
            render: (row: any) => {
              // TODO 多选数据源返回来的数据不确定
              return row[field.key]?.join('；') ?? '-';
            },
          };
        }

        return {
          label: field.label,
          key: field.key,
          valueSlotName: 'render',
          render: (row: any) => {
            return field.key === 'products' ? row.products.join('；') : row[field.id];
          },
        };
      });
      if (![FormDesignKeyEnum.SEARCH_ADVANCED_CLUE_POOL, FormDesignKeyEnum.SEARCH_ADVANCED_PUBLIC].includes(e.value)) {
        searchResultMap.value[e.value as SearchTableKey].describe = [...resultDesc, createTimeDesc];
      } else {
        searchResultMap.value[e.value as SearchTableKey].describe = resultDesc;
      }
    });
  }

  return {
    initSearchFormConfig,
    initSearchDetail,
    searchFieldMap,
    allFieldMap,
    configList,
    formModel,
    searchResultMap,
    getSearchListApiMap,
    initSearchListConfig,
  };
}
