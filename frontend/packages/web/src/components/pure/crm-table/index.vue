<template>
  <n-data-table
    v-bind="{ ...$attrs }"
    v-model:checked-row-keys="checkedRowKeys"
    :columns="currentColumns"
    :row-key="getRowKey"
    @update:sorter="handleSorterChange"
    @update:filters="handleFiltersChange"
    @update:checked-row-keys="handleCheck"
    @update:page="handlePageChange"
    @update:page-size="handlePageSizeChange"
  />
</template>

<script lang="ts" setup>
  import { NDataTable } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import type { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import ColumnSetting from './components/columnSetting.vue';

  import { useI18n } from '@/hooks/useI18n';
  import { useTableStore } from '@/store';

  import { SpecialColumnEnum, TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import type { DataTableFilterState, DataTableRowKey, DataTableSortState } from 'naive-ui';

  const props = defineProps<{
    columns: CrmDataTableColumn[];
    tableRowKey?: string;
  }>();
  const emit = defineEmits<{
    (e: 'pageChange', value: number): void;
    (e: 'pageSizeChange', value: number): void;
    (e: 'sorterChange', value: { [key: string]: string }): void;
    (e: 'filterChange', value: DataTableFilterState): void;
  }>();
  const attrs = useAttrs();
  const { t } = useI18n();
  const tableStore = useTableStore();

  const checkedRowKeys = defineModel<DataTableRowKey[]>('checkedRowKeys', { default: [] });

  const currentColumns = ref<CrmDataTableColumn[]>([]);

  // TODO lmy 设置列
  async function initColumn() {
    let columns = cloneDeep(props.columns);
    if (attrs.showSetting) {
      columns = await tableStore.getShowInTableColumns(attrs.tableKey as TableKeyEnum);
      currentColumns.value = columns.map((column) => {
        // 操作列
        if (column.key === SpecialColumnEnum.OPERATION) {
          return {
            ...column,
            title() {
              const children = [h('div', t('common.operation'))];
              if (attrs.showSetting) {
                children.push(
                  h(ColumnSetting, {
                    tableKey: attrs.tableKey as TableKeyEnum,
                    onChangeColumnsSetting: () => {
                      initColumn();
                    },
                  })
                );
              }
              return h('div', { class: 'flex items-center gap-[8px]' }, children);
            },
          };
        }
        return column;
      });
    } else {
      currentColumns.value = columns;
    }
  }

  watch(
    () => props.columns,
    () => {
      initColumn();
    },
    { immediate: true }
  );

  function getRowKey(rowData: Record<string, any>) {
    return props.tableRowKey ? rowData[props.tableRowKey] : rowData.id;
  }

  function handlePageChange(page: number) {
    emit('pageChange', page);
  }
  function handlePageSizeChange(pageSize: number) {
    emit('pageSizeChange', pageSize);
  }

  function handleSorterChange(sorter: DataTableSortState) {
    let sortOrder = '';
    if (sorter.order === 'ascend') {
      sortOrder = 'asc';
    } else if (sorter.order === 'descend') {
      sortOrder = 'desc';
    }
    emit('sorterChange', !sorter.order ? {} : { [sorter.columnKey]: sortOrder });
  }

  function handleFiltersChange(filters: DataTableFilterState) {
    emit('filterChange', filters);
  }

  function handleCheck(rowKeys: DataTableRowKey[]) {
    console.log('🤔️ => handleCheck', rowKeys);
  }
</script>
