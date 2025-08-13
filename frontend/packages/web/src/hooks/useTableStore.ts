import { cloneDeep } from 'lodash-es';

import { SpecialColumnEnum, TableKeyEnum } from '@lib/shared/enums/tableEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';
import { isArraysEqualWithOrder } from '@lib/shared/method/equal';

import type { CrmDataTableColumn, TableStorageConfigItem } from '@/components/pure/crm-table/type';

import useAppStore from '@/store/modules/app';

import useLocalForage from './useLocalForage';

const { t } = useI18n();

export default function useTableStore() {
  const { getItem, setItem } = useLocalForage();
  const appStore = useAppStore();

  async function getTableColumnsMap(tableKey: TableKeyEnum): Promise<TableStorageConfigItem | null> {
    // TODO 存储隔离
    const tableColumnsMap = await getItem<TableStorageConfigItem>(tableKey);
    return tableColumnsMap;
  }

  async function setTableColumnsMap(tableKey: TableKeyEnum, tableColumnsMap: TableStorageConfigItem) {
    // TODO 存储隔离
    await setItem(tableKey, tableColumnsMap);
  }

  function columnsTransform(columns: CrmDataTableColumn[]) {
    columns.forEach((item) => {
      if (item.showInTable === undefined) {
        // 默认在表格中展示
        item.showInTable = true;
      }
      if (item.key === SpecialColumnEnum.OPERATION) {
        item.title = t('common.operation');
      }
    });
    return columns;
  }

  function sortByOldOrder(oldArr: CrmDataTableColumn[], newArr: CrmDataTableColumn[]): CrmDataTableColumn[] {
    const mapNew = new Map(newArr.map((item) => [item.key, item]));
    // 先按 old 顺序排列 new 中存在的项
    const sorted = oldArr.map((item) => mapNew.get(item.key)).filter(Boolean) as CrmDataTableColumn[];
    // 再把 new 中 old 没有的项追加在最后
    const extra = newArr.filter((item) => !oldArr.some((o) => o.key === item.key));
    return [...sorted, ...extra];
  }

  async function initColumn(tableKey: TableKeyEnum, column: CrmDataTableColumn[]) {
    try {
      const tableColumnsMap = await getTableColumnsMap(tableKey);
      if (!tableColumnsMap) {
        // 如果没有在indexDB里初始化
        column = columnsTransform(column);
        setTableColumnsMap(tableKey, {
          column,
          columnBackup: cloneDeep(column),
        });
      } else {
        // 初始化过了，但是可能有新变动，如列的顺序，列的显示隐藏，列的拖拽
        column = columnsTransform(column);
        const { columnBackup: oldColumn } = tableColumnsMap;
        // 比较页面上定义的 column 和 浏览器备份的column 是否相同
        const isEqual = isArraysEqualWithOrder(oldColumn, column);
        if (!isEqual) {
          // 如果不相等，说明有变动将新的column存入indexDB
          const newColumns = sortByOldOrder(tableColumnsMap.column, column).map((e) => {
            const sameItem = tableColumnsMap.column.find((item) => item.key === e.key);
            if (sameItem) {
              // 如果是相同的列，则更新除了宽度、显隐、固定以外的属性
              return {
                ...e,
                width: sameItem.width,
                showInTable: sameItem.showInTable,
                fixed: sameItem.fixed,
              };
            }
            return e;
          });
          setTableColumnsMap(tableKey, {
            ...tableColumnsMap,
            column: newColumns,
            columnBackup: cloneDeep(column),
          });
        }
      }
    } catch (e) {
      // eslint-disable-next-line no-console
      console.log(e);
    }
  }

  // 表头显示设置的列
  async function getCanSetColumns(tableKey: TableKeyEnum) {
    const tableColumnsMap = await getTableColumnsMap(tableKey);
    if (tableColumnsMap) {
      return tableColumnsMap.column.filter(
        (item) => item.type !== SpecialColumnEnum.SELECTION && item.key !== SpecialColumnEnum.ORDER
      );
    }
    return [];
  }

  // 在表格上展示的列
  async function getShowInTableColumns(tableKey: TableKeyEnum) {
    const tableColumnsMap = await getTableColumnsMap(tableKey);
    if (tableColumnsMap) {
      return tableColumnsMap.column.filter((i) => i.showInTable);
    }
    return [];
  }

  async function setColumns(tableKey: TableKeyEnum, columns: CrmDataTableColumn[]) {
    try {
      const tableColumnsMap = await getTableColumnsMap(tableKey);
      if (tableColumnsMap) {
        const operationColumn = tableColumnsMap.column.find((i) => i.key === SpecialColumnEnum.OPERATION);
        const newOperationColumn = columns.find((i) => i.key === SpecialColumnEnum.OPERATION);
        const selectColumn = tableColumnsMap.column.find((i) => i.type === SpecialColumnEnum.SELECTION);
        const orderColumn = tableColumnsMap.column.find((i) => i.key === SpecialColumnEnum.ORDER);
        columns = columns.filter((col) => col.key !== SpecialColumnEnum.OPERATION);
        if (selectColumn) columns.unshift(selectColumn);
        if (orderColumn) columns.unshift(orderColumn);
        if (operationColumn) {
          columns.push({
            ...operationColumn,
            fixed: newOperationColumn?.fixed,
          });
        }

        tableColumnsMap.column = cloneDeep(columns);
        await setTableColumnsMap(tableKey, tableColumnsMap);
      }
    } catch (e) {
      // eslint-disable-next-line no-console
      console.error('tableStore.setColumns', e);
    }
  }

  async function getPageSize(tableKey: TableKeyEnum) {
    const tableColumnsMap = await getTableColumnsMap(tableKey);
    return tableColumnsMap ? tableColumnsMap.pageSize : appStore.pageSize;
  }

  async function setPageSize(tableKey: TableKeyEnum, pageSize: number): Promise<void> {
    try {
      const tableColumnsMap = await getTableColumnsMap(tableKey);
      if (tableColumnsMap) {
        tableColumnsMap.pageSize = pageSize;
        await setTableColumnsMap(tableKey, tableColumnsMap);
      }
    } catch (e) {
      // eslint-disable-next-line no-console
      console.log(e);
    }
  }

  async function setTableLineHeight(tableKey: TableKeyEnum, layout: string): Promise<void> {
    try {
      const tableColumnsMap = await getTableColumnsMap(tableKey);
      if (tableColumnsMap) {
        tableColumnsMap.layout = layout;
        await setTableColumnsMap(tableKey, tableColumnsMap);
      }
    } catch (e) {
      // eslint-disable-next-line no-console
      console.log(e);
    }
  }

  async function getTableLineHeight(tableKey: TableKeyEnum) {
    const tableColumnsMap = await getTableColumnsMap(tableKey);
    return tableColumnsMap && tableColumnsMap.layout ? tableColumnsMap.layout : 'compact';
  }

  return {
    initColumn,
    getCanSetColumns,
    setColumns,
    getShowInTableColumns,
    setPageSize,
    getPageSize,
    setTableLineHeight,
    getTableLineHeight,
  };
}
