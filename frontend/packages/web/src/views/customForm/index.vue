<template>
  <CrmCard no-content-padding hide-footer>
    <CrmSplitPanel :default-size="0.2" :min="0.2" :max="0.5">
      <template #1>
        <div class="h-full p-[24px]">
          <div class="mb-[8px] flex w-full items-center gap-[8px]">
            <CrmSearchInput v-model:value="keyword" class="flex-1" />
            <n-button type="primary" class="p-[8px]" ghost @click="addForm">
              <CrmIcon type="iconicon_add" />
            </n-button>
          </div>
          <n-empty
            v-if="finished && formList.length === 0"
            :description="t('common.noData')"
            class="flex h-[400px] flex-col items-center justify-center"
          />
          <CrmList
            v-show="finished && formList.length > 0"
            v-model:data="formList"
            v-model:active-item-key="activeForm"
            virtual-scroll-height="calc(100% - 40px)"
            key-field="id"
            :item-height="100"
            :item-more-actions="formAction"
            mode="static"
            @item-click="handleFormClick"
            @more-action-select="handleMoreActionSelect"
          >
            <template #titleLeft="{ item }">
              <n-switch v-model:value="item.open" />
            </template>
          </CrmList>
        </div>
      </template>
      <template #2>
        <div class="h-full p-[24px]">
          <formTable v-if="activeForm" :form-key="activeForm" />
        </div>
      </template>
    </CrmSplitPanel>
  </CrmCard>
  <CustomFormConfigDrawer v-model:visible="configDrawerVisible" :source-id="currentSourceId" />
</template>

<script setup lang="ts">
  import { NButton, NEmpty, NSwitch } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n.js';
  import type { CustomFormItem } from '@lib/shared/models/customForm.js';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmList from '@/components/pure/crm-list/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type.js';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmSplitPanel from '@/components/pure/crm-split-panel/index.vue';
  import CustomFormConfigDrawer from './components/customFormConfigDrawer/index.vue';
  import formTable from './components/formTable.vue';

  import { getCustomFormList } from '@/api/modules/index.js';
  import type { ActionItem } from '@/store/modules/app/types';

  const { t } = useI18n();

  const formList = ref<CustomFormItem[]>([]);
  const loading = ref(false);
  const pageNation = ref({
    total: 0,
    pageSize: 10,
    current: 1,
  });
  const finished = ref(false);
  const keyword = ref('');
  const activeForm = ref('');

  async function loadFormList(_keyword?: string) {
    try {
      loading.value = true;
      formList.value = await getCustomFormList();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
      finished.value = true;
    }
  }

  const formAction: ActionItem[] = [
    {
      label: t('common.rename'),
      key: 'rename',
    },
    {
      label: t('common.edit'),
      key: 'edit',
    },
    {
      label: t('org.addMember'),
      key: 'addMember',
    },
    {
      label: '',
      key: '',
      type: 'divider',
    },
    {
      label: t('common.delete'),
      key: 'delete',
    },
  ];

  const configDrawerVisible = ref(false);
  const currentSourceId = ref();

  function handleEdit() {
    configDrawerVisible.value = true;
  }

  function handleMoreActionSelect(event: ActionsItem, item: Record<string, any>) {
    switch (event.key) {
      case 'edit':
        currentSourceId.value = item.id;
        handleEdit();
        break;
      default:
        break;
    }
  }

  function addForm() {
    configDrawerVisible.value = true;
  }

  function handleFormClick(form: any) {
    activeForm.value = form.id;
  }

  onBeforeMount(() => {
    loadFormList();
  });
</script>

<style scoped></style>
