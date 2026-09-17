import { type MaybeRef, ref, unref, watch } from 'vue';

import type { FormConfig, FormDetailTabOption } from '@lib/shared/models/system/module';

import { getFormDetailTabOptions } from '@/api/modules';

// 同一表单的关联字段候选在当前会话内复用，重复打开详情时无需再次等待接口。
const detailTabOptionsCache = new Map<string, FormDetailTabOption[]>();
const detailTabOptionsRequestMap = new Map<string, Promise<FormDetailTabOption[]>>();

export default function useFormDetailTabAvailability(
  formConfig: MaybeRef<FormConfig | undefined>,
  currentFormId: MaybeRef<string | undefined>
) {
  // undefined 表示关联字段有效性仍在校验，避免详情页先展示部分标签后再补齐。
  const availableDetailTabIds = ref<Set<string>>();

  let requestVersion = 0;
  async function initAvailableDetailTabs() {
    const currentVersion = ++requestVersion;
    const config = unref(formConfig);
    const formId = unref(currentFormId);
    if (!config || !formId) {
      // 表单配置尚未返回时，不提前渲染系统标签，避免后续关联标签补齐造成闪动。
      availableDetailTabIds.value = undefined;
      return;
    }

    const detailTabs = config.detailTabs || [];
    const relatedTabs = detailTabs.filter((item) => !item.internalKey && item.relatedForm?.id && item.relatedField?.id);

    if (!relatedTabs.length) {
      availableDetailTabIds.value = new Set();
      return;
    }

    try {
      let relatedFormOptions = detailTabOptionsCache.get(formId);
      const cachedRelatedFormOptions = relatedFormOptions || [];
      const canUseCache =
        relatedFormOptions !== undefined &&
        relatedTabs.every((detailTab) => {
          const relatedForm = cachedRelatedFormOptions.find((item) => item.id === detailTab.relatedForm!.id);
          return relatedForm?.sourceTypeFields.some((field) => field.id === detailTab.relatedField!.id);
        });
      if (!canUseCache) {
        // 首次读取候选项时统一等待，避免先显示系统标签、再异步补齐关联标签。
        availableDetailTabIds.value = undefined;
        let request = detailTabOptionsRequestMap.get(formId);
        if (!request) {
          request = getFormDetailTabOptions(formId);
          detailTabOptionsRequestMap.set(formId, request);
        }
        relatedFormOptions = await request;
        detailTabOptionsCache.set(formId, relatedFormOptions);
        detailTabOptionsRequestMap.delete(formId);
      }

      const availableTabs = relatedTabs
        .filter((detailTab) => {
          const relatedForm = (relatedFormOptions || []).find((item) => item.id === detailTab.relatedForm!.id);
          return relatedForm?.sourceTypeFields.some((field) => field.id === detailTab.relatedField!.id);
        })
        .map((detailTab) => detailTab.id);

      if (currentVersion === requestVersion) {
        availableDetailTabIds.value = new Set(availableTabs);
      }
    } catch (error) {
      detailTabOptionsRequestMap.delete(formId);
      // 关联表单无查看权限、字段已删除或候选接口请求失败时均不展示关联标签。
      if (currentVersion === requestVersion) {
        availableDetailTabIds.value = new Set();
      }
    }
  }

  watch(
    [() => unref(formConfig)?.detailTabs, () => unref(currentFormId)],
    () => {
      initAvailableDetailTabs();
    },
    { deep: true, immediate: true }
  );

  return {
    availableDetailTabIds,
  };
}
