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
        <CrmIcon class="mr-[4px]" type="iconicon_set_up" :size="16" />
        {{ t('common.tabConfig') }}
      </n-button>
    </template>
    <n-scrollbar class="max-h-[416px] px-[12px] py-[4px]">
      <div class="mb-[4px] flex h-[24px] w-[175px] items-center justify-between text-[12px]">
        <div class="font-medium text-[var(--text-n1)]"> {{ t('common.tabConfig') }} </div>
        <n-button text type="primary" size="tiny" :disabled="!hasChange" @click="handleReset">
          {{ t('common.resetDefault') }}
        </n-button>
      </div>
      <div
        v-for="element in cachedData"
        :key="element.name"
        class="flex h-[28px] w-[175px] items-center justify-between py-[4px]"
      >
        <div class="flex flex-1 items-center overflow-hidden">
          <span class="one-line-text text-[12px]">
            {{ element.tab }}
          </span>
        </div>
        <n-switch
          v-model:value="element.enable"
          :disabled="!element.allowClose"
          size="small"
          @update:value="handleChange"
        />
      </div>
    </n-scrollbar>
  </n-popover>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NButton, NPopover, NScrollbar, NSwitch } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { isArraysEqualWithOrder } from '@lib/shared/method/equal';

  import useLocalForage from '@/hooks/useLocalForage';

  import type { ContentTabsMap, TabContentItem } from './type';

  const { t } = useI18n();

  const props = defineProps<{
    tabList: TabContentItem[];
    settingKey: string;
  }>();

  const cachedData = defineModel<TabContentItem[]>('cachedList', {
    default: [],
  });

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

  async function initTab() {
    const res = await getTabsFromLocal();
    cachedData.value = res.length ? res : props.tabList.map((tab) => ({ ...tab, enable: true }));
    await nextTick();
    saveTabsToLocal(cachedData.value);
  }

  const loadTab = async () => {
    const res = await getTabsFromLocal();
    const nonCloseTab = res.filter((item) => !item.allowClose);
    const couldCloseTab = res.filter((item) => item.allowClose);
    cachedData.value = [...nonCloseTab, ...couldCloseTab];
  };

  const hasChange = ref<boolean>(false);
  function handleUpdateShow(show: boolean) {
    if (!show) {
      if (hasChange.value) {
        saveTabsToLocal(cachedData.value);
        hasChange.value = false;
      }
    }
  }

  function handleChange() {
    hasChange.value = true;
  }

  function handleReset() {
    hasChange.value = false;
    loadTab();
  }

  onBeforeMount(() => {
    console.log(111);
    loadTab();
    initTab();
  });
</script>

<style lang="less">
  .crm-tab-setting-popover {
    padding: 0 !important;
  }
</style>
