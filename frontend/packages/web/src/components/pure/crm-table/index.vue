<template>
  <BatchAction
    v-if="props.actionConfig"
    :select-row-count="checkedRowKeys.length"
    size="medium"
    :action-config="props.actionConfig"
    @clear="handleClear"
    @batch-action="handleBatchAction"
  >
    <template #actionLeft>
      <slot name="actionLeft"></slot>
    </template>
    <template #actionRight>
      <div class="flex items-center gap-[8px]">
        <slot name="actionRight"></slot>
      </div>
    </template>
  </BatchAction>
  <n-data-table
    v-bind="{ ...$attrs }"
    v-model:checked-row-keys="checkedRowKeys"
    :columns="currentColumns as TableColumns"
    :row-key="getRowKey"
    @update:sorter="handleSorterChange"
    @update:filters="handleFiltersChange"
    @update:checked-row-keys="handleCheck"
  >
    <template #empty>
      <div class="w-full">
        <slot name="empty">
          <div class="flex items-center justify-center">
            <span class="text-[14px] text-[var(--text-n4)]">{{ t('common.noData') }}</span>
          </div>
        </slot>
      </div>
    </template>
  </n-data-table>
  <CrmPagination
    class="mt-[16px]"
    v-bind="{ ...(attrs.crmPagination || {}) }"
    @handle-page-change="handlePageChange"
    @handle-page-size-change="handlePageSizeChange"
  />
</template>

<script lang="ts" setup>
  import { NDataTable } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import { SpecialColumnEnum, TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import type { SortParams } from '@lib/shared/models/common';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmPagination from '@/components/pure/crm-pagination/index.vue';
  import type { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import CrmTagGroup from '@/components/pure/crm-tag-group/index.vue';
  import BatchAction from './components/batchAction.vue';
  import ColumnSetting from './components/columnSetting.vue';

  import { useI18n } from '@/hooks/useI18n';
  import useTableStore from '@/hooks/useTableStore';

  import { BatchActionConfig } from './type';
  import type { DataTableFilterState, DataTableRowKey, DataTableSortState } from 'naive-ui';
  import type { InternalRowData, TableColumns } from 'naive-ui/es/data-table/src/interface';

  const props = defineProps<{
    columns: CrmDataTableColumn[];
    tableRowKey?: string;
    actionConfig?: BatchActionConfig; // 批量操作
  }>();
  const emit = defineEmits<{
    (e: 'pageChange', value: number): void;
    (e: 'pageSizeChange', value: number): void;
    (e: 'filterChange', value: DataTableFilterState): void;
    (e: 'batchAction', value: ActionsItem): void;
    (e: 'sorterChange', value: SortParams): void;
    (e: 'rowKeyChange', keys: DataTableRowKey[], rows: InternalRowData[]): void;
  }>();
  const attrs = useAttrs();
  const { t } = useI18n();
  const tableStore = useTableStore();

  const checkedRowKeys = defineModel<DataTableRowKey[]>('checkedRowKeys', { default: [] });

  const currentColumns = ref<CrmDataTableColumn[]>([]);

  async function initColumn(hasInitStore = false) {
    // 将render去掉，防止报错
    let columns = cloneDeep(props.columns).map((column: CrmDataTableColumn) => {
      const _col = { ...column };
      Object.keys(_col).forEach((key) => {
        if (typeof _col[key as keyof CrmDataTableColumn] === 'function') {
          delete _col[key as keyof CrmDataTableColumn];
        }
      });
      return _col;
    });
    if (attrs.showSetting) {
      if (!hasInitStore && attrs.showSetting && attrs.tableKey) {
        await tableStore.initColumn(attrs.tableKey as TableKeyEnum, columns);
      }
      columns = await tableStore.getShowInTableColumns(attrs.tableKey as TableKeyEnum);
    }
    currentColumns.value = columns.map((column) => {
      // 添加上render
      let render = props.columns.find((item) => item.key === column.key)?.render;

      if (column.isTag) {
        render = (row: Record<string, any>) =>
          h(CrmTagGroup, {
            tags: row[column.key as string] || [],
            ...column.tagGroupProps,
          });
      }

      // 选择列
      if (column.type === SpecialColumnEnum.SELECTION) {
        return {
          ...column,
          width: 46,
          titleAlign: 'center',
        };
      }
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
                    initColumn(true);
                  },
                })
              );
            }
            return h('div', { class: 'flex items-center gap-[8px]' }, children);
          },
          render,
        };
      }

      // 排序图标处理
      const sorterColumn = column.sorter
        ? {
            renderSorterIcon: (options: { order: 'descend' | 'ascend' | false }) => {
              return h('div', [
                h(CrmIcon, {
                  type: 'iconicon_chevron_up',
                  class: 'h-[8px] sort-up-icon',
                  color: options.order === 'ascend' ? 'var( --primary-8)' : 'var(--text-n2)',
                }),
                h(CrmIcon, {
                  type: 'iconicon_chevron_down',
                  class: 'h-[8px] sort-down-icon',
                  color: options.order === 'descend' ? 'var( --primary-8)' : 'var(--text-n2)',
                }),
              ]);
            },
          }
        : {};

      // 过滤图标处理
      const filterColumn = column.filter
        ? {
            renderFilterIcon: (options: { active: boolean; show: boolean }) => {
              return h(CrmIcon, {
                type: 'iconicon_filter',
                size: 16,
                color: options.active ? 'var( --primary-8)' : 'var(--text-n2)',
              });
            },
          }
        : {};
      return {
        ...column,
        ...sorterColumn,
        ...filterColumn,
        titleAlign: 'left',
        resizable: true,
        render,
      };
    });
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
    currentColumns.value.forEach((column) => {
      if (column.sortOrder === undefined) return;
      if (!sorter) {
        column.sortOrder = false;
        return;
      }
      if (column.key === sorter.columnKey) {
        column.sortOrder = sorter.order;
      } else {
        column.sortOrder = false;
      }
    });
    let sortOrder = '';
    if (sorter.order === 'ascend') {
      sortOrder = 'asc';
    } else if (sorter.order === 'descend') {
      sortOrder = 'desc';
    }
    emit('sorterChange', !sorter.order ? {} : { name: sorter.columnKey as string, type: sortOrder });
  }

  function handleFiltersChange(filters: DataTableFilterState) {
    emit('filterChange', filters);
  }

  function handleClear() {
    checkedRowKeys.value = [];
  }

  const handleBatchAction = (item: ActionsItem) => {
    emit('batchAction', item);
  };

  function handleCheck(rowKeys: DataTableRowKey[], rows: InternalRowData[]) {
    emit('rowKeyChange', rowKeys, rows);
  }
</script>

<style lang="less">
  .n-data-table-th--fixed-left {
    background-color: var(--text-n10) !important;
  }
</style>

<style lang="less" scoped>
  :deep(.sort-up-icon) {
    transform: translateY(4px);
  }
  :deep(.sort-down-icon) {
    transform: translateY(-11px);
  }
</style>
