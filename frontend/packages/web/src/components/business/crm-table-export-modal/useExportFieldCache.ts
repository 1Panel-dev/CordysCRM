import { computed, type Ref } from 'vue';

import type { ExportTableColumnItem } from '@lib/shared/models/common';

import useLocalForage from '@/hooks/useLocalForage';
import { useUserStore } from '@/store';

interface UseExportFieldCacheOptions {
  type: Ref<string>;
  customFormId: Ref<string | undefined>;
  columns: Ref<ExportTableColumnItem[]>;
}

const EXPORT_FIELD_CACHE_PREFIX = 'crm-table-export-fields';
const FOLLOW_RESOURCE_NAME_LEGACY_KEY = 'name';
const FOLLOW_RESOURCE_NAME_KEYS = ['customerId', 'clueId'];
const FOLLOW_RESOURCE_NAME_CACHE_TYPES = ['followRecord', 'followPlan'];

function migrateCachedFieldKeys(type: string, keys: string[]) {
  if (!FOLLOW_RESOURCE_NAME_CACHE_TYPES.includes(type) || !keys.includes(FOLLOW_RESOURCE_NAME_LEGACY_KEY)) {
    return keys;
  }
  // 旧版导出将客户名称、公司名称合并为一个字段，升级后还原为两个独立字段
  return Array.from(
    new Set(keys.flatMap((key) => (key === FOLLOW_RESOURCE_NAME_LEGACY_KEY ? FOLLOW_RESOURCE_NAME_KEYS : [key])))
  );
}

export default function useExportFieldCache(options: UseExportFieldCacheOptions) {
  const userStore = useUserStore();
  const { getItem, setItem } = useLocalForage();

  const storageKey = computed(() => {
    const userId = userStore.userInfo.id || 'anonymous';
    const formId = options.type.value === 'customForm' ? options.customFormId.value || 'default' : 'default';
    return `${EXPORT_FIELD_CACHE_PREFIX}:${userId}:${options.type.value}:${formId}`;
  });

  async function getCachedFieldKeys() {
    try {
      const cache = await getItem<{ keys?: string[] } | string[]>(storageKey.value);
      const keys = Array.isArray(cache) ? cache : cache?.keys;
      return Array.isArray(keys) ? keys.filter((key) => typeof key === 'string') : [];
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
      return [];
    }
  }

  async function getSelectedListByCache() {
    const cacheKeys = await getCachedFieldKeys();
    if (!cacheKeys.length) {
      return [];
    }

    const columnMap = new Map(options.columns.value.map((item) => [item.key, item]));
    return migrateCachedFieldKeys(options.type.value, cacheKeys)
      .map((key) => columnMap.get(key))
      .filter(Boolean) as ExportTableColumnItem[];
  }

  async function saveSelectedListCache(selectedList: ExportTableColumnItem[]) {
    const keys = selectedList.map((item) => item.key);
    await setItem(storageKey.value, { keys });
  }

  return {
    getSelectedListByCache,
    saveSelectedListCache,
  };
}
