import { computed, type MaybeRef, unref } from 'vue';

import type { FormConfig, FormDetailTabConfig } from '@lib/shared/models/system/module';

import type { TabContentItem } from '@/components/business/crm-tab-setting/type';

import { systemDetailTabTableConfigMap } from '@/hooks/useFormDetailTabTable';
import { hasAllPermission } from '@/utils/permission';

export interface FormDetailTabItem extends TabContentItem {
  detailTab?: FormDetailTabConfig;
}

export default function useFormDetailTabs(
  formConfig: MaybeRef<FormConfig | undefined>,
  staticTabList: MaybeRef<TabContentItem[]> = [],
  availableDetailTabIds?: MaybeRef<Set<string> | undefined>
) {
  const isDetailTabAvailabilityReady = computed(
    () => !availableDetailTabIds || unref(availableDetailTabIds) !== undefined
  );

  const systemDetailTabList = computed<FormDetailTabItem[]>(() => {
    if (!isDetailTabAvailabilityReady.value) {
      return [];
    }
    const detailTabs = unref(formConfig)?.detailTabs || [];
    const systemTabConfigMap = new Map(
      detailTabs.filter((detailTab) => detailTab.internalKey).map((detailTab) => [detailTab.internalKey!, detailTab])
    );
    const systemTabList = unref(staticTabList).map((tab) => {
      const detailTab = systemTabConfigMap.get(tab.internalKey || String(tab.name));
      if (!detailTab) {
        return tab;
      }
      return {
        ...tab,
        // name 保持内容切换标识不变，展示文案与开关以表单配置为准。
        tab: detailTab.name || tab.tab,
        enable: detailTab.enable,
        disabled: !detailTab.enable,
        detailTab,
      };
    });
    return systemTabList;
  });

  const customDetailTabList = computed<FormDetailTabItem[]>(() => {
    if (!isDetailTabAvailabilityReady.value) {
      return [];
    }
    const detailTabs = unref(formConfig)?.detailTabs || [];
    const availableIds = availableDetailTabIds ? unref(availableDetailTabIds) : undefined;
    const customTabList = detailTabs
      .filter(
        (detailTab) =>
          !detailTab.internalKey &&
          detailTab.relatedForm?.id &&
          detailTab.relatedField?.id &&
          detailTab.name.trim() &&
          (!availableIds || availableIds.has(detailTab.id))
      )
      .map((detailTab) => ({
        // 同一关联表单下的关联字段不可重复，可作为运行态 Tab 的稳定唯一标识。
        name: `detail-tab:${detailTab.relatedForm!.id}:${detailTab.relatedField!.id}`,
        tab: detailTab.name,
        enable: detailTab.enable,
        disabled: !detailTab.enable,
        detailTab,
        permission: systemDetailTabTableConfigMap[
          detailTab.relatedForm!.id as keyof typeof systemDetailTabTableConfigMap
        ]?.permission || ['CUSTOM_FORM:READ'],
      }));
    return customTabList;
  });

  const detailTabList = computed<FormDetailTabItem[]>(() => [
    ...systemDetailTabList.value,
    ...customDetailTabList.value,
  ]);
  const enabledSystemDetailTabList = computed<FormDetailTabItem[]>(() =>
    systemDetailTabList.value.filter((tab) => tab.enable)
  );
  const enabledDetailTabList = computed<FormDetailTabItem[]>(() =>
    detailTabList.value.filter((tab) => tab.enable && hasAllPermission(tab.permission || []))
  );
  return {
    detailTabList,
    enabledDetailTabList,
    systemDetailTabList,
    enabledSystemDetailTabList,
    customDetailTabList,
  };
}
