<template>
  <CrmCard no-content-padding hide-footer>
    <CrmSplitPanel :default-size="0.2" :min="0.2" :max="0.5">
      <template #1>
        <div class="h-full p-[24px]">
          <div class="mb-[8px] flex w-full items-center gap-[8px]">
            <CrmSearchInput v-model:value="keyword" class="flex-1" @search="searchList" />
            <n-button
              v-permission="['CUSTOM_FORM:ADD']"
              type="primary"
              class="n-btn-outline-primary p-[8px]"
              ghost
              @click="addForm"
            >
              <CrmIcon type="iconicon_add" :size="16" />
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
            :item-more-actions="getFormAction"
            item-class="gap-[8px] px-[4px]"
            activeItemClass="bg-[var(--text-n9)]"
            mode="static"
            @item-click="handleFormClick"
            @more-action-select="handleMoreActionSelect"
          >
            <template #titleLeft="{ item }">
              <n-tooltip trigger="hover" :disabled="item.isAdmin">
                <template #trigger>
                  <n-switch
                    :value="item.enable"
                    :disabled="!item.isAdmin"
                    class="ml-[4px]"
                    size="small"
                    @click="handleBeforeEnableChange(item)"
                  />
                </template>
                {{ t('customForm.enableDisabledTip') }}
              </n-tooltip>
            </template>
            <template #title="{ item }">
              <n-tooltip trigger="hover">
                <template #trigger>
                  <div class="one-line-text" :class="activeForm === item.id ? 'text-[var(--primary-8)]' : ''">
                    {{ item.name }}
                  </div>
                </template>
                {{ item.name }}
              </n-tooltip>
            </template>
          </CrmList>
        </div>
      </template>
      <template #2>
        <div class="h-full p-[24px]">
          <formTable v-if="activeForm" :form-key="activeForm" :readonly="!activeFormIsAdmin" />
        </div>
      </template>
    </CrmSplitPanel>
  </CrmCard>
  <CustomFormConfigDrawer
    v-model:visible="configDrawerVisible"
    :source-id="currentSourceId"
    :defaultTab="defaultTab"
    @saved="handleFormSaved"
  />
</template>

<script setup lang="ts">
  import { NButton, NEmpty, NSwitch, NTooltip, useMessage } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import { useI18n } from '@lib/shared/hooks/useI18n.js';
  import { characterLimit } from '@lib/shared/method/index.js';
  import type { CustomFormItem } from '@lib/shared/models/customForm.js';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmList from '@/components/pure/crm-list/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type.js';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmSplitPanel from '@/components/pure/crm-split-panel/index.vue';
  import CustomFormConfigDrawer from './components/customFormConfigDrawer/index.vue';
  import formTable from './components/formTable.vue';

  import { deleteCustomForm, disableCustomForm, enableCustomForm, getCustomFormList } from '@/api/modules/index.js';
  import useModal from '@/hooks/useModal.js';

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();

  const formList = ref<CustomFormItem[]>([]);
  const formListBackup = ref<CustomFormItem[]>([]);
  const loading = ref(false);
  const finished = ref(false);
  const keyword = ref('');
  const activeForm = ref('');
  const activeFormIsAdmin = computed(() => formList.value.find((e) => e.id === activeForm.value)?.isAdmin);

  async function loadFormList() {
    try {
      loading.value = true;
      formList.value = await getCustomFormList();
      formListBackup.value = cloneDeep(formList.value);
      if (activeForm.value === '') {
        activeForm.value = formList.value[0]?.id || '';
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
      finished.value = true;
    }
  }

  function searchList(_keyword: string) {
    formList.value = formListBackup.value.filter((e) =>
      e.name.toLocaleLowerCase().includes(_keyword?.toLocaleLowerCase())
    );
  }

  const formAction: ActionsItem[] = [
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
      danger: true,
    },
  ];

  function getFormAction(item: any) {
    if (item.isAdmin) {
      return formAction;
    }
    return [];
  }

  const configDrawerVisible = ref(false);
  const currentSourceId = ref();
  const defaultTab = ref<'design' | 'memberPermission'>('design');

  // 删除
  function handleDelete(row: any) {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: characterLimit(row.name) }),
      content: t('customForm.deleteFormTip'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteCustomForm(row.id);
          Message.success(t('common.deleteSuccess'));
          loadFormList();
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  function handleMoreActionSelect(event: ActionsItem, item: Record<string, any>) {
    switch (event.key) {
      case 'edit':
        currentSourceId.value = item.id;
        configDrawerVisible.value = true;
        break;
      case 'addMember':
        currentSourceId.value = item.id;
        defaultTab.value = 'memberPermission';
        configDrawerVisible.value = true;
        break;
      case 'delete':
        handleDelete(item);
        break;
      default:
        break;
    }
  }

  async function handleBeforeEnableChange(item: CustomFormItem) {
    if (!item.isAdmin) {
      return;
    }
    if (item.enable) {
      openModal({
        type: 'error',
        title: t('common.confirmClose'),
        content: t('customForm.closeFormTip'),
        positiveText: t('common.confirmClose'),
        negativeText: t('common.cancel'),
        onPositiveClick: async () => {
          try {
            await disableCustomForm(item.id);
            Message.success(t('common.closeSuccess'));
            item.enable = false;
          } catch (error) {
            // eslint-disable-next-line no-console
            console.log(error);
          }
        },
      });
    } else {
      try {
        await enableCustomForm(item.id);
        Message.success(t('common.enableSuccess'));
        item.enable = true;
      } catch (error) {
        // eslint-disable-next-line no-console
        console.log(error);
      }
    }
  }

  function addForm() {
    currentSourceId.value = undefined;
    configDrawerVisible.value = true;
  }

  function handleFormClick(form: any) {
    activeForm.value = form.id;
  }

  async function handleFormSaved(id?: string) {
    await loadFormList();
    if (id) {
      currentSourceId.value = id;
      activeForm.value = id;
    }
  }

  onBeforeMount(() => {
    loadFormList();
  });
</script>

<style scoped></style>
