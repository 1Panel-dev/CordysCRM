import { ref, watch } from 'vue';

import { getProductOptions } from '@/api/modules';

import { FilterOption } from 'naive-ui/es/data-table/src/interface';

export const remoteFilterApiMap: Record<string, () => Promise<any>> = {
  products: getProductOptions,
};

/**
 * Hook: 根据 filterApiKey 获取远程 filterOptions
 * @param filterApiKey string
 */
export default function useRemoteFilterOptions(filterApiKey?: string) {
  const filterOptions = ref<FilterOption[]>([]);

  async function fetchFilterOptions() {
    if (!filterApiKey) return;

    const api = remoteFilterApiMap[filterApiKey];
    if (!api) {
      return [];
    }

    try {
      const result = await api();
      filterOptions.value =
        (result || []).map((e: any) => ({
          value: e.id,
          label: e.name,
        })) ?? [];
    } catch (err) {
      // eslint-disable-next-line no-console
      console.error(err);
    }
  }

  watch(() => filterApiKey, fetchFilterOptions, { immediate: true });

  return {
    filterOptions,
    refresh: fetchFilterOptions,
  };
}
