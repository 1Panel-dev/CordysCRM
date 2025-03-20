<template>
  <CrmSearchInput v-model:value="keyword" class="mb-[16px] !w-[240px]" @search="searchData" />
  <CrmTable
    v-model:checked-row-keys="selectedKeys"
    v-bind="propsRes"
    @page-change="propsEvent.pageChange"
    @page-size-change="propsEvent.pageSizeChange"
    @sorter-change="propsEvent.sorterChange"
    @filter-change="propsEvent.filterChange"
    @row-key-change="handleRowKeyChange"
  />
</template>

<script setup lang="ts">
  import { DataTableRowKey } from 'naive-ui';

  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';

  import { useI18n } from '@/hooks/useI18n';

  import { InternalRowData } from 'naive-ui/es/data-table/src/interface';

  const props = withDefaults(
    defineProps<{
      multiple?: boolean;
    }>(),
    {
      multiple: true,
    }
  );

  const { t } = useI18n();

  const selectedKeys = defineModel<DataTableRowKey[]>('selectedKeys', {
    required: true,
  });
  const selectedRows = defineModel<InternalRowData[]>('selectedRows', {
    default: [],
  });

  const columns: CrmDataTableColumn[] = [
    {
      type: 'selection',
      multiple: props.multiple,
    },
    {
      title: t('common.name'),
      key: 'name',
      width: 200,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('crmFormDesign.phone'),
      key: 'phone',
    },
  ];

  // TODO:数据源接口
  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable(
    () =>
      Promise.resolve({
        list: [
          {
            id: '1',
            name: '数据源1',
            phone: '12345678901',
          },
          {
            id: '2',
            name: '数据源2',
            phone: '12345678902',
          },
        ],
        pageSize: 10,
        current: 1,
        total: 10,
      }),
    {
      columns,
      showSetting: false,
    }
  );

  const keyword = ref('');

  function searchData() {
    setLoadListParams({ keyword: keyword.value });
    loadList();
  }

  function handleRowKeyChange(keys: DataTableRowKey[], _rows: InternalRowData[]) {
    selectedRows.value = _rows;
  }

  onBeforeMount(() => {
    loadList();
  });
</script>

<style lang="less" scoped></style>
