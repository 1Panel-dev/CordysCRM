import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';
import type { CommonList } from '@lib/shared/models/common';

import { FilterFormItem } from '@/components/pure/crm-advance-filter/type';
import type { CrmDataTableColumn } from '@/components/pure/crm-table/type';
import useTable from '@/components/pure/crm-table/useTable';
import CrmTableButton from '@/components/pure/crm-table-button/index.vue';

import { globalSearchOptPage } from '@/api/modules';
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
  searchTableKey: SearchTableKey;
  fieldList: FilterFormItem[];
  selectedFieldKeyList: string[]; // 匹配字段
}

export const getSearchListApiMap: Record<SearchTableKey, (data: any) => Promise<CommonList<any>>> = {
  [FormDesignKeyEnum.SEARCH_ADVANCED_CLUE]: globalSearchOptPage,
  [FormDesignKeyEnum.SEARCH_ADVANCED_CUSTOMER]: globalSearchOptPage,
  [FormDesignKeyEnum.SEARCH_ADVANCED_CONTACT]: globalSearchOptPage,
  [FormDesignKeyEnum.SEARCH_ADVANCED_PUBLIC]: globalSearchOptPage,
  [FormDesignKeyEnum.SEARCH_ADVANCED_CLUE_POOL]: globalSearchOptPage,
  [FormDesignKeyEnum.SEARCH_ADVANCED_OPPORTUNITY]: globalSearchOptPage,
};

// 固定展示字段 TODO lmy 对照一下字段
export const fixedFieldKeyListMap: Record<SearchTableKey, string[]> = {
  [FormDesignKeyEnum.SEARCH_ADVANCED_CLUE]: ['name', 'ownerName', 'departmentId', 'productNameList'], // 公司名称 、负责人、部门、意向产品
  [FormDesignKeyEnum.SEARCH_ADVANCED_CUSTOMER]: ['name', 'ownerName', 'departmentId'], // 客户名称、负责人、部门
  [FormDesignKeyEnum.SEARCH_ADVANCED_CONTACT]: ['customerName', 'name', 'phone', 'ownerName', 'departmentId'], // 客户名称、姓名、手机号、负责人、部门
  [FormDesignKeyEnum.SEARCH_ADVANCED_PUBLIC]: ['name', 'xxx'], // 客户名称、公海名称
  [FormDesignKeyEnum.SEARCH_ADVANCED_CLUE_POOL]: ['name', 'productNameList', 'xxx'], // 公司名称 、意向产品、线索池名称
  [FormDesignKeyEnum.SEARCH_ADVANCED_OPPORTUNITY]: [
    'name',
    'customerName',
    'ownerName',
    'departmentId',
    'productNames',
    'stage',
  ], // 商机名称、客户名称、负责人、部门、意向产品、商机阶段
};

export default async function useSearchTable(props: SearchTableProps) {
  const { openNewPage } = useOpenNewPage();
  const { t } = useI18n();

  const loading = ref(false);

  const displayedColumnList = computed(() => {
    // TODO lmy props.selectedFieldKeyList里是id，fixedFieldKeyListMap[props.searchTableKey]里是businessKey 需要转换成同属性的
    const displayedColumnKeyList = [
      ...new Set([...fixedFieldKeyListMap[props.searchTableKey], ...props.selectedFieldKeyList]),
    ];
    return props.fieldList.filter((item) => displayedColumnKeyList.includes(item.dataIndex as string));
  });

  const createTimeColumn = {
    title: t('common.createTime'),
    key: 'createTime',
    width: 120,
    ellipsis: {
      tooltip: true,
    },
  };

  let columns: CrmDataTableColumn[] = [];

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
      opportunityName: row.opportunityName,
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

  async function getColumns() {
    try {
      loading.value = true;

      // TODO lmy 处理column
      columns = displayedColumnList.value.map((field) => {
        // 名称
        if (field.dataIndex === 'name' && props.searchTableKey !== FormDesignKeyEnum.SEARCH_ADVANCED_CONTACT) {
          return {
            title: field.title,
            width: 200,
            key: field.dataIndex,
            render: (row: any) => {
              if (!row.hasPermission) return row.name;
              return h(
                CrmTableButton,
                {
                  onClick: () => {
                    getNewPageMap[props.searchTableKey](row);
                  },
                },
                { default: () => row.name, trigger: () => row.name }
              );
            },
          } as CrmDataTableColumn;
        }

        if (field.dataIndex === 'customerName') {
          return {
            title: field.title,
            width: 200,
            key: field.dataIndex,
            render: (row: any) => {
              if (!row.hasPermission) return row.customerName;
              return h(
                CrmTableButton,
                {
                  onClick: () => {
                    if (props.searchTableKey === FormDesignKeyEnum.SEARCH_ADVANCED_CONTACT) {
                      getNewPageMap[props.searchTableKey](row);
                    } else if (props.searchTableKey === FormDesignKeyEnum.SEARCH_ADVANCED_OPPORTUNITY) {
                      getNewPageMap[props.searchTableKey](row, true);
                    }
                  },
                },
                { default: () => row.customerName, trigger: () => row.customerName }
              );
            },
          } as CrmDataTableColumn;
        }

        if (field.dataIndex === 'departmentId') {
          return {
            title: field.title,
            width: 200,
            key: field.dataIndex,
            render: (row: any) => row.departmentName || '-',
          } as CrmDataTableColumn;
        }

        if (field.dataIndex === 'stage') {
          return {
            title: field.title,
            width: 200,
            key: field.dataIndex,
            render: (row) => {
              const step = lastOpportunitySteps.find((e: any) => e.value === row.stage);
              return step ? step.label : '-';
            },
          } as CrmDataTableColumn;
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
        } as CrmDataTableColumn;
      });
      // 除了线索池和公海，其他的有创建时间
      if (
        ![FormDesignKeyEnum.SEARCH_ADVANCED_CLUE_POOL, FormDesignKeyEnum.SEARCH_ADVANCED_PUBLIC].includes(
          props.searchTableKey
        )
      ) {
        columns = [...columns, createTimeColumn];
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }
  await getColumns();

  const useTableRes = useTable(getSearchListApiMap[props.searchTableKey], {
    showPagination: true,
    columns,
    showSetting: false,
    hiddenTotal: true,
    hiddenRefresh: true,
    hiddenAllScreen: true,
  });

  return {
    loading,
    useTableRes,
  };
}
