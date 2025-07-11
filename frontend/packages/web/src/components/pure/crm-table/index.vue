<template>
  <div class="flex h-full flex-col overflow-hidden">
    <slot name="tableTop"></slot>
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
      ref="tableRef"
      v-bind="{ scrollX: scrollXWidth, ...$attrs }"
      v-model:checked-row-keys="checkedRowKeys"
      :columns="currentColumns as TableColumns"
      :row-key="getRowKey"
      flex-height
      :class="`${props.notShowTableFilter ? 'not-show-filter' : ''} flex-1`"
      virtual-scroll
      @update:sorter="handleSorterChange"
      @update:filters="handleFiltersChange"
      @update:checked-row-keys="handleCheck"
      @scroll="handleScroll"
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
      v-if="!!attrs.showPagination"
      class="mt-[16px]"
      v-bind="{ ...(attrs.crmPagination || {}) }"
      :checked-count="checkedRowKeys.length"
      hide-pagination
      @handle-page-change="handlePageChange"
      @handle-page-size-change="handlePageSizeChange"
    />
  </div>
</template>

<script lang="ts" setup>
  import { NCheckbox, NDataTable, NTooltip } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import { OperatorEnum } from '@lib/shared/enums/commonEnum';
  import { SpecialColumnEnum, TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { FilterConditionItem, SortParams, TableDraggedParams } from '@lib/shared/models/common';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmPagination from '@/components/pure/crm-pagination/index.vue';
  import type { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import CrmTagGroup from '@/components/pure/crm-tag-group/index.vue';
  import BatchAction from './components/batchAction.vue';
  import ColumnSetting from './components/columnSetting.vue';

  import useTableStore from '@/hooks/useTableStore';
  import { useAppStore } from '@/store';
  import { hasAnyPermission } from '@/utils/permission';

  import { BatchActionConfig } from './type';
  import type { DataTableBaseColumn, DataTableFilterState, DataTableRowKey, DataTableSortState } from 'naive-ui';
  import type { InternalRowData, TableColumns } from 'naive-ui/es/data-table/src/interface';
  import Sortable from 'sortablejs';

  const appStore = useAppStore();

  const props = defineProps<{
    columns: CrmDataTableColumn[];
    tableRowKey?: string;
    actionConfig?: BatchActionConfig; // 批量操作
    notShowTableFilter?: boolean; // 不显示表头筛选
    draggable?: boolean; // 允许拖拽
  }>();
  const emit = defineEmits<{
    (e: 'pageChange', value: number): void;
    (e: 'pageSizeChange', value: number): void;
    (e: 'filterChange', value: FilterConditionItem[]): void;
    (e: 'batchAction', value: ActionsItem): void;
    (e: 'sorterChange', value: SortParams): void;
    (e: 'rowKeyChange', keys: DataTableRowKey[], rows: InternalRowData[]): void;
    (e: 'drag', params: TableDraggedParams): void;
  }>();
  const attrs = useAttrs();
  const { t } = useI18n();
  const tableStore = useTableStore();

  const checkedRowKeys = defineModel<DataTableRowKey[]>('checkedRowKeys', { default: [] });

  const currentColumns = ref<CrmDataTableColumn[]>([]);

  // 处理排序和过滤图标
  function sorterAndFilterColumn(column: CrmDataTableColumn) {
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

    return { ...sorterColumn, ...filterColumn };
  }

  // 计算文本宽度
  function getTextWidth(text: string, fontSize = 14) {
    const span = document.createElement('span');
    span.style.visibility = 'hidden';
    span.style.position = 'absolute';
    span.style.fontSize = `${fontSize}px`;
    span.style.fontFamily = "'Helvetica Neue', Arial, 'PingFang SC', 'Source Han Serif'";
    span.innerText = text;
    document.body.appendChild(span);
    const width = span.offsetWidth;
    document.body.removeChild(span);
    return width;
  }

  // 计算列的最小宽度
  function calculateColumnMinWidth(column: CrmDataTableColumn) {
    if (column.minWidth) return column.minWidth;

    let minWidth = 80;

    // 计算标题文本宽度
    const title = typeof column.title === 'string' ? column.title : (column.key as string);
    const textWidth = getTextWidth(title);
    // 增加图标空间
    const iconSpace = (column.sorter ? 23 : 0) + (column.filter ? 24 : 0);

    // 计算总宽度 = 文本宽度 + 图标空间 + 左右padding(各16px)
    minWidth = Math.ceil(textWidth + iconSpace + 32);
    return minWidth;
  }

  async function initColumn(hasInitStore = false) {
    const hasSelectedColumn = props.columns.find((e) => e.type === SpecialColumnEnum.SELECTION);
    // 无任何权限不展示选择列
    const propsColumns =
      !hasAnyPermission((attrs?.permission || []) as string[]) && hasSelectedColumn
        ? props.columns.filter((e) => e.type !== SpecialColumnEnum.SELECTION)
        : props.columns;

    // 将render去掉，防止报错
    let columns = cloneDeep(propsColumns).map((column: CrmDataTableColumn) => {
      const _col = { ...column };
      Object.keys(_col).forEach((key) => {
        if (typeof _col[key as keyof CrmDataTableColumn] === 'function') {
          delete _col[key as keyof CrmDataTableColumn];
        }
      });
      return _col;
    });
    if (!hasInitStore && attrs.tableKey) {
      await tableStore.initColumn(attrs.tableKey as TableKeyEnum, columns);
    }
    if (attrs.showSetting) {
      columns = await tableStore.getShowInTableColumns(attrs.tableKey as TableKeyEnum);
    }

    const noDragColumns = columns.map((column) => {
      const defaultRender = (row: Record<string, any>) => row[column.key as string] || '-';
      // 添加上render
      let render = props.columns.find((item) => item.key === column.key)?.render || defaultRender;
      const disabled = props.columns.find((item) => item.key === column.key)?.disabled;
      const selectTooltip = props.columns.find((item) => item.key === column.key)?.selectTooltip;

      if (column.isTag) {
        render = (row: Record<string, any>) =>
          row[column.key as string]?.length
            ? h(CrmTagGroup, {
                tags: row[column.key as string] || [],
                ...column.tagGroupProps,
              })
            : '-';
        // 预留标签组最小列宽
        column.minWidth = 90;
      }

      // 选择列
      if (column.type === SpecialColumnEnum.SELECTION) {
        if (selectTooltip) {
          return {
            title: () => {
              // 计算可选择的行的ID（排除disabled的行）
              const selectableRowIds: string[] = (attrs.data as [])
                .filter((row: Record<string, any>) => !disabled || !disabled(row))
                .map((row: Record<string, any>) => row.id);

              // 计算已选中的可选项
              const selectedSelectableCount = selectableRowIds.filter((id) =>
                checkedRowKeys.value?.includes(id)
              ).length;

              return h(NCheckbox, {
                checked: selectableRowIds.length > 0 && selectedSelectableCount === selectableRowIds.length,
                indeterminate: selectedSelectableCount > 0 && selectedSelectableCount < selectableRowIds.length,
                disabled: selectableRowIds.length === 0,
                onUpdateChecked: (val: boolean) => {
                  const currentSelected = new Set(checkedRowKeys.value || []);
                  if (val) {
                    selectableRowIds.forEach((id) => currentSelected.add(id));
                  } else {
                    selectableRowIds.forEach((id) => currentSelected.delete(id));
                  }
                  checkedRowKeys.value = Array.from(currentSelected);
                },
              });
            },
            key: SpecialColumnEnum.SELECTION,
            fixed: 'left',
            width: 46,
            titleAlign: 'center',
            render: (row: Record<string, any>) => {
              const isDisabled = disabled?.(row);
              const showTooltip = selectTooltip?.showTooltip?.(row) && isDisabled;
              const checkbox = h(NCheckbox, {
                disabled: isDisabled,
                checked: checkedRowKeys.value?.includes(row.id),
                onUpdateChecked: (checked: boolean) => {
                  // 处理行选择变化
                  const newSelected = new Set(checkedRowKeys.value || []);
                  if (checked) {
                    newSelected.add(row.id);
                  } else {
                    newSelected.delete(row.id);
                  }
                  checkedRowKeys.value = Array.from(newSelected);
                },
              });

              return showTooltip
                ? h(NTooltip, {}, { trigger: () => checkbox, default: () => selectTooltip?.tooltipText })
                : checkbox;
            },
          };
        }
        return {
          ...column,
          width: 46,
          titleAlign: 'center',
          disabled,
        };
      }
      // 操作列
      if (column.key === SpecialColumnEnum.OPERATION) {
        return {
          ...column,
          resizable: false,
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

      return {
        ...column,
        ...sorterAndFilterColumn(column),
        titleAlign: 'left',
        resizable: column.resizable !== undefined ? column.resizable : true,
        render,
        maxWidth: 600,
        minWidth: calculateColumnMinWidth(column),
      };
    });

    currentColumns.value = [
      ...(props.draggable
        ? [
            {
              title: '',
              key: SpecialColumnEnum.SELECTION,
              width: 40,
              render: () =>
                h(
                  'div',
                  { class: 'crm-table-data-draggable-handle' },
                  h(CrmIcon, {
                    type: 'iconicon_move',
                    size: 14,
                    class: 'sort-handle text-[var(--text-n4)]',
                  })
                ),
            },
          ]
        : []),
      ...noDragColumns,
    ] as CrmDataTableColumn[];
  }

  watch(
    () => props.columns,
    () => {
      initColumn();
    },
    { immediate: true }
  );

  const tableRef = ref();
  watch(
    () => props.notShowTableFilter,
    (val: boolean) => {
      if (val) {
        tableRef.value?.filter(null);
      }
    }
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
    if (!attrs.showPagination) return;
    emit('sorterChange', !sorter.order ? {} : { name: sorter.columnKey as string, type: sortOrder });
  }

  function handleFiltersChange(filters: DataTableFilterState, initiatorColumn: DataTableBaseColumn) {
    if (!attrs.showPagination) return;
    const filterList = Object.entries(filters)
      .filter(([, value]) => (value as string[])?.length > 0)
      .map(([key, value]) => ({
        name: key,
        value,
        multipleValue:
          initiatorColumn.key === key ? !!(initiatorColumn as CrmDataTableColumn).filterMultipleValue : undefined,
        operator: OperatorEnum.IN,
      }));
    emit('filterChange', filterList);
  }

  function handleClear() {
    checkedRowKeys.value = [];
  }

  const handleBatchAction = (item: ActionsItem) => {
    emit('batchAction', item);
  };

  const selectedRows = ref<InternalRowData[]>([]);

  function handleCheck(rowKeys: DataTableRowKey[], rows: InternalRowData[]) {
    const rowKeySet = new Set(rowKeys);
    // 去掉被取消选中的项
    selectedRows.value = selectedRows.value.filter((row) => row && rowKeySet.has(getRowKey(row)));
    const existingIds = new Set(selectedRows.value.map((row) => row.id));
    const newRows = rows.filter((row) => row && !existingIds.has(row.id));
    // 追加新项
    selectedRows.value = [...selectedRows.value, ...newRows];
    emit('rowKeyChange', rowKeys, selectedRows.value);
  }

  function handleScroll(e: Event) {
    const target = e.target as HTMLElement;
    const pagination = attrs.crmPagination as any;
    // 处理有纵向滚动的情况
    if (
      target.scrollHeight > target.clientHeight && // 有纵向滚动条
      target.scrollHeight - target.scrollTop === target.clientHeight && // 滚动到底部
      pagination
    ) {
      if (pagination.itemCount > pagination.page * pagination.pageSize) {
        emit('pageChange', pagination.page + 1);
      }
    }
  }

  onMounted(() => {
    checkedRowKeys.value = [];
  });

  const sortable = ref();
  async function setDraggerSort() {
    const observer = new MutationObserver((mutations, obs) => {
      const el = tableRef.value?.$el?.querySelector('.n-data-table tbody');
      if (el) {
        obs.disconnect();
        sortable.value = Sortable.create(el, {
          ghostClass: 'sortable-ghost',
          handle: '.crm-table-data-draggable-handle',
          setData(dataTransfer: any) {
            dataTransfer.setData('Text', '');
          },
          onEnd: (e: any) => {
            const { oldIndex, newIndex } = e;
            const data = attrs.data as any[];
            const rowKey = props.tableRowKey || 'id';

            const moveId = data[oldIndex][rowKey];

            let targetId;
            let moveMode: 'AFTER' | 'BEFORE';

            if (newIndex >= data.length) {
              targetId = data[data.length - 1][rowKey];
              moveMode = 'AFTER';
            } else if (newIndex === 0) {
              targetId = data[0][rowKey];
              moveMode = 'BEFORE';
            } else if (oldIndex < newIndex) {
              targetId = data[newIndex][rowKey];
              moveMode = 'AFTER';
            } else {
              targetId = data[newIndex][rowKey];
              moveMode = 'BEFORE';
            }

            emit('drag', {
              targetId,
              moveId,
              moveMode,
              orgId: appStore.orgId,
            });
          },
        });
      }
    });

    observer.observe(tableRef.value.$el, {
      childList: true,
      subtree: true,
    });
  }

  onMounted(() => {
    if (props.draggable) {
      setDraggerSort();
    }
  });

  const scrollXWidth = computed(() => currentColumns.value.reduce((prev, curr) => prev + (curr.width as number), 0));
</script>

<style lang="less">
  .n-data-table-th--fixed-left {
    background-color: var(--text-n10) !important;
  }

  // 缩小表头区域触发排序，减少拖拽列宽触发排序：官方推荐解决方案：不在排序按钮附近放置拖拽手柄，手动规避冲突区域
  .n-data-table-th {
    pointer-events: none;
    .n-data-table-th__title-wrapper {
      pointer-events: auto;
    }
    .n-data-table-resize-button {
      pointer-events: auto;
    }
    .n-data-table-filter {
      pointer-events: auto;
    }
    &.n-data-table-th--selection {
      pointer-events: auto;
    }
  }
</style>

<style lang="less" scoped>
  :deep(.sort-up-icon) {
    transform: translateY(4px);
  }
  :deep(.sort-down-icon) {
    transform: translateY(-11px);
  }
  .not-show-filter {
    :deep(.n-data-table-filter) {
      display: none;
    }
  }
</style>
