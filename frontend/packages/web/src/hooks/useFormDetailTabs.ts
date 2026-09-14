import { computed, type MaybeRef, unref } from 'vue';

import type { FormConfig, FormDetailTabConfig } from '@lib/shared/models/system/module';

import type { TabContentItem } from '@/components/business/crm-tab-setting/type';

export interface FormDetailTabItem extends TabContentItem {
  detailTab?: FormDetailTabConfig;
}

export default function useFormDetailTabs(
  formConfig: MaybeRef<FormConfig | undefined>,
  staticTabList: MaybeRef<TabContentItem[]> = []
) {
  const systemDetailTabList = computed<FormDetailTabItem[]>(() => {
    const detailTabs = unref(formConfig)?.detailTabs || [];
    const systemTabConfigMap = new Map(
      detailTabs
        .filter((detailTab) => detailTab.origin === 'SYSTEM' && detailTab.systemTabKey)
        .map((detailTab) => [detailTab.systemTabKey!, detailTab])
    );
    const systemTabList = unref(staticTabList).map((tab) => {
      const detailTab = systemTabConfigMap.get(String(tab.name));
      if (!detailTab) {
        return tab;
      }
      return {
        ...tab,
        enable: detailTab.enable,
        disabled: !detailTab.enable,
        detailTab,
      };
    });
    return systemTabList;
  });

  const customDetailTabList = computed<FormDetailTabItem[]>(() => {
    const detailTabs = unref(formConfig)?.detailTabs || [];
    const customTabList = detailTabs
      .filter(
        (detailTab) =>
          detailTab.origin === 'CUSTOM' &&
          detailTab.relatedFormType &&
          detailTab.relatedFormId &&
          detailTab.relatedFieldId &&
          detailTab.name.trim()
      )
      .map((detailTab) => ({
        name: `detail-tab:${detailTab.id}`,
        tab: detailTab.name,
        enable: detailTab.enable,
        disabled: !detailTab.enable,
        detailTab,
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
  const systemTabConfigVersion = computed(() =>
    enabledSystemDetailTabList.value
      .map((tab) => String(tab.name))
      .sort()
      .join('|')
  );

  return {
    detailTabList,
    systemDetailTabList,
    enabledSystemDetailTabList,
    systemTabConfigVersion,
    customDetailTabList,
  };
}
