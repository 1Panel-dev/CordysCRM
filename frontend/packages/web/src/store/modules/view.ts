import { defineStore } from 'pinia';
import { TabPaneProps } from 'naive-ui';

import { OperatorEnum } from '@lib/shared/enums/commonEnum';
import { CustomerSearchTypeEnum } from '@lib/shared/enums/customerEnum';
import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { OpportunitySearchTypeEnum } from '@lib/shared/enums/opportunityEnum';
import type { SortParams, TableDraggedParams } from '@lib/shared/models/common';
import type { CustomFormUserViewItem } from '@lib/shared/models/customForm';
import type { OpportunityStageConfig } from '@lib/shared/models/opportunity';
import type { ViewItem } from '@lib/shared/models/view';

import { ConditionsItem, FilterFormItem, filterOptionKeyMap } from '@/components/pure/crm-advance-filter/type';
import {
  internalConditionsMap,
  nonFixedInternalViewIds,
  viewApiMap,
} from '@/components/business/crm-view-select/config';

import { getOpportunityStageConfig } from '@/api/modules';
import { TabType } from '@/hooks/useHiddenTab';
import useLocalForage from '@/hooks/useLocalForage';
import useUserStore from '@/store/modules/user';

type ViewType = TabType | FormDesignKeyEnum.CUSTOM_FORM;

function getViewApiArgs(type: ViewType, customFormId?: string, ...args: unknown[]) {
  return type === FormDesignKeyEnum.CUSTOM_FORM ? [customFormId, ...args] : args;
}

// 视图
const useViewStore = defineStore('view', {
  persist: true,
  state: () => ({
    customViews: [] as ViewItem[],
    internalViews: [] as ViewItem[],
  }),
  actions: {
    getInternalKey(type: ViewType, customFormId?: string) {
      const scope = type === FormDesignKeyEnum.CUSTOM_FORM ? `custom-form-${customFormId}` : type;
      return `internal_views_${scope}`;
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
      return key === FormDesignKeyEnum.CUSTOMER && data === 'VISIBLE'
        ? CustomerSearchTypeEnum.CUSTOMER_COLLABORATION
        : data;
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
    async resolveTypeOptions(type: ViewType) {
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
      type: ViewType;
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
    async loadInternalViews(type: ViewType, internalList: TabPaneProps[], customFormId?: string) {
      const userStore = useUserStore();

      const storageKey = this.getInternalKey(type, customFormId);
      const stored = await this.getInternalViews(storageKey);
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
                fixed: !nonFixedInternalViewIds.has(item.name as string),
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
      if (
        type === FormDesignKeyEnum.CUSTOMER &&
        stored.some((item) => item.id === 'VISIBLE' && item.fixed && item.enable) &&
        !merged.some((item) => item.fixed && item.enable)
      ) {
        const collaborationView = merged.find((item) => item.id === CustomerSearchTypeEnum.CUSTOMER_COLLABORATION);
        if (collaborationView) {
          collaborationView.fixed = true;
          collaborationView.enable = true;
        }
      }
      // admin 不显示部门视图
      if (userStore.userInfo.id === 'admin') {
        // @ts-ignore-next-line
        this.internalViews = merged.filter((item) => item.id !== CustomerSearchTypeEnum.DEPARTMENT);
      } else {
        this.internalViews = merged;
      }
      await this.setInternalViews(storageKey, this.internalViews);
    },

    async loadCustomViews(type: ViewType, customFormId?: string) {
      try {
        const views = await viewApiMap.list[type](...getViewApiArgs(type, customFormId));
        this.customViews =
          type === FormDesignKeyEnum.CUSTOM_FORM
            ? (views as CustomFormUserViewItem[]).map((item) => ({
                ...item,
                type: 'custom',
                searchMode: 'AND',
                list: [],
              }))
            : views;
      } catch (error) {
        // eslint-disable-next-line no-console
        console.log(error);
      }
    },

    // 获取详情
    async getViewDetail(type: ViewType, option: ViewItem, customFormId?: string) {
      let res: ViewItem;
      if (option.type === 'internal') {
        res = option;
      } else {
        try {
          res = await viewApiMap.detail[type](...getViewApiArgs(type, customFormId, option.id));
          res.list = (res.conditions ?? []).map((item: ConditionsItem) => {
            const values = Array.isArray(item.value) ? item.value : [item.value];
            const options =
              res.optionMap?.[item.name as string]?.filter((i: { id: string; name: string }) =>
                values.includes(i.id)
              ) ?? [];
            const keyText = filterOptionKeyMap[item.type as FieldTypeEnum];

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
    async toggleDrag(type: ViewType, params: TableDraggedParams, customFormId?: string) {
      const isInternal = this.internalViews.some((item) => item.id === params.moveId);
      if (isInternal) {
        const movedItem = this.internalViews.splice(params.oldIndex, 1)[0];
        this.internalViews.splice(params.newIndex, 0, movedItem);
        await this.setInternalViews(this.getInternalKey(type, customFormId), this.internalViews);
      } else {
        try {
          await viewApiMap.drag[type](...getViewApiArgs(type, customFormId, params));
          await this.loadCustomViews(type, customFormId);
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      }
    },

    // 固定/取消固定
    async toggleFixed(type: ViewType, option: ViewItem, customFormId?: string) {
      if (option.type === 'internal') {
        const index = this.internalViews.findIndex((i: ViewItem) => i.id === option.id);
        if (index !== -1) this.internalViews[index].fixed = !this.internalViews[index].fixed;
        this.setInternalViews(this.getInternalKey(type, customFormId), this.internalViews);
      } else {
        try {
          await viewApiMap.fixed[type](...getViewApiArgs(type, customFormId, option.id));
          await this.loadCustomViews(type, customFormId);
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      }
    },

    async toggleEnabled(type: ViewType, option: ViewItem, customFormId?: string) {
      if (option.type === 'internal') {
        const index = this.internalViews.findIndex((i: ViewItem) => i.id === option.id);
        if (index !== -1) this.internalViews[index].enable = !this.internalViews[index].enable;
        this.setInternalViews(this.getInternalKey(type, customFormId), this.internalViews);
      } else {
        try {
          await viewApiMap.enable[type](...getViewApiArgs(type, customFormId, option.id));
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      }
    },
  },
});

export default useViewStore;
