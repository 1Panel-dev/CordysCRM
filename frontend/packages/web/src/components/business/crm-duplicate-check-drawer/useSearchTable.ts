import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';
import type { CommonList } from '@lib/shared/models/common';
import type { ModuleField } from '@lib/shared/models/customer';

import { FilterFormItem } from '@/components/pure/crm-advance-filter/type';
import type { CrmDataTableColumn } from '@/components/pure/crm-table/type';
import useTable from '@/components/pure/crm-table/useTable';
import CrmTableButton from '@/components/pure/crm-table-button/index.vue';

import {
  getGlobalCluePoolList,
  getGlobalCustomerContactList,
  getGlobalCustomerList,
  getGlobalOpenSeaCustomerList,
  getGlobalSearchClueList,
  globalSearchOptPage,
} from '@/api/modules';
import { lastOpportunitySteps } from '@/config/opportunity';
import useOpenNewPage from '@/hooks/useOpenNewPage';

import { ClueRouteEnum, CustomerRouteEnum, OpportunityRouteEnum } from '@/enums/routeEnum';

export type SearchTableKey =
  | FormDesignKeyEnum.SEARCH_ADVANCED_CLUE
  | FormDesignKeyEnum.SEARCH_ADVANCED_CUSTOMER
  | FormDesignKeyEnum.SEARCH_ADVANCED_CONTACT
  | FormDesignKeyEnum.SEARCH_ADVANCED_PUBLIC
  | FormDesignKeyEnum.SEARCH_ADVANCED_CLUE_POOL
  | FormDesignKeyEnum.SEARCH_ADVANCED_OPPORTUNITY;

export interface SearchTableProps {
  searchTableKey: Ref<SearchTableKey>;
  fieldList: Ref<FilterFormItem[]>;
  selectedFieldIdList: Ref<string[]>;
}

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

export default async function useSearchTable(props: SearchTableProps) {
  const { openNewPage } = useOpenNewPage();
  const { t } = useI18n();

  function openNewPageClue(row: any) {
    openNewPage(ClueRouteEnum.CLUE_MANAGEMENT, {
      id: row.id,
      transitionType: row.transitionType,
      name: row.name,
    });
  }
  function openNewPageCluePool(row: any) {
    openNewPage(ClueRouteEnum.CLUE_MANAGEMENT_POOL, {
      id: row.id,
      name: row.name,
      poolId: row.poolId,
    });
  }

  function openNewPageOpportunity(row: any, isCustomer = false) {
    const customerParams = {
      customerId: row.customerId,
      inCustomerPool: row.inCustomerPool,
      poolId: row.poolId,
    };
    const opportunityParams = {
      id: row.id,
      opportunityName: row.name,
    };
    openNewPage(OpportunityRouteEnum.OPPORTUNITY, !isCustomer ? opportunityParams : customerParams);
  }

  function openNewPageCustomer(row: any) {
    openNewPage(CustomerRouteEnum.CUSTOMER_INDEX, {
      id: row.id,
    });
  }

  function openNewPageCustomerSea(row: any) {
    openNewPage(CustomerRouteEnum.CUSTOMER_OPEN_SEA, {
      id: row.id,
      poolId: row.poolId,
    });
  }

  function openNewPageCustomerContact(row: any) {
    openNewPage(CustomerRouteEnum.CUSTOMER_CONTACT, {
      id: row.customerId,
    });
  }

  const getNewPageMap: Record<SearchTableKey, (data: any, value?: any) => void> = {
    [FormDesignKeyEnum.SEARCH_ADVANCED_CLUE]: openNewPageClue,
    [FormDesignKeyEnum.SEARCH_ADVANCED_CUSTOMER]: openNewPageCustomer,
    [FormDesignKeyEnum.SEARCH_ADVANCED_CONTACT]: openNewPageCustomerContact,
    [FormDesignKeyEnum.SEARCH_ADVANCED_PUBLIC]: openNewPageCustomerSea,
    [FormDesignKeyEnum.SEARCH_ADVANCED_CLUE_POOL]: openNewPageCluePool,
    [FormDesignKeyEnum.SEARCH_ADVANCED_OPPORTUNITY]: openNewPageOpportunity,
  };

  const systemFieldList = computed(() => [
    { dataIndex: 'departmentId', title: t('org.department'), type: FieldTypeEnum.INPUT },
    { dataIndex: 'stage', title: t('opportunity.stage'), type: FieldTypeEnum.INPUT },
    {
      dataIndex: 'poolName',
      title:
        props.searchTableKey.value === FormDesignKeyEnum.SEARCH_ADVANCED_PUBLIC
          ? t('module.customer.openSeaName')
          : t('module.clue.name'),
      type: FieldTypeEnum.INPUT,
    },
  ]);

  const displayedColumnList = computed<FilterFormItem[]>(() => {
    if (!props.searchTableKey.value) return [];
    // 转换成同属性后查重
    const selectedFieldKeyList = props.selectedFieldIdList.value.map(
      (item) => props.fieldList.value.find((i) => i.id === item)?.dataIndex
    );
    const displayedColumnKeyList = [
      ...new Set([...fixedFieldKeyListMap[props.searchTableKey.value], ...selectedFieldKeyList]),
    ];
    return displayedColumnKeyList
      .map((item) => [...props.fieldList.value, ...systemFieldList.value].find((i) => i.dataIndex === item))
      .filter(Boolean) as FilterFormItem[];
  });

  const createTimeColumn = {
    title: t('common.createTime'),
    key: 'createTime',
    width: 180,
    ellipsis: {
      tooltip: true,
    },
  };

  const columns = computed<CrmDataTableColumn[]>(() => {
    const resultColumns: CrmDataTableColumn[] = displayedColumnList.value.map((field) => {
      // 名称
      if (field.dataIndex === 'name' && props.searchTableKey.value !== FormDesignKeyEnum.SEARCH_ADVANCED_CONTACT) {
        return {
          title: field.title,
          width: 200,
          key: field.dataIndex,
          ellipsis: {
            tooltip: true,
          },
          render: (row: any) => {
            if (!row.hasPermission) return row.name;
            return h(
              CrmTableButton,
              {
                onClick: () => {
                  getNewPageMap[props.searchTableKey.value](row);
                },
              },
              { default: () => row.name, trigger: () => row.name }
            );
          },
        };
      }

      if (field.dataIndex === 'customerId') {
        return {
          title: field.title,
          width: 200,
          key: field.dataIndex,
          ellipsis: {
            tooltip: true,
          },
          render: (row: any) => {
            if (!row.hasPermission) return row.customerName;
            return h(
              CrmTableButton,
              {
                onClick: () => {
                  if (props.searchTableKey.value === FormDesignKeyEnum.SEARCH_ADVANCED_CONTACT) {
                    getNewPageMap[props.searchTableKey.value](row);
                  } else if (props.searchTableKey.value === FormDesignKeyEnum.SEARCH_ADVANCED_OPPORTUNITY) {
                    getNewPageMap[props.searchTableKey.value](row, true);
                  }
                },
              },
              { default: () => row.customerName, trigger: () => row.customerName }
            );
          },
        };
      }

      // 部门
      if (field.dataIndex === 'departmentId') {
        return {
          title: field.title,
          width: 130,
          key: 'departmentName',
          ellipsis: {
            tooltip: true,
          },
        };
      }

      // 负责人
      if (field.dataIndex === 'owner') {
        return {
          title: field.title,
          width: 150,
          key: 'ownerName',
          ellipsis: {
            tooltip: true,
          },
        };
      }

      if (field.dataIndex === 'stage') {
        return {
          title: field.title,
          width: 100,
          key: field.dataIndex,
          render: (row: any) => {
            const step = lastOpportunitySteps.find((e: any) => e.value === row.stage);
            return step ? step.label : '-';
          },
        };
      }

      if (field.type === FieldTypeEnum.DATA_SOURCE_MULTIPLE) {
        return {
          title: field.title,
          width: 150,
          key: field.dataIndex,
          isTag: true,
        } as CrmDataTableColumn;
      }

      return {
        title: field.title,
        width: 150,
        key: field.dataIndex,
        ellipsis: {
          tooltip: true,
        },
        isTag: field.dataIndex === 'products',
      } as CrmDataTableColumn;
    });

    // 除了线索池和公海，其他的有创建时间
    if (
      ![FormDesignKeyEnum.SEARCH_ADVANCED_CLUE_POOL, FormDesignKeyEnum.SEARCH_ADVANCED_PUBLIC].includes(
        props.searchTableKey.value
      )
    ) {
      return [...resultColumns, createTimeColumn];
    }
    return resultColumns;
  });

  const api = computed(() => {
    return getSearchListApiMap[props.searchTableKey.value] ?? (() => Promise.resolve([]));
  });
  const useTableRes = useTable(
    (params) => api.value(params),
    {
      showPagination: true,
      columns: columns.value,
      showSetting: false,
      hiddenRefresh: true,
      hiddenAllScreen: true,
    },
    (item) => {
      const fieldMap = new Map(item.moduleFields?.map((field: ModuleField) => [field.fieldId, field.fieldValue]));
      const fieldAttr: Record<string, any> = Object.fromEntries(
        displayedColumnList.value
          .filter((i: FilterFormItem) => i.id && fieldMap.has(i.id))
          .map((i: FilterFormItem) => [i.dataIndex, fieldMap.get(i.id)!])
      );

      return {
        ...item,
        ...fieldAttr,
      };
    }
  );

  return {
    useTableRes,
    columns,
    openNewPageOpportunity,
    openNewPageClue,
  };
}
