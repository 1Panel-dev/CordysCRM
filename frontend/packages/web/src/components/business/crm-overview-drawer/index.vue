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
      <CrmButtonGroup :list="props.buttonList" not-show-divider @select="handleButtonClick">
        <template v-for="item in props.buttonList" #[item.popSlotContent]>
          <slot :name="item.popSlotContent"></slot>
        </template>
      </CrmButtonGroup>
      <CrmMoreAction
        v-if="props.buttonMoreList?.length"
        :options="props.buttonMoreList"
        trigger="click"
        @select="(item:ActionsItem) => emit('buttonSelect', item.key as string)"
      >
        <n-button type="primary" ghost class="n-btn-outline-primary ml-[12px]">
          {{ t('common.more') }}
          <CrmIcon class="ml-[8px]" type="iconicon_chevron_down" :size="16" />
        </n-button>
      </CrmMoreAction>
    </template>
    <div v-if="layout === 'horizontal'" class="h-full w-full overflow-hidden">
      <CrmSplitPanel :max="0.5" :min="0.2" :default-size="0.2" disabled>
        <template #1>
          <slot name="left" />
        </template>
        <template #2>
          <div class="flex h-full w-full flex-col bg-[var(--text-n9)] p-[16px]">
            <slot name="rightTop" />
            <div class="flex justify-between gap-[24px] border-b-[1px] border-[var(--text-n8)] bg-[var(--text-n10)]">
              <CrmTab
                v-if="cachedList.length"
                :key="cachedListKey"
                v-model:active-tab="activeTab"
                no-content
                :tab-list="cachedList"
                type="line"
                class="cachedListTab flex-1 overflow-hidden"
              />
              <div class="mr-[24px] flex items-center gap-[16px]">
                <CrmTab v-model:active-tab="layout" no-content :tab-list="layoutList" type="segment" />
                <CrmTabSetting
                  v-if="props.showTabSetting"
                  :tab-list="props.tabList"
                  :setting-key="`${props.formKey}-settingKey`"
                  @init="initTabList"
                />
              </div>
            </div>
            <div class="flex-1 overflow-hidden">
              <slot name="right" />
            </div>
          </div>
        </template>
      </CrmSplitPanel>
    </div>

    <n-scrollbar v-else>
      <div class="bg-[var(--text-n9)] px-[16px] pt-[16px]">
        <CrmCard no-content-padding hide-footer auto-height>
          <slot name="left" />
        </CrmCard>
      </div>

      <div class="flex h-[calc(100vh-65px)] w-full flex-col bg-[var(--text-n9)] p-[16px]">
        <slot name="rightTop" />
        <div class="flex justify-between gap-[24px] border-b-[1px] border-[var(--text-n8)] bg-[var(--text-n10)]">
          <CrmTab
            v-if="cachedList.length"
            :key="cachedListKey"
            v-model:active-tab="activeTab"
            no-content
            :tab-list="cachedList"
            type="line"
            class="cachedListTab flex-1 overflow-hidden"
          />
          <div class="mr-[24px] flex items-center gap-[16px]">
            <CrmTab v-model:active-tab="layout" no-content :tab-list="layoutList" type="segment" />
            <CrmTabSetting
              v-if="props.showTabSetting"
              :tab-list="props.tabList"
              :setting-key="`${props.formKey}-settingKey`"
              @init="initTabList"
            />
          </div>
        </div>
        <div class="flex-1 overflow-hidden">
          <slot name="right" />
        </div>
      </div>
    </n-scrollbar>
  </CrmDrawer>
  <CrmFormCreateDrawer
    v-model:visible="formDrawerVisible"
    :form-key="realFormKey"
    :source-id="sourceId"
    :need-init-detail="needInitDetail"
    @saved="emit('saved')"
  />
</template>

<script lang="ts" setup>
  import { NButton, NScrollbar, NTooltip } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmButtonGroup from '@/components/pure/crm-button-group/index.vue';
  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmMoreAction from '@/components/pure/crm-more-action/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmSplitPanel from '@/components/pure/crm-split-panel/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmTabSetting from '@/components/business/crm-tab-setting/index.vue';
  import type { TabContentItem } from '@/components/business/crm-tab-setting/type';

  import useLocalForage from '@/hooks/useLocalForage';

  const props = defineProps<{
    title?: string;
    subtitle?: string;
    tabList: TabContentItem[];
    buttonList: ActionsItem[];
    buttonMoreList?: ActionsItem[];
    formKey: FormDesignKeyEnum;
    sourceId?: string;
    showTabSetting?: boolean;
  }>();

  const emit = defineEmits<{
    (e: 'buttonSelect', key: string, done?: () => void): void;
    (e: 'saved'): void;
  }>();

  const showDrawer = defineModel<boolean>('show', {
    required: true,
  });

  const activeTab = defineModel<string>('activeTab', {
    required: true,
  });

  const cachedList = ref<TabContentItem[]>(props.tabList);

  const { setItem, getItem } = useLocalForage();
  const { t } = useI18n();

  const formDrawerVisible = ref(false);
  const realFormKey = ref<FormDesignKeyEnum>(props.formKey);
  const needInitDetail = ref(false);

  function handleButtonClick(key: string, done?: () => void) {
    switch (key) {
      case 'edit':
        realFormKey.value = props.formKey;
        needInitDetail.value = true;
        formDrawerVisible.value = true;
        return;
      default:
        break;
    }
    emit('buttonSelect', key, done);
  }
  const cachedListKey = computed<string>(() => cachedList.value.map((i) => i.name).join('|'));
  function initTabList(list: TabContentItem[]) {
    cachedList.value = list;
  }

  watch(
    () => cachedList.value,
    (val) => {
      nextTick(() => {
        activeTab.value = (val[0]?.name ?? '') as string;
      });
    }
  );

  const layout = ref<'horizontal' | 'vertical'>();
  const layoutList = [
    {
      name: 'horizontal',
      tab: t('crmFormDesign.horizontal'),
    },
    {
      name: 'vertical',
      tab: t('crmFormDesign.vertical'),
    },
  ];

  onMounted(async () => {
    layout.value =
      (await getItem<'horizontal' | 'vertical'>(`active-overview-layout-${props.formKey}`)) ?? 'horizontal';
  });

  watch(
    () => layout.value,
    async (val) => {
      if (val) {
        await setItem(`active-overview-layout-${props.formKey}`, val);
      }
    }
  );

  defineExpose({
    layout,
  });
</script>

<style lang="less" scoped>
  :deep(.n-tabs-scroll-padding) {
    width: 16px !important;
  }
  .crm-button-group {
    gap: 12px;
  }
  .cachedListTab {
    :deep(.n-tabs-nav.n-tabs-nav--line-type.n-tabs-nav--top .n-tabs-nav-scroll-content) {
      border-bottom-color: transparent;
    }
  }
</style>
