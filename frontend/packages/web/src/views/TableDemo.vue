<template>
  <CrmTable
    v-bind="propsRes"
    @page-change="propsEvent.pageChange"
    @page-size-change="propsEvent.pageSizeChange"
    @sorter-change="propsEvent.sorterChange"
    @filter-change="propsEvent.filterChange"
  />
</template>

<script setup lang="ts">
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { CrmDataTableColumn, CrmTableDataItem } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';

  import { useTableStore } from '@/store';

  import { TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import type { CommonList } from '@lib/shared/models/common';

  const tableStore = useTableStore();

  interface RoleItem {
    id: string;
    num: string;
    status: string;
    title: string;
  }

  const columns: CrmDataTableColumn[] = [
    {
      type: 'selection',
      // multiple: false, // 设置单选
    },
    {
      title: 'common.creator',
      key: 'num',
      width: 60,
      sortOrder: false,
      sorter: 'default',
    },
    {
      title: 'common.execute',
      key: 'title',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: 'default',
      filterOptions: [
        {
          label: '222',
          value: '222',
        },
        {
          label: 'string',
          value: 'string',
        },
      ],
      filter(value, row) {
        return row.title === value;
      },
    },
    {
      title: 'common.text',
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
      filter(value, row) {
        return row.status === value;
      },
    },
    { key: 'operation', width: 80 },
  ];

  function getRoleList() {
    const data: CommonList<CrmTableDataItem<RoleItem>> = {
      list: [
        {
          id: '11',
          num: 'string',
          title: 'string',
          status: 'string',
          updateTime: null,
          createTime: null,
        },
        {
          id: '22',
          num: '232324323',
          title: '222',
          status: 'aaaa',
          updateTime: null,
          createTime: null,
        },
        {
          id: '33',
          num: 'string',
          title: 'string',
          status: 'string',
          updateTime: null,
          createTime: null,
        },
        {
          id: '44',
          num: '232324323',
          title: '222',
          status: 'aaaa',
          updateTime: null,
          createTime: null,
        },
        {
          id: '55',
          num: 'string',
          title: 'string',
          status: 'string',
          updateTime: null,
          createTime: null,
        },
        {
          id: '66',
          num: '232324323',
          title: '222',
          status: 'aaaa',
          updateTime: null,
          createTime: null,
        },
        {
          id: '77',
          num: 'string',
          title: 'string',
          status: 'string',
          updateTime: null,
          createTime: null,
        },
        {
          id: '88',
          num: '232324323',
          title: '222',
          status: 'aaaa',
          updateTime: null,
          createTime: null,
        },
        {
          id: '99',
          num: 'string',
          title: 'string',
          status: 'string',
          updateTime: null,
          createTime: null,
        },
        {
          id: '1',
          num: '232324323',
          title: '222',
          status: 'aaaa',
          updateTime: null,
          createTime: null,
        },
        {
          id: '2',
          num: '232324323',
          title: '222',
          status: 'aaaa',
          updateTime: null,
          createTime: null,
        },
      ],
      total: 11,
      pageSize: 10,
      current: 1,
    };
    return new Promise<CommonList<CrmTableDataItem<RoleItem>>>((resolve) => {
      setTimeout(() => {
        resolve(data);
      }, 1000);
    });
  }

  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable(getRoleList, {
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

  await tableStore.initColumn(TableKeyEnum.SYSTEM_USER, columns);
</script>
