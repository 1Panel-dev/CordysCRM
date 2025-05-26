import { TabPaneProps } from 'naive-ui';

import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import type { CustomerTabHidden } from '@lib/shared/models/customer';

import { getClueTab, getCustomerContactTab, getCustomerTab, getOptTab } from '@/api/modules';

export type TabType =
  | FormDesignKeyEnum.CUSTOMER
  | FormDesignKeyEnum.BUSINESS
  | FormDesignKeyEnum.CLUE
  | FormDesignKeyEnum.CONTACT;
export default function useHiddenTab(tabData: TabPaneProps[], type: TabType) {
  const activeTab = ref();

  const tabApiMap: Record<TabType, () => Promise<CustomerTabHidden>> = {
    [FormDesignKeyEnum.CUSTOMER]: getCustomerTab,
    [FormDesignKeyEnum.CONTACT]: getCustomerContactTab,
    [FormDesignKeyEnum.BUSINESS]: getOptTab,
    [FormDesignKeyEnum.CLUE]: getClueTab,
  };

  const tabList = ref<TabPaneProps[]>([]);

  async function initTab() {
    try {
      const result = await tabApiMap[type]();
      const { all, dept } = result;
      tabList.value = tabData.filter((e) => {
        if (e.name === 'ALL') return !!all;
        if (e.name === 'DEPARTMENT') return !!dept;
        return true;
      });

      nextTick(() => {
        activeTab.value = tabList.value[0]?.name;
      });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  onBeforeMount(() => {
    initTab();
  });

  return {
    tabList,
    activeTab,
  };
}
