import { type MaybeRef, ref, unref, watch } from 'vue';

import type { FormConfig } from '@lib/shared/models/system/module';

// 同一表单的关联标签配置在当前会话内复用，重复打开详情时无需重复计算。
const detailTabIdsCache = new Map<string, Set<string>>();

export default function useFormDetailTabAvailability(
  formConfig: MaybeRef<FormConfig | undefined>,
  currentFormId: MaybeRef<string | undefined>
) {
  // undefined 表示关联字段有效性仍在校验，避免详情页先展示部分标签后再补齐。
  const availableDetailTabIds = ref<Set<string>>();

  function initAvailableDetailTabs() {
    const config = unref(formConfig);
    const formId = unref(currentFormId);
    if (!config || !formId) {
      // 表单配置尚未返回时，不提前渲染系统标签，避免后续关联标签补齐造成闪动。
      availableDetailTabIds.value = undefined;
      return;
    }

    const cachedDetailTabIds = detailTabIdsCache.get(formId);
    if (cachedDetailTabIds) {
      availableDetailTabIds.value = cachedDetailTabIds;
      return;
    }

    const detailTabIds = new Set(
      (config.detailTabs || [])
        .filter((item) => !item.internalKey && item.relatedForm?.id && item.relatedField?.id)
        .map((item) => item.id)
    );
    detailTabIdsCache.set(formId, detailTabIds);
    availableDetailTabIds.value = detailTabIds;
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
