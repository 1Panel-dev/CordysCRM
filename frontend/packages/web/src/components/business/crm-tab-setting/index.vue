<template>
  <n-popover
    v-model:show="popoverVisible"
    trigger="click"
    placement="bottom-end"
    class="crm-tab-setting-popover"
    @update:show="handleUpdateShow"
  >
    <template #trigger>
      <n-button secondary>
        <CrmIcon class="mr-[4px] text-[var(--text-n1)]" type="iconicon_set_up" :size="16" />
        {{ t('common.tabConfig') }}
      </n-button>
    </template>
    <n-scrollbar class="max-h-[416px] py-[4px]">
      <div class="mb-[4px] flex h-[24px] w-[175px] items-center justify-between px-[8px] text-[12px]">
        <div class="font-medium text-[var(--text-n1)]"> {{ t('common.tabConfig') }} </div>
        <n-button text type="primary" size="tiny" :disabled="!hasChange" @click="handleReset">
          {{ t('common.resetDefault') }}
        </n-button>
      </div>
      <VueDraggable v-model="cachedData" handle=".sort-handle" @change="handleChange">
        <div v-for="element in cachedData" :key="element.name" class="crm-tab-setting-item px-[8px]">
          <div class="flex flex-1 items-center gap-[8px]">
            <CrmIcon type="iconicon_move" class="sort-handle cursor-move text-[var(--text-n4)]" :size="12" />
            <div class="flex flex-1 items-center overflow-hidden">
              <span class="one-line-text text-[12px]">
                {{ element.tab }}
              </span>
            </div>
            <n-switch
              v-model:value="element.enable"
              :disabled="cachedData.filter((e) => e.enable).length <= 1 && element.enable"
              size="small"
              :rubber-band="false"
              @update:value="handleChange"
            />
          </div>
        </div>
      </VueDraggable>
    </n-scrollbar>
  </n-popover>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NButton, NPopover, NScrollbar, NSwitch } from 'naive-ui';
  import { VueDraggable } from 'vue-draggable-plus';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { isArraysEqualWithOrder } from '@lib/shared/method/equal';

  import useLocalForage from '@/hooks/useLocalForage';

  import type { ContentTabsMap, TabContentItem } from './type';

  const { t } = useI18n();

  const props = defineProps<{
    tabList: TabContentItem[];
    settingKey: string;
  }>();

  const emit = defineEmits<{
    (e: 'init', value: TabContentItem[]): void;
  }>();

  const cachedData = ref<TabContentItem[]>(props.tabList);

  const popoverVisible = ref(false);

  async function saveTabsToLocal(list: TabContentItem[]) {
    const { getItem, setItem } = useLocalForage();
    try {
      const tabsMap = await getItem<ContentTabsMap>(props.settingKey, true);
      const newTabsMap = {
        tabList: list,
        backupTabList: list,
      };
      if (tabsMap) {
        const isEqual = isArraysEqualWithOrder(tabsMap.backupTabList, list);
        if (!isEqual) {
          await setItem(props.settingKey, newTabsMap, true);
        }
      } else {
        await setItem(props.settingKey, newTabsMap, true);
      }
    } catch (e) {
      // eslint-disable-next-line no-console
      console.error(e);
    }
  }

  async function getTabsFromLocal() {
    const { getItem } = useLocalForage();
    try {
      const tabsMap = await getItem<ContentTabsMap>(props.settingKey, true);
      return tabsMap ? tabsMap.tabList : [];
    } catch (e) {
      // eslint-disable-next-line no-console
      console.error(e);
      return [];
    }
  }

  const enableTabs = computed<TabContentItem[]>(() => cachedData.value.filter((e) => e.enable));

  async function loadTab() {
    try {
      const localTabs = await getTabsFromLocal();
      const currentTabMap = new Map(props.tabList.map((tab) => [tab.name, tab]));

      if (localTabs.length > 0) {
        // 使用本地存储的顺序，但只包含当前仍然存在的标签页
        const mergedTabs = localTabs
          .filter((tab) => currentTabMap.has(tab.name)) // 过滤掉已删除的标签页
          .map((localTab) => ({
            ...currentTabMap.get(localTab.name)!,
            enable: localTab.enable, // 保留启用状态
          }));

        // 添加新增的标签页（在本地存储中不存在的）
        const existingNames = new Set(mergedTabs.map((tab) => tab.name));
        const newTabs = props.tabList
          .filter((tab) => !existingNames.has(tab.name))
          .map((tab) => ({ ...tab, enable: true }));

        const finalTabs = [...mergedTabs, ...newTabs];
        cachedData.value = finalTabs;

        // 如果有新增标签页或顺序变化，更新本地存储
        if (newTabs.length > 0 || !isArraysEqualWithOrder(localTabs, mergedTabs)) {
          await saveTabsToLocal(finalTabs);
        }
      } else {
        // 没有本地存储，使用默认设置
        await saveTabsToLocal(cachedData.value);
      }
    } catch (e) {
      // eslint-disable-next-line no-console
      console.error(e);
      cachedData.value = props.tabList.map((tab) => ({ ...tab, enable: true }));
    }
  }

  const hasChange = ref(false);
  function handleUpdateShow(show: boolean) {
    if (!show && hasChange.value) {
      saveTabsToLocal(cachedData.value);
      emit('init', enableTabs.value);
      hasChange.value = false;
    }
  }

  function handleChange() {
    hasChange.value = true;
  }

  function handleReset() {
    hasChange.value = false;
    loadTab();
  }

  onBeforeMount(async () => {
    await loadTab();
    emit('init', enableTabs.value);
  });

  watch(
    () => props.tabList,
    () => {
      loadTab();
    },
    { deep: true }
  );
</script>

<style lang="less">
  .crm-tab-setting-popover {
    padding: 4px !important;
    min-width: 175px;
    .crm-tab-setting-item {
      height: 28px;
      gap: 8px;
      @apply flex items-center justify-between rounded;
      &:hover {
        background: var(--text-n9);
      }
    }
  }
</style>
