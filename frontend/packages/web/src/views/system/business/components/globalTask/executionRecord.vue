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
  import { onMounted, ref } from 'vue';
  import dayjs from 'dayjs';

  import { TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { AgentTaskExecutionRecordItem } from '@lib/shared/models/system/agentTask';

  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import type { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';

  import { getAgentTaskExecutionRecordList } from '@/api/modules';

  const { t } = useI18n();
  const keyword = ref('');
  const crmTableRef = ref<InstanceType<typeof CrmTable>>();

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
    // TODO lmy 结果的tag样式？
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
    loadList();
  });
</script>
