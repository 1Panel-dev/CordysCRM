import { TabPaneProps } from 'naive-ui';

import { CustomerSearchTypeEnum } from '@lib/shared/enums/customerEnum';
import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { OpportunitySearchTypeEnum } from '@lib/shared/enums/opportunityEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';
import type { CustomerTabHidden } from '@lib/shared/models/customer';

import { getClueTab, getCustomerContactTab, getCustomerTab, getOptTab } from '@/api/modules';

export type TabType =
  | FormDesignKeyEnum.CUSTOMER
  | FormDesignKeyEnum.BUSINESS
  | FormDesignKeyEnum.CLUE
  | FormDesignKeyEnum.CONTACT;
export default function useHiddenTab(type?: TabType) {
  const { t } = useI18n();

  const tabApiMap: Record<TabType, () => Promise<CustomerTabHidden>> = {
    [FormDesignKeyEnum.CUSTOMER]: getCustomerTab,
    [FormDesignKeyEnum.CONTACT]: getCustomerContactTab,
    [FormDesignKeyEnum.BUSINESS]: getOptTab,
    [FormDesignKeyEnum.CLUE]: getClueTab,
  };

  const allClueTabList: TabPaneProps[] = [
    {
      name: CustomerSearchTypeEnum.ALL,
      tab: t('clue.allClues'),
    },
    {
      name: CustomerSearchTypeEnum.SELF,
      tab: t('clue.myClues'),
    },
    {
      name: CustomerSearchTypeEnum.DEPARTMENT,
      tab: t('clue.departmentClues'),
    },
  ];

  const allContactTabList: TabPaneProps[] = [
    {
      name: CustomerSearchTypeEnum.ALL,
      tab: t('customer.contacts.all'),
    },
    {
      name: CustomerSearchTypeEnum.SELF,
      tab: t('customer.contacts.mine'),
    },
    {
      name: CustomerSearchTypeEnum.DEPARTMENT,
      tab: t('customer.contacts.department'),
    },
  ];

  const allCustomerTabList: TabPaneProps[] = [
    {
      name: CustomerSearchTypeEnum.ALL,
      tab: t('customer.all'),
    },
    {
      name: CustomerSearchTypeEnum.SELF,
      tab: t('customer.mine'),
    },
    {
      name: CustomerSearchTypeEnum.DEPARTMENT,
      tab: t('customer.deptCustomer'),
    },
    {
      name: CustomerSearchTypeEnum.CUSTOMER_COLLABORATION,
      tab: t('customer.cooperationCustomer'),
    },
  ];

  const allOpportunityTabList: TabPaneProps[] = [
    {
      name: OpportunitySearchTypeEnum.ALL,
      tab: t('opportunity.allOpportunities'),
    },
    {
      name: OpportunitySearchTypeEnum.SELF,
      tab: t('opportunity.myOpportunities'),
    },
    {
      name: OpportunitySearchTypeEnum.DEPARTMENT,
      tab: t('opportunity.departmentOpportunities'),
    },
    {
      name: OpportunitySearchTypeEnum.OPPORTUNITY_SUCCESS,
      tab: t('opportunity.convertedOpportunities'),
    },
  ];

  const tabListMap: Record<TabType, TabPaneProps[]> = {
    [FormDesignKeyEnum.CUSTOMER]: allCustomerTabList,
    [FormDesignKeyEnum.CONTACT]: allContactTabList,
    [FormDesignKeyEnum.BUSINESS]: allOpportunityTabList,
    [FormDesignKeyEnum.CLUE]: allClueTabList,
  };

  const tabList = ref<TabPaneProps[]>([]);

  async function initTab() {
    try {
      if (!type) return;
      const result = await tabApiMap[type]();
      const { all, dept } = result;
      tabList.value = tabListMap[type].filter((e) => {
        if (e.name === 'ALL') return !!all;
        if (e.name === 'DEPARTMENT') return !!dept;
        return true;
      });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  return {
    tabList,
    initTab,
  };
}
