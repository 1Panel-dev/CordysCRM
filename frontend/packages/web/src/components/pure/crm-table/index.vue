<template>
  <div
    ref="tableFullRef"
    class="relative flex h-full flex-col overflow-hidden"
    :class="[isFullScreen && !props.fullscreenTargetRef ? 'bg-[var(--text-n10)] px-[16px] pt-[16px]' : '', props.class]"
  >
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
          <ColumnSetting
            v-if="attrs.showSetting && props.actionConfig"
            :table-key="attrs.tableKey as TableKeyEnum"
            @change-columns-setting="changeColumnsSetting"
          />

          <n-button
            v-if="!attrs.hiddenAllScreen && props.actionConfig"
            type="default"
            class="outline--secondary px-[8px]"
            @click="toggleFullScreen"
          >
            <CrmIcon
              class="text-[var(--text-n1)]"
              :type="isFullScreen ? 'iconicon_off_screen' : 'iconicon_full_screen_one'"
              :size="16"
            />
          </n-button>
          <n-button
            v-if="!attrs.hiddenRefresh"
            type="default"
            class="outline--secondary px-[8px]"
            @click="emit('refresh')"
          >
            <CrmIcon class="text-[var(--text-n1)]" type="iconicon_refresh" :size="16" />
          </n-button>
        </div>
      </template>
    </BatchAction>
    <div v-else class="mb-[16px] flex w-full items-center gap-[8px]">
      <div class="flex-1">
        <slot name="tableTop"></slot>
      </div>
      <ColumnSetting
        v-if="attrs.showSetting && !props.actionConfig"
        :table-key="attrs.tableKey as TableKeyEnum"
        @change-columns-setting="changeColumnsSetting"
      />
      <n-button
        v-if="!attrs.hiddenAllScreen && !props.actionConfig"
        type="default"
        class="outline--secondary px-[8px]"
        @click="toggleFullScreen"
      >
        <CrmIcon
          class="text-[var(--text-n1)]"
          :type="isFullScreen ? 'iconicon_off_screen' : 'iconicon_full_screen_one'"
          :size="16"
        />
      </n-button>
      <n-button v-if="!attrs.hiddenRefresh" type="default" class="outline--secondary px-[8px]" @click="emit('refresh')">
        <CrmIcon class="text-[var(--text-n1)]" type="iconicon_refresh" :size="16" />
      </n-button>
    </div>
    <slot name="view"></slot>
    <slot name="other"></slot>
    <template v-if="!props.notShowTable">
      <n-data-table
        ref="tableRef"
        v-bind="{ scrollX: scrollXWidth, ...$attrs }"
        v-model:checked-row-keys="checkedRowKeys"
        :columns="currentColumns as TableColumns"
        :row-key="getRowKey"
        flex-height
        :class="`${props.notShowTableFilter ? 'not-show-filter' : ''} ${
          attrs.showSetting ? crmTableLayoutClass : ''
        } flex-1`"
        :virtual-scroll="!props.notVirtualScroll"
        :virtual-scroll-x="props.virtualScrollX"
        :min-row-height="tableLineHeight"
        :header-height="tableLineHeight"
        :row-props="rowProps"
        :children-key="props.childrenKey"
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
    </template>
    <div
      v-if="!attrs.hiddenTotal || (attrs.hiddenTotal && isFullScreen) || hasFinished"
      class="crm-table-bottom-tip flex text-center"
    >
      <div :class="`flex flex-1 items-start`">
        <div v-if="!attrs.hiddenTotal || (attrs.hiddenTotal && isFullScreen)">
          {{ t('crmPagination.total', { count: (attrs.crmPagination as PaginationProps)?.itemCount }) }}
        </div>
        <slot name="totalRight"></slot>
      </div>
      <div
        v-if="hasFinished && !attrs.loading && !props.notShowTable"
        :class="`-ml-[24px] flex flex-1 items-start ${
          !(!attrs.hiddenTotal || (attrs.hiddenTotal && isFullScreen)) ? 'items-center justify-center' : 'items-start'
        }`"
      >
        {{ t('crmTable.tableScrollFinishedTip') }}
      </div>
      <div v-if="!attrs.hiddenBackToTop" class="flex items-center">
        <n-button size="small" type="primary" text class="text-btn-primary" @click="backToTop">
          <template #icon>
            <CrmIcon type="iconicon_backtop" :size="14" />
          </template>
          <div class="text-[14px]">{{ t('common.backToTop') }}</div>
        </n-button>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { NButton, NCheckbox, NDataTable, NTooltip } from 'naive-ui';
  import { cloneDeep, debounce } from 'lodash-es';

  import { OperatorEnum } from '@lib/shared/enums/commonEnum';
  import { SpecialColumnEnum, TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { FilterConditionItem, SortParams, TableDraggedParams } from '@lib/shared/models/common';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import type { CrmDataTableColumn, TableStorageConfigItem } from '@/components/pure/crm-table/type';
  import CrmTagGroup from '@/components/pure/crm-tag-group/index.vue';
  import BatchAction from './components/batchAction.vue';
  import ColumnSetting from './components/columnSetting.vue';
  import FilterMenu from './components/filterMenu.vue';

  import useFullScreen from '@/hooks/useFullScreen';
  import useLocalForage from '@/hooks/useLocalForage';
  import useTableStore from '@/hooks/useTableStore';
  import { useAppStore } from '@/store';
  import useViewStore from '@/store/modules/view';
  import { hasAnyPermission } from '@/utils/permission';

  import { BatchActionConfig } from './type';
  import type {
    DataTableBaseColumn,
    DataTableFilterState,
    DataTableRowKey,
    DataTableSortState,
    PaginationProps,
  } from 'naive-ui';
  import type { FilterOption, InternalRowData, TableColumns } from 'naive-ui/es/data-table/src/interface';
  import Sortable from 'sortablejs';

  const appStore = useAppStore();
  const viewStore = useViewStore();

  const props = defineProps<{
    columns: CrmDataTableColumn[];
    tableRowKey?: string;
    actionConfig?: BatchActionConfig; // 批量操作
    notShowTable?: boolean; // 不展示表格
    notShowTableFilter?: boolean; // 不显示表头筛选
    draggable?: boolean; // 允许拖拽
    dragMoveValidator?: (fromRow: any, toRow: any) => boolean; // 拖拽限制
    notVirtualScroll?: boolean; // 不开启竖向虚拟滚动
    virtualScrollX?: boolean; // 是否开启横向虚拟滚动
    fullscreenTargetRef?: HTMLElement | null;
    class?: string; // 自定义样式类
    childrenKey?: string; // 子节点字段名
  }>();
  const emit = defineEmits<{
    (e: 'pageChange', value: number): void;
    (e: 'pageSizeChange', value: number): void;
    (e: 'filterChange', value: FilterConditionItem[]): void;
    (e: 'batchAction', value: ActionsItem): void;
    (e: 'sorterChange', value: SortParams): void;
    (e: 'rowKeyChange', keys: DataTableRowKey[], rows: InternalRowData[]): void;
    (e: 'drag', params: TableDraggedParams): void;
    (e: 'refresh'): void;
  }>();
  const attrs = useAttrs();
  const { t } = useI18n();
  const tableStore = useTableStore();
  const { getItem, setItem } = useLocalForage();

  const tableFullRef = ref<HTMLElement | null>(null);
  const tableRef = ref();
  // 实际使用的全屏目标
  const actualTargetRef = computed<HTMLElement | null>(() => {
    return props.fullscreenTargetRef ?? tableFullRef.value;
  });

  const { toggleFullScreen, isFullScreen } = useFullScreen(actualTargetRef, !!attrs.hiddenAllScreen);

  function scrollTo(options: { top?: number; left?: number }) {
    tableRef.value?.scrollTo(options);
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
        type: (initiatorColumn as CrmDataTableColumn).filedType,
      }));
    scrollTo({
      top: 0,
    });
    emit('filterChange', filterList);
  }
  const checkedRowKeys = defineModel<DataTableRowKey[]>('checkedRowKeys', { default: [] });

  const checkFilterMap = ref<Record<string, (string | number)[]>>({});
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
          renderFilterMenu: (actions: { hide: () => void }) => {
            return h(FilterMenu, {
              'filterOptions': column.filterOptions as unknown as FilterOption[],
              'columnKey': column.key as string,
              'filterApiKey': column?.remoteFilterApiKey,
              'filters': checkFilterMap.value, // 初始值
              'onUpdate:filters': (val) => {
                checkFilterMap.value = val;
              },
              'onReset': (filters: DataTableFilterState) => {
                tableRef.value?.filter(filters);
                handleFiltersChange(filters, column as DataTableBaseColumn);
                actions.hide();
              },
              'onFilter': (filters: DataTableFilterState) => {
                tableRef.value?.filter(filters);
                handleFiltersChange(filters, column as DataTableBaseColumn);
                actions.hide();
              },
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

  async function initColumn() {
    const specialColumnsKeys = [SpecialColumnEnum.DRAG, SpecialColumnEnum.SELECTION, SpecialColumnEnum.ORDER];
    const hasSelectedColumn = props.columns.find((e) => e.type === SpecialColumnEnum.SELECTION);
    const hasOrderColumn = props.columns.find((e) => e.key === SpecialColumnEnum.ORDER);
    const hasDisabledColumn = props.columns.find(
      (e) => e.columnSelectorDisabled && !specialColumnsKeys.includes(e.key as SpecialColumnEnum)
    );
    const dragColumn: CrmDataTableColumn[] = props.draggable
      ? [
          {
            title: '',
            key: SpecialColumnEnum.DRAG,
            width: 40,
            fixed: 'left',
            resizable: false,
          },
        ]
      : [];
    const hasPermission = hasAnyPermission((attrs?.permission || []) as string[]);
    const propsColumns = props.columns.filter(
      (c) =>
        c.key !== SpecialColumnEnum.DRAG && c.type !== SpecialColumnEnum.SELECTION && c.key !== SpecialColumnEnum.ORDER
    );

    if (hasDisabledColumn) {
      const disabledIndex = propsColumns.indexOf(hasDisabledColumn);
      if (disabledIndex > -1) {
        propsColumns.splice(disabledIndex, 1);
      }
    }

    const specialColumns = [
      ...dragColumn,
      ...(hasPermission && hasSelectedColumn ? [hasSelectedColumn] : []),
      ...(hasOrderColumn ? [hasOrderColumn] : []),
      ...(hasDisabledColumn ? [hasDisabledColumn] : []),
    ];

    // 将render去掉，防止报错
    let columns = specialColumns.concat(propsColumns);
    columns = cloneDeep(columns).map((column: CrmDataTableColumn) => {
      const _col = { ...column };
      Object.keys(_col).forEach((key) => {
        if (typeof _col[key as keyof CrmDataTableColumn] === 'function') {
          delete _col[key as keyof CrmDataTableColumn];
        }
      });
      return _col;
    });
    if (attrs.tableKey) {
      await tableStore.initColumn(attrs.tableKey as TableKeyEnum, columns);
    }
    if (attrs.showSetting) {
      columns = await tableStore.getShowInTableColumns(attrs.tableKey as TableKeyEnum);
    }

    currentColumns.value = columns.map((column) => {
      const defaultRender = (row: Record<string, any>) =>
        row[column.key as string] === undefined ||
        row[column.key as string] === null ||
        row[column.key as string] === ''
          ? '-'
          : row[column.key as string];
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
      }

      if (column.key === SpecialColumnEnum.DRAG) {
        render = () =>
          h(
            'div',
            { class: 'crm-table-data-draggable-handle' },
            h(CrmIcon, {
              type: 'iconicon_move',
              size: 14,
              class: 'sort-handle text-[var(--text-n4)]',
            })
          );

        return {
          ...column,
          render,
        };
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
          title: t('common.operation'),
          render,
        };
      }

      // 代码定义的初始化列宽
      const codeMinWidth = propsColumns.find((e) => e.key === column.key)?.minWidth;
      return {
        ...column,
        ...sorterAndFilterColumn(column),
        titleAlign: 'left',
        resizable: column.resizable !== undefined ? column.resizable : true,
        render,
        maxWidth: 300,
        minWidth: typeof codeMinWidth === 'number' ? codeMinWidth : calculateColumnMinWidth(column),
      };
    });
  }

  function getRowKey(rowData: Record<string, any>) {
    return props.tableRowKey ? rowData[props.tableRowKey] : rowData.id;
  }

  const crmTableLayoutClass = ref('');
  const layOut = ref<string>('');
  async function initLayoutType() {
    if (attrs.showSetting) {
      const layout = await tableStore.getTableLineHeight(attrs.tableKey as TableKeyEnum);
      layOut.value = layout;
      crmTableLayoutClass.value = layout === 'compact' ? 'crm-data-table-compact' : 'crm-data-table-loose';
    }
  }

  const tableLineHeight = computed(() => {
    if (attrs.showSetting) {
      return layOut.value === 'compact' ? 36 : 46;
    }
    return 46;
  });

  const rowProps = (_rowData: object, _rowIndex: number) => {
    return {
      'style': {
        height: layOut.value === 'compact' ? '36px' : '46px',
      },
      'data-id': getRowKey(_rowData),
    };
  };

  function changeColumnsSetting() {
    initColumn();
    initLayoutType();
  }

  function patchColKeys() {
    if (!attrs.tableKey) return;
    const colElements = tableFullRef.value?.querySelectorAll('table col');
    if (!colElements || !currentColumns.value) return;

    colElements.forEach((colEl, index) => {
      const colKey = (currentColumns.value[index] as CrmDataTableColumn)?.key;
      if (colKey !== undefined && colKey !== null) {
        colEl.setAttribute('data-col-key', String(colKey));
      }
    });
  }

  /**
   * 监听列宽变化，更新缓存的列配置，以记住列宽
   * 这里使用 ResizeObserver 来监听 col 元素的宽度变化
   */
  function listenColWidthChange() {
    let isFirstResize = true;
    // 监听表格 col 元素的宽度变化
    const colElements = tableFullRef.value?.querySelectorAll('table col');
    const handleResize = debounce(async () => {
      const tableColumnsMap = await getItem<TableStorageConfigItem>(attrs.tableKey as TableKeyEnum);
      if (attrs.tableKey && tableColumnsMap) {
        if (isFirstResize) {
          isFirstResize = false; // 第一次触发，跳过
          return;
        }
        // 按colKey更新宽度，避免隐藏列后，实际DOM的col数量、位置、顺序发生改变，导致列宽度错乱
        colElements?.forEach((e) => {
          const key = e.getAttribute('data-col-key');
          const col = tableColumnsMap.column.find((c) => c.key === key);
          if (col) {
            col.width = (e.computedStyleMap()?.get('width') as any)?.value || col.width;
          }
        });
        setItem(attrs.tableKey as TableKeyEnum, tableColumnsMap);
        currentColumns.value = currentColumns.value.map((col) => {
          const cachedColumn = tableColumnsMap.column.find((c) => c.key === col.key);
          return {
            ...col,
            width: cachedColumn?.width || col.width,
          };
        });
      }
    }, 300);
    const observer = new ResizeObserver((entries) => {
      entries.forEach(() => {
        handleResize();
      });
    });
    // 批量注册
    colElements?.forEach((col) => observer.observe(col));
  }

  watch(
    () => props.columns,
    async () => {
      await initColumn();
      await nextTick();
      patchColKeys();
      listenColWidthChange();
      initLayoutType();
    },
    { immediate: true }
  );

  watch(
    () => props.notShowTableFilter,
    (val: boolean) => {
      if (val) {
        checkFilterMap.value = {};
        tableRef.value?.filter(null);
      }
    }
  );

  function handleSorterChange(sorter?: DataTableSortState) {
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
    if (sorter?.order === 'ascend') {
      sortOrder = 'asc';
    } else if (sorter?.order === 'descend') {
      sortOrder = 'desc';
    }
    if (!attrs.showPagination) return;
    scrollTo({
      top: 0,
    });
    emit('sorterChange', !sorter?.order ? {} : { name: sorter.columnKey as string, type: sortOrder });
  }

  async function setColumnSort(viewId: string) {
    // 切换视图column有变化（如切到协作视图）的时候，会执行initColumn，initColumn后再设置排序
    setTimeout(async () => {
      const sortObj = await viewStore.getViewSort(attrs.tableKey as TableKeyEnum, viewId);
      if (sortObj && Object.keys(sortObj).length) {
        handleSorterChange({
          columnKey: sortObj.name as string,
          order: sortObj?.type === 'desc' ? 'descend' : 'ascend',
          sorter: true,
        });
      } else {
        handleSorterChange();
      }
    }, 300);
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

  const hasFinished = ref(false);
  function handleScroll(e: Event) {
    const target = e.target as HTMLElement;
    const pagination = attrs.crmPagination as any;
    hasFinished.value = false;
    // 处理有纵向滚动的情况
    if (
      target.scrollHeight > target.clientHeight &&
      target.scrollHeight - target.scrollTop - target.clientHeight <= 40 &&
      pagination &&
      !attrs.loading &&
      !hasFinished.value
    ) {
      if (pagination.itemCount > pagination.page * pagination.pageSize) {
        emit('pageChange', pagination.page + 1);
      } else {
        hasFinished.value = true;
      }
    }
  }

  const sortable = ref();
  function initSortable(el: HTMLElement) {
    sortable.value = Sortable.create(el, {
      ghostClass: 'sortable-ghost',
      handle: '.crm-table-data-draggable-handle',
      setData(dataTransfer: any) {
        dataTransfer.setData('Text', '');
      },
      onMove: (evt) => {
        if (!props.dragMoveValidator) return true;
        const draggedEl = evt.dragged as HTMLElement;
        const relatedEl = evt.related as HTMLElement;
        const children = Array.from(evt.from.children); // 所有 row（tr）
        const data = attrs.data as any[];
        const fromRow = data[children.indexOf(draggedEl)];
        const toRow = data[children.indexOf(relatedEl)];
        return props.dragMoveValidator(fromRow, toRow);
      },
      onEnd: (e: any) => {
        const { oldIndex, newIndex, item, to } = e;
        const draggedEl = item as HTMLElement;
        const draggedId = draggedEl.dataset.id;

        if (!draggedId) return;

        const newOrderEls = Array.from(to.children) as HTMLElement[];
        const newIds = newOrderEls.map((childrenEl) => childrenEl.dataset.id);

        let targetId: string | undefined;
        let moveMode: 'AFTER' | 'BEFORE' = 'BEFORE';

        // 优先取后一项作为目标
        const nextId = newIds[newIndex + 1];
        const prevId = newIds[newIndex - 1];

        if (nextId) {
          targetId = nextId;
          moveMode = 'BEFORE';
        } else if (prevId) {
          targetId = prevId;
          moveMode = 'AFTER';
        }

        if (targetId) {
          emit('drag', {
            targetId,
            moveId: draggedId,
            moveMode,
            orgId: appStore.orgId,
            oldIndex,
            newIndex,
          });
        }
      },
    });
  }
  async function setDraggerSort() {
    if (props.notVirtualScroll) {
      const el = tableRef.value?.$el?.querySelector('.n-data-table tbody');
      if (el) {
        initSortable(el);
      }
    } else {
      const observer = new MutationObserver((mutations, obs) => {
        const el = tableRef.value?.$el?.querySelector('.n-data-table tbody');
        if (el) {
          obs.disconnect();
          initSortable(el);
        }
      });

      observer.observe(tableRef.value.$el, {
        childList: true,
        subtree: true,
      });
    }
  }

  function ensureDraggable() {
    if (!props.draggable) return;

    nextTick(() => {
      if (sortable.value) {
        sortable.value.destroy();
        sortable.value = null;
      }
      setDraggerSort();
    });
  }

  watch([() => attrs.data, () => props.draggable], () => {
    ensureDraggable();
  });

  onMounted(() => {
    ensureDraggable();
  });

  const scrollXWidth = computed(() =>
    currentColumns.value.reduce((prev, curr) => {
      const width = typeof curr.width === 'number' ? curr.width : 0;
      const minWidth = typeof curr.minWidth === 'number' ? curr.minWidth : calculateColumnMinWidth(curr);
      return prev + Math.max(width, typeof minWidth === 'number' ? minWidth : 0);
    }, 0)
  );

  function backToTop() {
    scrollTo({ top: 0 });
  }

  defineExpose({
    scrollTo,
    setColumnSort,
    clearCheckedRowKeys: handleClear,
    isFullScreen,
  });
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
      .n-data-table-th__title {
        .n-ellipsis {
          max-width: 300px;
        }
      }
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
  :deep(.n-data-table-td[data-col-key='order']) {
    padding-right: 0;
  }
  :deep(.n-data-table-td) {
    vertical-align: middle;
  }
  .crm-table-bottom-tip {
    position: sticky;
    bottom: 0;
    left: 0;
    z-index: 999;
    width: 100%;
    height: 30px;
    line-height: 30px;
    border-radius: 4px 4px 12px 12px;
    background: var(--text-n10);
    box-shadow: 0 -4px 10px rgb(100 103 103 / 5%);
  }
</style>
