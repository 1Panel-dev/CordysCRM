<template>
  <CrmDrawer
    v-model:show="visible"
    resizable
    no-padding
    :width="800"
    :footer="false"
    :title="title"
    :view-size="formViewSize"
  >
    <template #titleRight>
      <n-button v-if="isAdmin" type="primary" ghost class="n-btn-outline-primary" @click="emit('edit', props.sourceId)">
        {{ t('common.edit') }}
      </n-button>
      <n-button v-if="isAdmin" type="error" ghost class="ml-[12px]" @click="handleDelete">
        {{ t('common.delete') }}
      </n-button>
    </template>
    <div class="h-full bg-[var(--text-n9)] px-[16px] pt-[16px]">
      <CrmCard v-if="showDetailTabs" no-content-padding hide-footer auto-height class="mb-[16px]">
        <CrmTab v-model:active-tab="activeTab" no-content :tab-list="tabList" type="line">
          <template #suffix>
            <CrmTabSetting
              v-if="showDetailTabs"
              :tab-list="enabledDetailTabList"
              :setting-key="`${props.customFormId}-settingKey`"
              @init="initTabList"
            />
          </template>
        </CrmTab>
      </CrmCard>
      <CrmCard contentHeight="100%" hide-footer :special-height="showDetailTabs ? 80 : 0" no-content-padding>
        <div v-show="activeTab === 'customForm'" class="h-full p-[24px]">
          <CrmFormDescription
            :form-key="FormDesignKeyEnum.CUSTOM_FORM"
            :source-id="props.sourceId"
            :column="3"
            :refresh-key="formDescriptionRefreshKey"
            refresh-form-config
            label-width="auto"
            value-align="start"
            tooltip-position="top-start"
            :readonly="!isAdmin"
            :customFormId="props.customFormId"
            @init="handleInit"
          />
        </div>
        <template v-for="item in customDetailTabTableList" :key="String(item.tab.name)">
          <div v-if="activeTab === item.tab.name" class="h-full px-[24px] pt-[24px]">
            <component :is="item.table.component" v-bind="item.table.props" />
          </div>
        </template>
      </CrmCard>
    </div>
  </CrmDrawer>
</template>

<script lang="ts" setup>
  import { NButton, useMessage } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import { CollaborationType } from '@lib/shared/models/customer';
  import type { FormConfig, FormViewSize } from '@lib/shared/models/system/module';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import CrmFormDescription from '@/components/business/crm-form-description/index.vue';
  import CrmTabSetting from '@/components/business/crm-tab-setting/index.vue';
  import type { TabContentItem } from '@/components/business/crm-tab-setting/type';

  import { deleteCustomFormData } from '@/api/modules';
  import useFormDetailTabAvailability from '@/hooks/useFormDetailTabAvailability';
  import useFormDetailTabs from '@/hooks/useFormDetailTabs';
  import useFormDetailTabTable from '@/hooks/useFormDetailTabTable';
  import useModal from '@/hooks/useModal';

  const props = defineProps<{
    sourceId: string;
    refreshId?: number;
    customFormId?: string;
  }>();
  const emit = defineEmits<{
    (e: 'edit', sourceId: string): void;
    (e: 'refresh'): void;
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const { t } = useI18n();
  const { openModal } = useModal();
  const Message = useMessage();
  const title = ref('');
  const isAdmin = ref(false);
  const formConfig = ref<FormConfig>();
  const formViewSize = ref<FormViewSize>('large');
  const formDescriptionRefreshKey = ref(0);

  function handleInit(type?: CollaborationType, name?: string, detail?: Record<string, any>, config?: FormConfig) {
    title.value = name || '';
    isAdmin.value = !!detail?.isAdmin;
    formConfig.value = config;
    formViewSize.value = config?.viewSize || 'large';
  }

  const activeTab = ref('customForm');
  const { availableDetailTabIds } = useFormDetailTabAvailability(
    formConfig,
    computed(() => props.customFormId)
  );
  const { customDetailTabList, enabledDetailTabList } = useFormDetailTabs(formConfig, [], availableDetailTabIds);
  const showDetailTabs = computed(() => enabledDetailTabList.value.length > 0);
  const { getDetailTabTable } = useFormDetailTabTable();
  const customDetailTabTableList = computed(() =>
    customDetailTabList.value.flatMap((tab) => {
      const table = getDetailTabTable(tab.detailTab, props.sourceId, props.customFormId);
      return table ? [{ tab, table }] : [];
    })
  );
  const settingTabList = ref<TabContentItem[]>([]);
  const tabList = computed<TabContentItem[]>(() => [
    {
      name: 'customForm',
      tab: title.value,
      enable: true,
      permission: ['CUSTOM_FORM:READ'],
    },
    ...settingTabList.value,
  ]);

  function initTabList(list: TabContentItem[]) {
    settingTabList.value = list;
  }

  watch([() => visible.value, () => props.sourceId, () => props.customFormId, () => props.refreshId], ([isVisible]) => {
    if (isVisible) {
      formDescriptionRefreshKey.value += 1;
    }
  });

  watch(
    () => tabList.value,
    (list) => {
      if (!list.some((item) => item.name === activeTab.value)) {
        activeTab.value = list[0]?.name as string;
      }
    }
  );

  // 删除
  function handleDelete() {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: characterLimit(title.value) }),
      content: t('common.deleteConfirmContent'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteCustomFormData(props.sourceId);
          Message.success(t('common.deleteSuccess'));
          emit('refresh');
          visible.value = false;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }
</script>
