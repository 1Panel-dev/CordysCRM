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
      <div class="mb-[4px] flex h-[24px] w-full items-center justify-between px-[8px] text-[12px]">
        <div class="one-line-text font-medium text-[var(--text-n1)]"> {{ t('common.tabConfig') }} </div>
        <n-button text type="primary" size="tiny" :disabled="!hasChange" @click="handleReset">
          {{ t('common.resetDefault') }}
        </n-button>
      </div>
      <VueDraggable v-model="cachedData" handle=".sort-handle" @change="handleChange" @end="handleChange">
        <div v-for="element in cachedData" :key="element.name" class="crm-tab-setting-item px-[8px]">
          <div class="flex min-w-0 flex-1 items-center gap-[8px]">
            <CrmIcon type="iconicon_move" class="sort-handle cursor-move text-[var(--text-n4)]" :size="12" />
            <div class="min-w-0 flex-1 overflow-hidden">
              <n-tooltip trigger="hover" placement="top">
                <template #trigger>
                  <span class="one-line-text block text-[12px]">
                    {{ element.tab }}
                  </span>
                </template>
                {{ element.tab }}
              </n-tooltip>
            </div>
            <n-switch
              v-model:value="element.enable"
              :disabled="element.disabled"
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
  import { NButton, NPopover, NScrollbar, NSwitch, NTooltip } from 'naive-ui';
  import { VueDraggable } from 'vue-draggable-plus';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { isArraysEqualWithOrder } from '@lib/shared/method/equal';

  import useLocalForage from '@/hooks/useLocalForage';
  import { hasAllPermission } from '@/utils/permission';

  import type { ContentTabsMap, TabContentItem } from './type';

  const { t } = useI18n();

  const props = defineProps<{
    tabList: TabContentItem[];
    settingKey: string;
  }>();

  const emit = defineEmits<{
    (e: 'init', value: TabContentItem[]): void;
  }>();

  // 个人展示配置不能直接复用 props 中的对象，否则切换开关会改写全局表单标签配置。
  const cachedData = ref<TabContentItem[]>(
    props.tabList.filter((e) => hasAllPermission(e?.permission || [])).map((tab) => ({ ...tab }))
  );

  const popoverVisible = ref(false);

  async function saveTabsToLocal(list: TabContentItem[]) {
    const { setItem } = useLocalForage();
    try {
      await setItem(
        props.settingKey,
        {
          tabList: list,
        },
        true
      );
    } catch (e) {
      // eslint-disable-next-line no-console
      console.error(e);
    }
  }

  async function getTabsFromLocal() {
    const { getItem } = useLocalForage();
    try {
      const tabsMap = await getItem<ContentTabsMap>(props.settingKey, true);
      return tabsMap;
    } catch (e) {
      // eslint-disable-next-line no-console
      console.error(e);
      return undefined;
    }
  }

  const enableTabs = computed<TabContentItem[]>(() => cachedData.value.filter((e) => e.enable));
  const newTabList = computed<TabContentItem[]>(() =>
    props.tabList.filter((e) => hasAllPermission(e?.permission || [])).map((tab) => ({ ...tab }))
  );

  let loadVersion = 0;
  async function loadTab() {
    const currentVersion = ++loadVersion;
    const currentTabList = newTabList.value;
    try {
      const tabsMap = await getTabsFromLocal();
      if (currentVersion !== loadVersion) {
        return;
      }

      const localTabs = tabsMap?.tabList || [];
      const currentTabMap = new Map(currentTabList.map((tab) => [tab.name, tab]));
      // 保留仍由表单配置开启的标签及个人开关/排序；表单关闭或删除的标签直接移除。
      const retainedTabs = localTabs
        .filter((tab) => currentTabMap.has(tab.name))
        .map((localTab) => ({
          ...currentTabMap.get(localTab.name)!,
          enable: localTab.enable,
        }));
      const retainedNames = new Set(retainedTabs.map((tab) => tab.name));
      // 新增标签，以及表单配置“关闭后重新开启”的标签，均默认在个人配置中开启。
      const addedTabs = currentTabList
        .filter((tab) => !retainedNames.has(tab.name))
        .map((tab) => ({ ...tab, enable: true }));
      const finalTabs = [...retainedTabs, ...addedTabs];

      cachedData.value = finalTabs;
      if (!isArraysEqualWithOrder(localTabs, finalTabs)) {
        await saveTabsToLocal(finalTabs);
      }
      emit('init', enableTabs.value);
    } catch (e) {
      // eslint-disable-next-line no-console
      console.error(e);
      cachedData.value = newTabList.value.map((tab) => ({
        ...tab,
        enable: tab.enable,
      }));
      emit('init', enableTabs.value);
    }
  }

  const hasChange = ref(false);
  async function handleUpdateShow(show: boolean) {
    if (!show && hasChange.value) {
      // 拖拽结束后统一提交最终排序，确保详情页与本地缓存使用同一份数组。
      await saveTabsToLocal(cachedData.value);
      emit('init', enableTabs.value);
      hasChange.value = false;
    }
  }

  function handleChange() {
    hasChange.value = true;
  }

  function handleReset() {
    cachedData.value = newTabList.value.map((tab) => ({ ...tab, enable: true }));
    hasChange.value = true;
  }

  onBeforeMount(async () => {
    await loadTab();
  });

  watch(
    () => newTabList.value,
    () => {
      loadTab();
    },
    { deep: true }
  );
</script>

<style lang="less">
  .crm-tab-setting-popover {
    padding: 4px !important;
    width: 200px;
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
