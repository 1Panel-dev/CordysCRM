<template>
  <CrmCard hide-footer>
    <div class="mb-[16px] flex items-center justify-between">
      <n-button type="primary">{{ t('system.business.authenticationSettings.add') }}</n-button>
      <n-input v-model:value="keyword" :placeholder="t('common.searchByName')" clearable class="!w-[240px]" />
    </div>
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
  import { NButton, NInput } from 'naive-ui';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';

  import { useI18n } from '@/hooks/useI18n';

  import { TableKeyEnum } from '@lib/shared/enums/tableEnum';

  const { t } = useI18n();

  const keyword = ref('');
  const columns: CrmDataTableColumn[] = [
    {
      title: t('common.creator'),
      key: 'num',
      width: 60,
      sortOrder: false,
      sorter: true,
    },
    {
      title: t('common.execute'),
      key: 'title',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: true,
      filterOptions: [
        {
          label: 'London',
          value: 'London',
        },
        {
          label: 'New York',
          value: 'New York',
        },
      ],
      filter: 'default',
    },
    {
      title: t('common.text'),
      key: 'status',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      filterOptions: [
        {
          label: 'London',
          value: 'London',
        },
        {
          label: 'New York',
          value: 'New York',
        },
      ],
      filter: 'default',
    },
    { key: 'operation', width: 80 },
  ];

  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable(undefined, {
    tableKey: TableKeyEnum.SYSTEM_USER,
    showSetting: true,
    columns,
  });

  function searchData() {
    setLoadListParams({ keyword: '' });
    loadList();
  }

  onMounted(() => {
    searchData();
  });
</script>
