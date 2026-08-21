<template>
  <CrmTable
    ref="crmTableRef"
    v-bind="propsRes"
    class="global-task-table"
    @page-change="propsEvent.pageChange"
    @page-size-change="propsEvent.pageSizeChange"
    @sorter-change="propsEvent.sorterChange"
    @filter-change="propsEvent.filterChange"
    @refresh="searchData"
  >
    <template #tableTop>
      <n-button v-permission="['SYSTEM_SETTING:ADD']" type="primary" @click="openAddDrawer">
        {{ t('system.business.globalTask.addTask') }}
      </n-button>
    </template>
    <template #actionRight>
      <CrmSearchInput v-model:value="keyword" :placeholder="t('common.searchByName')" @search="searchData" />
    </template>
  </CrmTable>

  <GlobalTaskDrawer v-model:show="drawerVisible" :task="editingTask" @saved="handleTaskSaved" />
</template>

<script setup lang="ts">
  import { computed, h, onMounted, ref, watch } from 'vue';
  import { NButton, NSwitch, useMessage } from 'naive-ui';

  import { SpecialColumnEnum, TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import { type AgentTaskItem } from '@lib/shared/models/system/agentTask';

  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import type { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import GlobalTaskDrawer from './globalTaskDrawer.vue';

  import { deleteAgentTask, getAgentTaskDetail, getAgentTaskList, switchAgentTask } from '@/api/modules';
  import { confirmationLevelOptions } from '@/config/globalTask';
  import useModal from '@/hooks/useModal';
  import { hasAnyPermission } from '@/utils/permission';

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();

  const keyword = ref('');
  const crmTableRef = ref<InstanceType<typeof CrmTable>>();
  const tableRefreshId = ref(0);
  const drawerVisible = ref(false);
  const editingTask = ref<AgentTaskItem | null>(null);

  const confirmationLevelLabelMap = computed(() =>
    Object.fromEntries(confirmationLevelOptions.map((item) => [item.value, item.tableLabel]))
  );

  async function toggleTaskStatus(row: AgentTaskItem, enable: boolean) {
    try {
      await switchAgentTask(row.id);
      Message.success(enable ? t('common.enableSuccess') : t('common.disableSuccess'));
      tableRefreshId.value += 1;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  function openAddDrawer() {
    editingTask.value = null;
    drawerVisible.value = true;
  }

  async function handleEdit(row: AgentTaskItem) {
    try {
      editingTask.value = await getAgentTaskDetail(row.id);
      drawerVisible.value = true;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  const tableRemoveRefreshId = ref('');
  function handleDelete(row: AgentTaskItem) {
    openModal({
      type: 'error',
      title: t('system.business.globalTask.deleteConfirmTitle', { name: characterLimit(row.name) }),
      content: t('system.business.globalTask.deleteConfirmContent'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteAgentTask(row.id);
          Message.success(t('common.deleteSuccess'));
          tableRemoveRefreshId.value = row.id;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
    });
  }

  function handleActionSelect(row: AgentTaskItem, actionKey: string) {
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

  const columns: CrmDataTableColumn<AgentTaskItem>[] = [
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
      title: t('system.business.globalTask.taskName'),
      key: 'name',
      width: 200,
      ellipsis: {
        tooltip: true,
      },
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
          'disabled': !hasAnyPermission(['SYSTEM_SETTING:UPDATE']),
          'rubberBand': false,
          'onUpdate:value': (enable: boolean) => {
            if (!hasAnyPermission(['SYSTEM_SETTING:UPDATE'])) return;
            toggleTaskStatus(row, enable);
          },
        }),
    },
    {
      title: t('system.business.globalTask.confirmationLevel'),
      key: 'confirmationLevel',
      width: 150,
      ellipsis: {
        tooltip: true,
      },
      render: (row) => confirmationLevelLabelMap.value[row.confirmationLevel] || '-',
    },
    {
      title: t('system.business.globalTask.triggerCondition'),
      key: 'executionCondition',
      width: 180,
      ellipsis: {
        tooltip: true,
      },
      render: (row) => row.executionCondition || '-',
    },
    {
      title: t('system.business.globalTask.executionAction'),
      key: 'executionAction',
      width: 220,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      key: 'operation',
      width: 100,
      fixed: 'right',
      render: (row) =>
        h(CrmOperationButton, {
          groupList: [
            { label: t('common.edit'), key: 'edit', permission: ['SYSTEM_SETTING:UPDATE'] },
            { label: t('common.delete'), key: 'delete', permission: ['SYSTEM_SETTING:DELETE'] },
          ],
          onSelect: (key: string) => handleActionSelect(row, key),
        }),
    },
  ];

  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable<AgentTaskItem>(getAgentTaskList, {
    columns,
    tableKey: TableKeyEnum.SYSTEM_GLOBAL_TASK,
    permission: ['SYSTEM_SETTING:UPDATE', 'SYSTEM_SETTING:DELETE'],
    showSetting: true,
    containerClass: '.global-task-table',
  });

  function searchData(val?: string, refreshId?: string) {
    setLoadListParams({ keyword: val ?? keyword.value });
    loadList(false, refreshId);
    if (!refreshId) {
      crmTableRef.value?.scrollTo({ top: 0 });
    }
  }

  function handleTaskSaved(refreshId?: string) {
    searchData(keyword.value, refreshId);
  }

  function removeItemFromList(id: string) {
    propsRes.value.data = propsRes.value.data.filter((item) => item.id !== id) as typeof propsRes.value.data;
    propsRes.value.crmPagination = {
      ...propsRes.value.crmPagination,
      itemCount: (propsRes.value.crmPagination?.itemCount ?? 1) - 1,
    };
  }

  watch(
    () => tableRefreshId.value,
    () => {
      searchData(keyword.value);
    }
  );

  watch(
    () => tableRemoveRefreshId.value,
    (val) => {
      if (val) {
        removeItemFromList(val);
      }
    }
  );

  onMounted(() => {
    loadList();
  });
</script>
