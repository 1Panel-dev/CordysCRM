<template>
  <CrmDrawer v-model:show="visible" resizable no-padding :width="800" :footer="false" :title="title">
    <template #titleRight>
      <n-button
        v-permission="['PRODUCT_MANAGEMENT:UPDATE']"
        type="primary"
        ghost
        class="n-btn-outline-primary"
        @click="emit('edit', props.sourceId)"
      >
        {{ t('common.edit') }}
      </n-button>
    </template>
    <div class="h-full bg-[var(--text-n9)] px-[16px] pt-[16px]">
      <CrmCard v-if="showDetailTabs" no-content-padding hide-footer auto-height class="mb-[16px]">
        <CrmTab v-model:active-tab="activeTab" no-content :tab-list="tabList" type="line">
          <template #suffix>
            <CrmTabSetting
              v-if="showDetailTabs"
              :tab-list="enabledDetailTabList"
              :setting-key="`${FormDesignKeyEnum.PRODUCT}-settingKey`"
              @init="initTabList"
            />
          </template>
        </CrmTab>
      </CrmCard>
      <CrmCard contentHeight="100%" hide-footer :special-height="showDetailTabs ? 80 : 0" no-content-padding>
        <div v-show="activeTab === 'product'" class="h-full p-[24px]">
          <CrmFormDescription
            :form-key="FormDesignKeyEnum.PRODUCT"
            :source-id="props.sourceId"
            :column="3"
            :refresh-key="props.refreshId"
            label-width="auto"
            value-align="start"
            tooltip-position="top-start"
            :readonly="!hasAnyPermission(['PRODUCT_MANAGEMENT:UPDATE'])"
            @init="handleInit"
          />
        </div>
        <template v-for="item in customDetailTabTableList" :key="String(item.tab.name)">
          <div v-if="activeTab === item.tab.name" class="h-full px-[24px] pt-[24px]">
            <component :is="item.table.component" v-bind="item.table.props" hideBoard />
          </div>
        </template>
      </CrmCard>
    </div>
  </CrmDrawer>
</template>

<script lang="ts" setup>
  import { NButton } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { CollaborationType } from '@lib/shared/models/customer';
  import type { FormConfig } from '@lib/shared/models/system/module';

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
    sourceId: string;
    refreshId?: number;
  }>();
  const emit = defineEmits<{
    (e: 'edit', sourceId: string): void;
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const { t } = useI18n();
  const title = ref('');
  const formConfig = ref<FormConfig>();

  function handleInit(type?: CollaborationType, name?: string, _detail?: Record<string, any>, config?: FormConfig) {
    title.value = name || '';
    formConfig.value = config;
  }

  const activeTab = ref('product');
  const { availableDetailTabIds } = useFormDetailTabAvailability(formConfig, FormDesignKeyEnum.PRODUCT);
  const { customDetailTabList, enabledDetailTabList } = useFormDetailTabs(formConfig, [], availableDetailTabIds);
  const showDetailTabs = computed(() => enabledDetailTabList.value.length > 0);
  const { getDetailTabTable } = useFormDetailTabTable();
  const customDetailTabTableList = computed(() =>
    customDetailTabList.value.flatMap((tab) => {
      const table = getDetailTabTable(tab.detailTab, props.sourceId, FormDesignKeyEnum.PRODUCT);
      return table ? [{ tab, table }] : [];
    })
  );
  const settingTabList = ref<TabContentItem[]>([]);
  const tabList = computed<TabContentItem[]>(() => [
    {
      name: 'product',
      tab: t('crmFormDesign.product'),
      enable: true,
      permission: ['PRODUCT_MANAGEMENT:READ'],
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
</script>
