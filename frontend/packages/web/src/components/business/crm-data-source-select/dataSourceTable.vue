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

  import { FieldDataSourceTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { CommonList } from '@lib/shared/models/common';

  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';

  import {
    getCustomerOptions,
    getFieldClueList,
    getFieldContactList,
    getFieldCustomerList,
    getFieldOpportunityList,
    getFieldProductList,
    getUserOptions,
  } from '@/api/modules';

  import { InternalRowData, RowData } from 'naive-ui/es/data-table/src/interface';

  const props = withDefaults(
    defineProps<{
      sourceType: FieldDataSourceTypeEnum;
      multiple?: boolean;
      disabledSelection?: (row: RowData) => boolean;
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
      width: 46,
      disabled(row: RowData) {
        return props.disabledSelection ? props.disabledSelection(row) : false;
      },
      resizable: false,
    },
    {
      title: t('common.name'),
      key: 'name',
      width: 200,
      ellipsis: {
        tooltip: true,
      },
      resizable: false,
    },
  ];

  if (props.sourceType === FieldDataSourceTypeEnum.CONTACT) {
    columns.push({
      title: t('crmFormDesign.phone'),
      key: 'phone',
      resizable: false,
    });
  }

  const sourceApi: Record<FieldDataSourceTypeEnum, (data: any) => Promise<CommonList<any>>> = {
    [FieldDataSourceTypeEnum.BUSINESS]: getFieldOpportunityList,
    [FieldDataSourceTypeEnum.CLUE]: getFieldClueList,
    [FieldDataSourceTypeEnum.CONTACT]: getFieldContactList,
    [FieldDataSourceTypeEnum.CUSTOMER]: getFieldCustomerList,
    [FieldDataSourceTypeEnum.PRODUCT]: getFieldProductList,
    [FieldDataSourceTypeEnum.CUSTOMER_OPTIONS]: getCustomerOptions,
    [FieldDataSourceTypeEnum.USER_OPTIONS]: getUserOptions,
  };

  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable(sourceApi[props.sourceType], {
    columns,
    showSetting: false,
  });

  const keyword = ref('');

  function searchData() {
    setLoadListParams({ keyword: keyword.value });
    loadList();
  }

  function handleRowKeyChange(keys: DataTableRowKey[], _rows: InternalRowData[]) {
    selectedKeys.value = keys;
    selectedRows.value = _rows;
  }

  onBeforeMount(() => {
    searchData();
  });
</script>

<style lang="less" scoped></style>
