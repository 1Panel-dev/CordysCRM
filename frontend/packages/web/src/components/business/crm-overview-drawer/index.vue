<template>
  <CrmDrawer v-model:show="showDrawer" width="100%" :footer="false" no-padding :show-back="true" :closable="false">
    <template #title>
      <n-tooltip trigger="hover" :delay="300" :disabled="!props.title">
        <template #trigger>
          <div class="flex gap-[4px] overflow-hidden">
            <div class="one-line-text flex-1 text-[var(--text-n1)]">{{ props.title }}</div>
            <div v-if="props.subtitle" class="flex text-[var(--text-n4)]">
              (
              <div class="one-line-text max-w-[300px]">{{ props.subtitle }}</div>
              )
            </div>
          </div>
        </template>
        {{ `${props.title}${props.subtitle ? `(${props.subtitle})` : ''}` }}
      </n-tooltip>
    </template>
    <template #titleRight>
      <CrmButtonGroup :list="props.buttonList" not-show-divider @select="handleButtonClick" />
    </template>
    <div class="h-full w-full overflow-hidden">
      <CrmSplitPanel :max="0.5" :min="0.2" :default-size="0.2" disabled>
        <template #1>
          <slot name="left" />
        </template>
        <template #2>
          <div class="h-full w-full bg-[var(--text-n9)]">
            <n-scrollbar class="p-[16px]">
              <div class="relative bg-[var(--text-n10)]">
                <CrmTab
                  v-if="showTabList.length"
                  v-model:active-tab="activeTab"
                  no-content
                  :tab-list="showTabList"
                  type="line"
                />
                <div class="absolute right-4 top-2">
                  <CrmTabSetting
                    v-model:cached-list="cachedList"
                    :tab-list="props.tabList"
                    :setting-key="'settingKey'"
                  />
                </div>
              </div>
              <slot name="right" />
            </n-scrollbar>
          </div>
        </template>
      </CrmSplitPanel>
    </div>
  </CrmDrawer>
  <CrmFormCreateDrawer v-model:visible="formDrawerVisible" :form-key="realFormKey" :source-id="realSourceId" />
</template>

<script lang="ts" setup>
  import { NScrollbar, NTooltip, TabPaneProps } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';

  import CrmButtonGroup from '@/components/pure/crm-button-group/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmSplitPanel from '@/components/pure/crm-split-panel/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmTabSetting from '@/components/business/crm-tab-setting/index.vue';
  import type { TabContentItem } from '@/components/business/crm-tab-setting/type';

  const props = defineProps<{
    title?: string;
    subtitle?: string;
    tabList: TabContentItem[];
    buttonList: ActionsItem[];
    formKey: FormDesignKeyEnum;
    sourceId?: string;
  }>();

  const emit = defineEmits<{
    (e: 'buttonSelect', key: string): void;
  }>();

  const showDrawer = defineModel<boolean>('show', {
    required: true,
  });

  const activeTab = defineModel<string>('activeTab', {
    required: true,
  });

  const cachedList = defineModel<TabContentItem[]>('cachedList', {
    required: true,
  });

  const showTabList = computed(() => {
    return cachedList.value.reduce((acc: TabPaneProps[], e: TabContentItem) => {
      if (e.enable) {
        acc.push({ name: e.name, tab: e.tab });
      }
      return acc;
    }, []);
  });

  const formDrawerVisible = ref(false);
  const realFormKey = ref<FormDesignKeyEnum>(props.formKey);
  const realSourceId = ref<string | undefined>(''); // 编辑时传入

  function handleButtonClick(key: string) {
    switch (key) {
      case 'addContract':
        realFormKey.value = FormDesignKeyEnum.CONTACT;
        formDrawerVisible.value = true;
        return;
      case 'followRecord':
        realFormKey.value = FormDesignKeyEnum.FOLLOW_RECORD;
        formDrawerVisible.value = true;
        return;
      case 'followPlan':
        realFormKey.value = FormDesignKeyEnum.FOLLOW_PLAN;
        formDrawerVisible.value = true;
        return;
      case 'edit':
        realFormKey.value = props.formKey;
        realSourceId.value = props.sourceId;
        formDrawerVisible.value = true;
        return;
      default:
        break;
    }
    emit('buttonSelect', key);
  }

  watch(
    () => showTabList.value,
    (val: TabPaneProps[]) => {
      // 避免已选中关闭后激活展示错误
      if (!val.some((tab) => tab.name === activeTab.value)) {
        activeTab.value = (val[0]?.name ?? '') as string;
      }
    },
    {
      deep: true,
    }
  );
</script>

<style lang="less" scoped>
  :deep(.n-tabs-scroll-padding) {
    width: 16px !important;
  }
  .crm-button-group {
    gap: 12px;
  }
</style>
