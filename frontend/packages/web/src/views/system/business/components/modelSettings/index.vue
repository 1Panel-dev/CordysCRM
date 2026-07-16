<template>
  <CrmCard hide-footer no-content-padding class="flex-1">
    <div class="h-full px-[24px] pt-[24px]">
      <CrmTable
        ref="crmTableRef"
        v-bind="propsRes"
        class="model-settings-table"
        @page-change="propsEvent.pageChange"
        @page-size-change="propsEvent.pageSizeChange"
        @sorter-change="propsEvent.sorterChange"
        @filter-change="propsEvent.filterChange"
        @refresh="searchData"
      >
        <template #tableTop>
          <div class="flex gap-[12px]">
            <n-button type="primary" @click="openAddDrawer">
              {{ t('system.business.modelSettings.addModel') }}
            </n-button>
            <n-tooltip trigger="hover" :disabled="!isRouteStrategyDisabled">
              <template #trigger>
                <span>
                  <n-button
                    class="n-btn-outline-primary"
                    type="primary"
                    ghost
                    :disabled="isRouteStrategyDisabled"
                    @click="openRouteModal"
                  >
                    <template #icon>
                      <CrmIcon type="iconicon_set_up" :size="16" />
                    </template>
                    {{ t('system.business.modelSettings.routeStrategy') }}
                  </n-button>
                </span>
              </template>
              {{ t('system.business.modelSettings.addModelBeforeRoute') }}
            </n-tooltip>
          </div>
        </template>
        <template #actionRight>
          <CrmSearchInput v-model:value="keyword" :placeholder="t('common.searchByName')" @search="searchData" />
        </template>
      </CrmTable>
    </div>
  </CrmCard>

  <ModelSettingsDrawer v-model:show="drawerVisible" :model="editingModel" @saved="handleModelSaved(editingModel?.id)" />

  <RouteStrategyModal v-model:show="routeModalVisible" />
</template>

<script setup lang="ts">
  import { computed, h, onMounted, ref, watch } from 'vue';
  import { NButton, NSwitch, NTooltip, useMessage } from 'naive-ui';

  import { SpecialColumnEnum, TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit, formatThousands } from '@lib/shared/method';
  import type { AiModelItem } from '@lib/shared/models/system/aiModel';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmNameTooltip from '@/components/pure/crm-name-tooltip/index.vue';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import type { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import ModelSettingsDrawer from './modelSettingsDrawer.vue';
  import RouteStrategyModal from './routeStrategyModal.vue';

  import { deleteAiModel, getAiModelList, updateAiModelStatus } from '@/api/modules';
  import useModal from '@/hooks/useModal';
  import { hasAnyPermission } from '@/utils/permission';

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();

  const keyword = ref('');
  const crmTableRef = ref<InstanceType<typeof CrmTable>>();
  const tableRefreshId = ref(0);

  async function toggleModelStatus(row: AiModelItem, enable: boolean) {
    try {
      await updateAiModelStatus({ id: row.id });
      Message.success(enable ? t('common.enableSuccess') : t('common.disableSuccess'));
      tableRefreshId.value += 1;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  const routeModalVisible = ref(false);
  function openRouteModal() {
    routeModalVisible.value = true;
  }

  function handleDelete(row: AiModelItem) {
    openModal({
      type: 'error',
      title: t('system.business.modelSettings.deleteConfirmTitle', { name: characterLimit(row.displayName) }),
      content: t('system.business.modelSettings.deleteConfirmContent'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteAiModel(row.id);
          tableRefreshId.value += 1;
          Message.success(t('common.deleteSuccess'));
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
    });
  }

  const drawerVisible = ref(false);
  const editingModel = ref<AiModelItem | null>(null);

  function openAddDrawer() {
    editingModel.value = null;
    drawerVisible.value = true;
  }

  function handleEdit(row: AiModelItem) {
    editingModel.value = row;
    drawerVisible.value = true;
  }

  function handleActionSelect(row: AiModelItem, actionKey: string) {
    switch (actionKey) {
      case 'edit':
        handleEdit(row);
        break;
      case 'delete':
        handleDelete(row);
        break;
      default:
        break;
    }
  }

  const columns: CrmDataTableColumn<AiModelItem>[] = [
    {
      fixed: 'left',
      title: t('crmTable.order'),
      width: 50,
      key: SpecialColumnEnum.ORDER,
      resizable: false,
      columnSelectorDisabled: true,
      render: (_row, index) => index + 1,
    },
    {
      fixed: 'left',
      columnSelectorDisabled: true,
      title: t('system.business.modelSettings.modelName'),
      ellipsis: {
        tooltip: true,
      },
      key: 'displayName',
      width: 120,
    },
    {
      title: t('system.business.modelSettings.modelIdColumn'),
      key: 'modelName',
      ellipsis: {
        tooltip: true,
      },
      width: 120,
      render: (row) => row.modelName || '-',
    },
    {
      title: t('system.business.modelSettings.provider'),
      ellipsis: {
        tooltip: true,
      },
      key: 'provider',
      width: 120,
    },

    {
      title: t('common.status'),
      key: 'enable',
      filter: true,
      filterOptions: [
        {
          value: true,
          label: t('common.enable'),
        },
        {
          value: false,
          label: t('common.disable'),
        },
      ],
      width: 100,
      render: (row) =>
        h(NSwitch, {
          'value': row.enable,
          'rubberBand': false,
          'onUpdate:value': (enable: boolean) => {
            // TODO lmy Permission
            if (!hasAnyPermission(['MODULE_SETTING:UPDATE'])) return;
            toggleModelStatus(row, enable);
          },
        }),
    },
    {
      title: t('system.business.modelSettings.globalDailyLimitColumn'),
      key: 'globalDailyLimit',
      width: 100,
      render: (row) => formatThousands(row.globalDailyLimit),
    },
    {
      title: t('system.business.modelSettings.todayUsage'),
      key: 'userDailyLimit',
      width: 100,
      render: (row) => formatThousands(row.userDailyLimit),
    },
    {
      title: t('common.createTime'),
      key: 'createTime',
      width: 150,
      sortOrder: false,
      sorter: true,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('common.creator'),
      key: 'createUser',
      sortOrder: false,
      sorter: true,
      width: 120,
      render: (row: AiModelItem) => {
        return h(CrmNameTooltip, { text: row.createUserName });
      },
    },
    {
      title: t('common.updateTime'),
      key: 'updateTime',
      width: 150,
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: true,
    },
    {
      title: t('common.updateUserName'),
      key: 'updateUser',
      width: 120,
      sortOrder: false,
      sorter: true,
      render: (row: AiModelItem) => {
        return h(CrmNameTooltip, { text: row.updateUserName });
      },
    },
    {
      key: 'operation',
      width: 100,
      fixed: 'right',
      render: (row) =>
        h(CrmOperationButton, {
          groupList: [
            // TODO lmy permission
            { label: t('common.edit'), key: 'edit', permission: [''] },
            { label: t('common.delete'), key: 'delete', permission: [''] },
          ],
          onSelect: (key: string) => handleActionSelect(row, key),
        }),
    },
  ];

  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable<AiModelItem>(getAiModelList, {
    columns,
    tableKey: TableKeyEnum.SYSTEM_MODEL_SETTINGS,
    permission: ['SYSTEM_ROLE:REMOVE_USER'], // TODO lmy
    showSetting: true,
    containerClass: '.model-settings-table',
  });
  const isRouteStrategyDisabled = computed(() => (propsRes.value.crmPagination?.itemCount ?? 0) === 0);

  function searchData(val?: string, refreshId?: string) {
    setLoadListParams({ keyword: val ?? keyword.value });
    loadList(false, refreshId);
    if (!refreshId) {
      crmTableRef.value?.scrollTo({ top: 0 });
    }
  }

  function handleModelSaved(refreshId?: string) {
    searchData(keyword.value, refreshId);
  }

  watch(
    () => tableRefreshId.value,
    () => {
      crmTableRef.value?.clearCheckedRowKeys();
      searchData(keyword.value);
    }
  );

  onMounted(() => {
    loadList();
  });
</script>
