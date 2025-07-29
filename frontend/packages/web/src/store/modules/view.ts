import { defineStore } from 'pinia';
import { TabPaneProps } from 'naive-ui';

import type { ViewItem } from '@lib/shared/models/view';

import { ConditionsItem } from '@/components/pure/crm-advance-filter/type';
import { internalConditionsMap, viewApiMap } from '@/components/business/crm-view-select/config';

import { TabType } from '@/hooks/useHiddenTab';
import useLocalForage from '@/hooks/useLocalForage';
// 视图
const useViewStore = defineStore('view', {
  persist: true,
  state: () => ({
    customViews: [] as ViewItem[],
    internalViews: [] as ViewItem[],
  }),
  actions: {
    getInternalKey(type: TabType) {
      return `internal_views_${type}`;
    },

    async getInternalViews(key: string): Promise<ViewItem[]> {
      const { getItem } = useLocalForage();
      const data = await getItem<ViewItem[]>(key);
      return data ?? [];
    },

    async setInternalViews(key: string, data: ViewItem[]) {
      const { setItem } = useLocalForage();
      await setItem(key, data);
    },

    async loadInternalViews(type: TabType, internalList: TabPaneProps[]) {
      // TODO 拖拽后的存储顺序和internalList顺序
      const stored = await this.getInternalViews(this.getInternalKey(type));
      this.internalViews = internalList.map((i) => {
        const storedItem = stored.find((item) => item.id === i.name);
        return {
          id: i.name as string,
          name: i.tab as string,
          enable: storedItem?.enable ?? true,
          fixed: storedItem?.fixed ?? true,
          type: 'internal',
          searchMode: 'AND',
          list: internalConditionsMap[i.name as string],
        };
      });
      this.setInternalViews(this.getInternalKey(type), this.internalViews);
    },

    async loadCustomViews(type: TabType) {
      try {
        this.customViews = await viewApiMap.list[type]();
      } catch (error) {
        // eslint-disable-next-line no-console
        console.log(error);
      }
    },

    // 获取详情
    async getViewDetail(type: TabType, option: ViewItem) {
      let res;
      if (option.type === 'internal') {
        res = option;
      } else {
        try {
          res = await viewApiMap.detail[type](option.id);
          res.list = res.conditions.map((item: ConditionsItem) => ({ ...item, dataIndex: item.name }));
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      }
      return {
        name: res.name,
        list: res.list,
        searchMode: res.searchMode,
      };
    },

    // 固定/取消固定
    async toggleFixed(type: TabType, option: ViewItem) {
      if (option.type === 'internal') {
        const index = this.internalViews.findIndex((i: ViewItem) => i.id === option.id);
        if (index !== -1) this.internalViews[index].fixed = !this.internalViews[index].fixed;
        this.setInternalViews(this.getInternalKey(type), this.internalViews);
      } else {
        try {
          await viewApiMap.fixed[type](option.id);
          this.loadCustomViews(type);
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      }
    },

    async toggleEnabled(type: TabType, option: ViewItem) {
      if (option.type === 'internal') {
        const index = this.internalViews.findIndex((i: ViewItem) => i.id === option.id);
        if (index !== -1) this.internalViews[index].enable = !this.internalViews[index].enable;
        this.setInternalViews(this.getInternalKey(type), this.internalViews);
      } else {
        try {
          await viewApiMap.enable[type](option.id);
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      }
    },
  },
});

export default useViewStore;
