<template>
  <div
    ref="fullscreenTargetRef"
    class="crm-data-source-table relative bg-[var(--text-n10)]"
    :style="{
      height: isFullScreen ? '100%' : '60vh',
      padding: isFullScreen ? '16px' : '0',
    }"
  >
    <CrmTable
      ref="crmTableRef"
      v-model:checked-row-keys="selectedKeys"
      v-bind="propsRes"
      :fullscreen-target-ref="fullscreenTargetRef"
      @page-change="propsEvent.pageChange"
      @page-size-change="propsEvent.pageSizeChange"
      @sorter-change="propsEvent.sorterChange"
      @filter-change="propsEvent.filterChange"
      @row-key-change="handleRowKeyChange"
      @refresh="searchData"
    >
      <template #tableTop>
        <CrmSearchInput
          v-model:value="keyword"
          class="!w-[240px]"
          :placeholder="
            props.sourceType === FieldDataSourceTypeEnum.CONTACT
              ? t('common.searchByNamePhone')
              : t('common.searchByName')
          "
          @search="searchData"
        />
      </template>
    </CrmTable>
  </div>
</template>

<script setup lang="ts">
  import { DataTableRowKey } from 'naive-ui';

  import { FieldDataSourceTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { CommonList } from '@lib/shared/models/common';

  import { FilterResult } from '@/components/pure/crm-advance-filter/type';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';

  import {
    getCustomerOptions,
    getFieldClueList,
    getFieldContactList,
    getFieldContractList,
    getFieldCustomerList,
    getFieldOpportunityList,
    getFieldPriceList,
    getFieldProductList,
    getFieldQuotationList,
    getUserOptions,
  } from '@/api/modules';

  import { InternalRowData, RowData } from 'naive-ui/es/data-table/src/interface';

  const props = withDefaults(
    defineProps<{
      sourceType: FieldDataSourceTypeEnum;
      multiple?: boolean;
      disabledSelection?: (row: RowData) => boolean;
      filterParams?: FilterResult;
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
      ellipsis: {
        tooltip: true,
      },
      resizable: false,
    },
  ];

  if (props.sourceType === FieldDataSourceTypeEnum.CONTACT) {
    columns.push(
      {
        title: t('crmFormDesign.phone'),
        key: 'phone',
        resizable: false,
      },
      {
        title: t('crmFormDesign.customer'),
        key: 'customerName',
        ellipsis: {
          tooltip: true,
        },
        resizable: true,
      }
    );
  }

  const sourceApi: Record<FieldDataSourceTypeEnum, (data: any) => Promise<CommonList<any>>> = {
    [FieldDataSourceTypeEnum.BUSINESS]: getFieldOpportunityList,
    [FieldDataSourceTypeEnum.CLUE]: getFieldClueList,
    [FieldDataSourceTypeEnum.CONTACT]: getFieldContactList,
    [FieldDataSourceTypeEnum.CUSTOMER]: getFieldCustomerList,
    [FieldDataSourceTypeEnum.PRODUCT]: getFieldProductList,
    [FieldDataSourceTypeEnum.CUSTOMER_OPTIONS]: getCustomerOptions,
    [FieldDataSourceTypeEnum.USER_OPTIONS]: getUserOptions,
    [FieldDataSourceTypeEnum.CONTRACT]: getFieldContractList,
    [FieldDataSourceTypeEnum.PRICE]: getFieldPriceList,
    [FieldDataSourceTypeEnum.QUOTATION]: getFieldQuotationList,
  };

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();
  const { propsRes, propsEvent, loadList, setAdvanceFilter, setLoadListParams } = useTable(
    sourceApi[props.sourceType],
    {
      columns,
      showSetting: false,
      crmPagination: {
        showSizePicker: false,
      },
      containerClass: '.crm-data-source-select-modal',
    }
  );

  const keyword = ref('');
  const fullscreenTargetRef = ref();

  function searchData(_keyword?: string) {
    if (props.filterParams) {
      setAdvanceFilter(props.filterParams);
    }
    setLoadListParams({ keyword: _keyword !== undefined ? _keyword : keyword.value });
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  function handleRowKeyChange(keys: DataTableRowKey[], _rows: InternalRowData[]) {
    selectedKeys.value = keys;
    selectedRows.value = _rows;
  }

  const isFullScreen = computed(() => crmTableRef.value?.isFullScreen);

  onBeforeMount(() => {
    searchData();
  });
</script>

<style lang="less">
  .crm-data-source-table {
    .n-checkbox--disabled {
      .check-icon {
        opacity: 1 !important;
        transform: scale(1) !important;
      }
    }
    .n-radio--disabled {
      .n-radio__dot::before {
        opacity: 1 !important;
        transform: scale(1) !important;
      }
    }
  }
</style>
