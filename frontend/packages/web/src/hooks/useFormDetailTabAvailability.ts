import { type MaybeRef, ref, unref, watch } from 'vue';

import type { FormConfig, FormDetailTabConfig } from '@lib/shared/models/system/module';

import { getCustomFormList } from '@/api/modules';
import { systemDetailTabTableConfigMap } from '@/hooks/useFormDetailTabTable';
import { hasAnyPermission } from '@/utils/permission';

export function getDetailTabAvailabilityKey(tab: FormDetailTabConfig) {
  return `detail-tab:${tab.relatedForm?.id}:${tab.relatedField?.id}`;
}

export default function useFormDetailTabAvailability(
  formConfig: MaybeRef<FormConfig | undefined>,
  currentFormId: MaybeRef<string | undefined>
) {
  // undefined 表示关联字段有效性仍在校验，避免详情页先展示部分标签后再补齐。
  const availableDetailTabIds = ref<Set<string>>();
  let requestToken = 0;

  async function initAvailableDetailTabs() {
    const token = ++requestToken;
    const config = unref(formConfig);
    const formId = unref(currentFormId);
    if (!config || !formId) {
      // 表单配置尚未返回时，不提前渲染系统标签，避免后续关联标签补齐造成闪动。
      availableDetailTabIds.value = undefined;
      return;
    }

    const tabs = (config.detailTabs || []).filter(
      (item) => !item.internalKey && item.relatedForm?.id && item.relatedField?.id
    );
    const isSystemForm = (id: string) => Object.prototype.hasOwnProperty.call(systemDetailTabTableConfigMap, id);
    availableDetailTabIds.value = undefined;
    let visibleCustomFormIds = new Set<string>();
    if (tabs.some((tab) => !isSystemForm(tab.relatedForm!.id)) && hasAnyPermission(['CUSTOM_FORM:READ'])) {
      try {
        // 与自定义表单列表使用同一可见范围，不缓存用户权限结果。
        const forms = await getCustomFormList();
        visibleCustomFormIds = new Set(forms.map((form) => form.id));
      } catch {
        // 可见范围获取失败时，仅保留系统表单标签。
      }
    }
    if (token !== requestToken) return;
    availableDetailTabIds.value = new Set(
      tabs
        .filter((tab) => isSystemForm(tab.relatedForm!.id) || visibleCustomFormIds.has(tab.relatedForm!.id))
        .map(getDetailTabAvailabilityKey)
    );
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
