import { defineStore } from 'pinia';
import { TabPaneProps } from 'naive-ui';

import { OperatorEnum } from '@lib/shared/enums/commonEnum';
import { CustomerSearchTypeEnum } from '@lib/shared/enums/customerEnum';
import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { OpportunitySearchTypeEnum } from '@lib/shared/enums/opportunityEnum';
import type { SortParams, TableDraggedParams } from '@lib/shared/models/common';
import type { OpportunityStageConfig } from '@lib/shared/models/opportunity';
import type { ViewItem } from '@lib/shared/models/view';

import { ConditionsItem, FilterFormItem } from '@/components/pure/crm-advance-filter/type';
import { internalConditionsMap, viewApiMap } from '@/components/business/crm-view-select/config';

import { getOpportunityStageConfig } from '@/api/modules';
import { TabType } from '@/hooks/useHiddenTab';
import useLocalForage from '@/hooks/useLocalForage';
import useUserStore from '@/store/modules/user';

const typeToKeyMap: Partial<Record<FieldTypeEnum, 'selectedRows' | 'selectedUserList'>> = {
  [FieldTypeEnum.DATA_SOURCE]: 'selectedRows',
  [FieldTypeEnum.DATA_SOURCE_MULTIPLE]: 'selectedRows',
  [FieldTypeEnum.DEPARTMENT]: 'selectedUserList',
  [FieldTypeEnum.DEPARTMENT_MULTIPLE]: 'selectedUserList',
  [FieldTypeEnum.MEMBER]: 'selectedUserList',
  [FieldTypeEnum.MEMBER_MULTIPLE]: 'selectedUserList',
};

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

    async getActiveView(key: string) {
      const { getItem } = useLocalForage();
      const data = await getItem<string>(`active-view-${key}`);
      return data;
    },

    async setActiveView(key: string, viewId: string) {
      const { setItem } = useLocalForage();
      await setItem(`active-view-${key}`, viewId);
    },

    async getViewSort(tableKey: string, viewId: string) {
      const { getItem } = useLocalForage();
      const data = await getItem<SortParams>(`view-sort-${tableKey}-${viewId}`);
      return data;
    },

    async setViewSort(tableKey: string, viewId: string, data: SortParams) {
      const { setItem } = useLocalForage();
      await setItem(`view-sort-${tableKey}-${viewId}`, data);
    },

    async getOpportunitySuccessStageOption() {
      try {
        const stageConfig: OpportunityStageConfig = await getOpportunityStageConfig();
        const successStage = stageConfig?.stageConfigList?.find((i) => i.type === 'END' && i.rate === '100');
        return [
          {
            dataIndex: 'stage',
            type: FieldTypeEnum.SELECT_MULTIPLE,
            operator: OperatorEnum.IN,
            value: [successStage?.id],
          },
        ];
      } catch (error) {
        // eslint-disable-next-line no-console
        console.log(error);
        return [
          {
            dataIndex: 'stage',
            type: FieldTypeEnum.SELECT_MULTIPLE,
            operator: OperatorEnum.IN,
            value: [],
          },
        ];
      }
    },
    async resolveTypeOptions(type: TabType) {
      switch (type) {
        case FormDesignKeyEnum.BUSINESS:
          return {
            opportunitySuccessOptions: await this.getOpportunitySuccessStageOption(),
          };
        default:
          return {};
      }
    },
    resolveInternalConditionList(params: {
      type: TabType;
      tabName: string;
      optionItem: Record<string, any>;
    }): FilterFormItem[] {
      const { type, tabName, optionItem } = params;
      // 商机成功阶段
      if (tabName === OpportunitySearchTypeEnum.OPPORTUNITY_SUCCESS) {
        return optionItem.opportunitySuccessOptions ?? [];
      }
      // 报价单我的视图
      if (type === FormDesignKeyEnum.OPPORTUNITY_QUOTATION && tabName === CustomerSearchTypeEnum.SELF) {
        return internalConditionsMap[CustomerSearchTypeEnum.SELF_QUOTATION] ?? [];
      }
      return internalConditionsMap[tabName] ?? [];
    },
    async loadInternalViews(type: TabType, internalList: TabPaneProps[]) {
      const userStore = useUserStore();

      const stored = await this.getInternalViews(this.getInternalKey(type));
      const listMap = internalList.reduce<Record<string, TabPaneProps>>((map, item) => {
        map[item.name as string] = item;
        return map;
      }, {});

      const storedIds = new Set(stored.map((i) => i.id));

      const optItem = (await this.resolveTypeOptions(type)) ?? null;
      const merged = [
        // 优先使用 stored 的顺序和设置
        ...stored
          .filter((item) => listMap[item.id])
          .map((item) => {
            const def = listMap[item.id];
            return {
              ...item,
              name: def.tab as string,
              list: this.resolveInternalConditionList({
                type,
                tabName: def.name as string,
                optionItem: optItem,
              }),
              type: 'internal',
              searchMode: item.searchMode ?? 'AND',
            } as ViewItem;
          }),

        // 添加 stored 中没有的新项
        ...internalList
          .filter((item) => !storedIds.has(item.name as string))
          .map(
            (item) =>
              ({
                id: item.name as string,
                name: item.tab as string,
                enable: true,
                fixed: true,
                type: 'internal',
                searchMode: 'AND',
                list: this.resolveInternalConditionList({
                  type,
                  tabName: item.name as string,
                  optionItem: optItem,
                }),
              } as ViewItem)
          ),
      ];
      // admin 不显示部门视图
      if (userStore.userInfo.id === 'admin') {
        this.internalViews = merged.filter((item) => item.id !== CustomerSearchTypeEnum.DEPARTMENT);
      } else {
        this.internalViews = merged;
      }
      await this.setInternalViews(this.getInternalKey(type), this.internalViews);
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
      let res: ViewItem;
      if (option.type === 'internal') {
        res = option;
      } else {
        try {
          res = await viewApiMap.detail[type](option.id);
          res.list = (res.conditions ?? []).map((item: ConditionsItem) => {
            const options =
              res.optionMap?.[item.name as string]?.filter((i: { id: string; name: string }) =>
                item.value?.includes(i.id)
              ) ?? [];
            const keyText = typeToKeyMap[item.type as FieldTypeEnum];

            return {
              ...item,
              dataIndex: item.name,
              ...(keyText ? { [keyText]: options } : {}),
            } as FilterFormItem;
          });
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
          return {};
        }
      }
      return {
        name: res.name,
        list: res.list,
        searchMode: res.searchMode,
      };
    },

    // 拖拽
    async toggleDrag(type: TabType, params: TableDraggedParams) {
      const isInternal = this.internalViews.some((item) => item.id === params.moveId);
      if (isInternal) {
        const movedItem = this.internalViews.splice(params.oldIndex, 1)[0];
        this.internalViews.splice(params.newIndex, 0, movedItem);
        await this.setInternalViews(this.getInternalKey(type), this.internalViews);
      } else {
        try {
          await viewApiMap.drag[type](params);
          this.loadCustomViews(type);
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      }
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
