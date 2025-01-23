<template>
  <n-scrollbar x-scrollable :content-style="{ 'min-width': '600px', 'width': '100%' }">
    <div class="mb-[16px] mt-[4px] flex items-center justify-between">
      <n-button type="primary" @click="handleCreate">
        {{ t('role.addMember') }}
      </n-button>
      <n-input v-model:value="keyword" :placeholder="t('common.searchByName')" clearable class="!w-[240px]">
        <template #suffix>
          <n-icon>
            <Search />
          </n-icon>
        </template>
      </n-input>
    </div>
    <crm-table
      v-bind="propsRes"
      @page-change="propsEvent.pageChange"
      @page-size-change="propsEvent.pageSizeChange"
      @sorter-change="propsEvent.sorterChange"
      @filter-change="propsEvent.filterChange"
    />
  </n-scrollbar>
</template>

<script setup lang="ts">
  import { NButton, NIcon, NInput, NScrollbar } from 'naive-ui';
  import { Search } from '@vicons/ionicons5';

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
      fixed: 'left',
    },
    {
      title: t('role.memberName'),
      key: 'name',
      width: 100,
      fixed: 'left',
    },
    {
      title: t('common.status'),
      key: 'status',
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
      title: t('role.department'),
      key: 'department',
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
    {
      title: t('role.job'),
      key: 'job',
      width: 100,
    },
    {
      title: t('role.role'),
      key: 'role',
      width: 100,
    },
    {
      title: t('common.addTime'),
      key: 'createTime',
      width: 100,
    },
    {
      title: t('common.updateTime'),
      key: 'updateTime',
      width: 100,
    },
    { key: 'operation', width: 80, fixed: 'right' },
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
    scrollX: 780,
  });

  const keyword = ref('');
  function searchData() {
    setLoadListParams({ keyword: keyword.value });
    loadList();
  }

  function handleCreate() {}

  onMounted(() => {
    searchData();
  });
</script>
