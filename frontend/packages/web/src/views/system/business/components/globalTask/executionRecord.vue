<template>
  <CrmTable
    ref="crmTableRef"
    v-bind="propsRes"
    class="global-task-execution-record-table"
    @page-change="propsEvent.pageChange"
    @page-size-change="propsEvent.pageSizeChange"
    @sorter-change="propsEvent.sorterChange"
    @filter-change="propsEvent.filterChange"
    @refresh="searchData"
  >
    <template #tableTop>
      <div class="font-[600]">{{ t('system.business.globalTask.executionRecordList') }}</div>
    </template>
    <template #actionRight>
      <CrmSearchInput v-model:value="keyword" :placeholder="t('common.searchByName')" @search="searchData" />
    </template>
  </CrmTable>
</template>

<script setup lang="ts">
  import { h, onMounted, ref, watch } from 'vue';
  import dayjs from 'dayjs';

  import { TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import {
    type AgentTaskExecutionRecordItem,
    AgentTaskExecutionRecordStatusEnum,
  } from '@lib/shared/models/system/agentTask';

  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import type { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';

  import {
    deleteAgentTaskExecutionRecord,
    getAgentTaskExecutionRecordList,
    stopAgentTaskExecutionRecord,
  } from '@/api/modules';
  import useModal from '@/hooks/useModal';

  const { t } = useI18n();
  const { openModal } = useModal();
  const keyword = ref('');
  const crmTableRef = ref<InstanceType<typeof CrmTable>>();
  const refreshId = ref(0);

  const statusLabelMap: Record<AgentTaskExecutionRecordStatusEnum, string> = {
    [AgentTaskExecutionRecordStatusEnum.RUNNING]: t('common.inProgress'),
    [AgentTaskExecutionRecordStatusEnum.COMPLETED]: t('common.completed'),
    [AgentTaskExecutionRecordStatusEnum.STOPPED]: t('common.stopped'),
  };

  function getActionList(row: AgentTaskExecutionRecordItem) {
    switch (row.status) {
      case AgentTaskExecutionRecordStatusEnum.RUNNING:
        return [{ label: t('common.stop'), key: 'stop', permission: ['SYSTEM_SETTING:UPDATE'] }];
      case AgentTaskExecutionRecordStatusEnum.COMPLETED:
      case AgentTaskExecutionRecordStatusEnum.STOPPED:
        return [{ label: t('common.delete'), key: 'delete', permission: ['SYSTEM_SETTING:DELETE'] }];
      default:
        return [];
    }
  }

  function handleStop(row: AgentTaskExecutionRecordItem) {
    openModal({
      type: 'warning',
      title: t('system.business.globalTask.stopRecordConfirmTitle', { name: row.taskName }),
      positiveText: t('common.stop'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await stopAgentTaskExecutionRecord(row.id);
          refreshId.value += 1;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
    });
  }

  function handleDelete(row: AgentTaskExecutionRecordItem) {
    openModal({
      type: 'error',
      title: t('system.business.globalTask.deleteRecordConfirmTitle', { name: row.taskName }),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteAgentTaskExecutionRecord(row.id);
          refreshId.value += 1;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
    });
  }

  function handleActionSelect(row: AgentTaskExecutionRecordItem, actionKey: string) {
    if (actionKey === 'stop') {
      handleStop(row);
    }
    if (actionKey === 'delete') {
      handleDelete(row);
    }
  }

  const columns: CrmDataTableColumn<AgentTaskExecutionRecordItem>[] = [
    {
      title: t('system.business.globalTask.executionTime'),
      key: 'executionTime',
      width: 190,
      sortOrder: false,
      sorter: true,
      render: (row) => (row.executionTime ? dayjs(row.executionTime).format('YYYY-MM-DD HH:mm:ss') : '-'),
    },
    {
      title: t('system.business.globalTask.taskName'),
      key: 'taskName',
      minWidth: 180,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('system.business.globalTask.triggerReason'),
      key: 'triggerReason',
      minWidth: 220,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('common.status'),
      key: 'status',
      width: 100,
      render: (row) => statusLabelMap[row.status] || '-',
    },
    {
      title: t('system.business.globalTask.result'),
      key: 'result',
      width: 140,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('system.business.globalTask.confirmer'),
      key: 'confirmUserName',
      width: 120,
      ellipsis: {
        tooltip: true,
      },
      render: (row) => row.confirmUserName || t('system.business.globalTask.autoExecute'),
    },
    {
      title: t('common.operation'),
      key: 'operation',
      width: 100,
      fixed: 'right',
      render: (row) =>
        h(CrmOperationButton, {
          groupList: getActionList(row),
          onSelect: (key: string) => handleActionSelect(row, key),
        }),
    },
  ];

  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable<AgentTaskExecutionRecordItem>(
    getAgentTaskExecutionRecordList,
    {
      columns,
      tableKey: TableKeyEnum.SYSTEM_GLOBAL_TASK_EXECUTION_RECORD,
      showSetting: true,
      containerClass: '.global-task-execution-record-table',
    }
  );

  function searchData(val?: string) {
    setLoadListParams({ keyword: val ?? keyword.value });
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  onMounted(() => {
    searchData();
  });

  watch(
    () => refreshId.value,
    () => {
      searchData(keyword.value);
    }
  );
</script>
