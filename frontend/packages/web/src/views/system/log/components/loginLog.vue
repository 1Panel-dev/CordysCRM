<template>
  <CrmCard hide-footer :special-height="160">
    <CrmTable
      v-bind="propsRes"
      @page-change="propsEvent.pageChange"
      @page-size-change="propsEvent.pageSizeChange"
      @sorter-change="propsEvent.sorterChange"
      @filter-change="propsEvent.filterChange"
    />
  </CrmCard>
</template>

<script setup lang="ts">
  import { TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { OperationLogParams } from '@lib/shared/models/system/log';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';

  import { loginLogList } from '@/api/modules/system/log';

  const { t } = useI18n();

  const platformOption = [
    { label: t('log.platform.web'), value: 'WEB' },
    { label: t('log.platform.mobile'), value: 'MOBILE' },
  ];

  const columns: CrmDataTableColumn[] = [
    {
      title: t('log.operator'),
      key: 'operatorName',
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('log.loginTime'),
      key: 'createTime',
      sortOrder: false,
      sorter: true,
    },
    {
      title: t('log.platform'),
      key: 'platform',
      resizable: false,
      render: (row) => {
        const item = platformOption.find((e) => e.value === row.platform);
        return item ? item.label : '-';
      },
    },
  ];
  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable(loginLogList, {
    showSetting: false,
    columns,
    tableKey: TableKeyEnum.LOGIN_LOG,
  });

  async function searchData(params: OperationLogParams) {
    setLoadListParams({ ...params });
    await loadList();
  }

  defineExpose({
    searchData,
  });
</script>
