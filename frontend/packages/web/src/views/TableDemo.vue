<template>
  <crm-card>
    <crm-table
      v-bind="propsRes"
      @page-change="propsEvent.pageChange"
      @page-size-change="propsEvent.pageSizeChange"
      @sorter-change="propsEvent.sorterChange"
      @filter-change="propsEvent.filterChange"
    />
  </crm-card>
</template>

<script setup lang="ts">
  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { CrmDataTableColumn, CrmTableDataItem } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';

  import { useI18n } from '@/hooks/useI18n';

  import { TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import type { CommonList } from '@lib/shared/models/common';

  interface RoleItem {
    id: string;
    num: string;
    status: string;
    title: string;
  }

  const { t } = useI18n();

  const columns: CrmDataTableColumn[] = [
    {
      type: 'selection',
      // multiple: false, // 设置单选
    },
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
          label: '222',
          value: '222',
        },
        {
          label: 'string',
          value: 'string',
        },
      ],
      filter: true,
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
      }, 200);
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
</script>
