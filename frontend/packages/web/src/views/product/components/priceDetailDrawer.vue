<template>
  <CrmDrawer v-model:show="show" :title="sourceName" :width="800" :footer="false" :view-size="formViewSize">
    <template #titleRight>
      <n-button
        v-permission="['PRICE:UPDATE']"
        type="primary"
        ghost
        class="n-btn-outline-primary ml-[12px]"
        @click="handleEdit"
      >
        {{ t('common.edit') }}
      </n-button>
    </template>
    <div class="h-full bg-[var(--text-n9)] p-[16px]">
      <CrmCard v-if="showDetailTabs" no-content-padding hide-footer auto-height class="mb-[16px]">
        <CrmTab v-model:active-tab="activeTab" no-content :tab-list="tabList" type="line">
          <template #suffix>
            <CrmTabSetting
              v-if="showDetailTabs"
              :tab-list="enabledDetailTabList"
              :setting-key="`${FormDesignKeyEnum.PRICE}-settingKey`"
              @init="initTabList"
            />
          </template>
        </CrmTab>
      </CrmCard>
      <CrmCard contentHeight="100%" hide-footer :special-height="showDetailTabs ? 80 : 0" no-content-padding>
        <CrmFormDescription
          v-show="activeTab === 'price'"
          ref="descriptionRef"
          :form-key="FormDesignKeyEnum.PRICE"
          :source-id="props.id"
          :column="3"
          label-width="auto"
          value-align="start"
          tooltip-position="top-start"
          :readonly="!hasAnyPermission(['PRICE:UPDATE'])"
          class="p-[24px]"
          @init="handleDescriptionInit"
        />
        <template v-for="item in customDetailTabTableList" :key="String(item.tab.name)">
          <div v-if="activeTab === item.tab.name" class="h-full px-[24px] pt-[24px]">
            <component :is="item.table.component" v-bind="item.table.props" hideBoard />
          </div>
        </template>
      </CrmCard>
    </div>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { NButton } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { CollaborationType } from '@lib/shared/models/customer';
  import type { FormConfig, FormViewSize } from '@lib/shared/models/system/module';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import CrmFormDescription from '@/components/business/crm-form-description/index.vue';
  import CrmTabSetting from '@/components/business/crm-tab-setting/index.vue';
  import type { TabContentItem } from '@/components/business/crm-tab-setting/type';

  import useFormDetailTabAvailability from '@/hooks/useFormDetailTabAvailability';
  import useFormDetailTabs from '@/hooks/useFormDetailTabs';
  import useFormDetailTabTable from '@/hooks/useFormDetailTabTable';
  import { hasAnyPermission } from '@/utils/permission';

  const props = defineProps<{
    id: string;
  }>();
  const emit = defineEmits<{
    (e: 'edit', id: string): void;
  }>();

  const { t } = useI18n();

  const show = defineModel<boolean>('show', {
    default: false,
  });

  const sourceName = ref<string>('');
  const formConfig = ref<FormConfig>();
  const formViewSize = ref<FormViewSize>('medium');
  function handleDescriptionInit(
    _collaborationType?: CollaborationType,
    _sourceName?: string,
    detail?: Record<string, any>,
    config?: FormConfig
  ) {
    sourceName.value = _sourceName || '';
    formConfig.value = config;
    formViewSize.value = config?.viewSize || 'medium';
  }

  const activeTab = ref('price');
  const { availableDetailTabIds } = useFormDetailTabAvailability(formConfig, FormDesignKeyEnum.PRICE);
  const { customDetailTabList, enabledDetailTabList } = useFormDetailTabs(formConfig, [], availableDetailTabIds);
  const showDetailTabs = computed(() => enabledDetailTabList.value.length > 0);
  const { getDetailTabTable } = useFormDetailTabTable();
  const customDetailTabTableList = computed(() =>
    customDetailTabList.value.flatMap((tab) => {
      const table = getDetailTabTable(tab.detailTab, props.id, FormDesignKeyEnum.PRICE);
      return table ? [{ tab, table }] : [];
    })
  );
  const settingTabList = ref<TabContentItem[]>([]);
  const tabList = computed<TabContentItem[]>(() => [
    {
      name: 'price',
      tab: t('crmFormCreate.drawer.price'),
      enable: true,
      permission: ['PRICE:READ'],
    },
    ...settingTabList.value,
  ]);

  function initTabList(list: TabContentItem[]) {
    settingTabList.value = list;
  }

  watch(
    () => tabList.value,
    (list) => {
      if (!list.some((item) => item.name === activeTab.value)) {
        activeTab.value = list[0]?.name as string;
      }
    }
  );

  function handleEdit() {
    emit('edit', props.id);
    show.value = false;
  }
</script>

<style lang="less" scoped></style>
