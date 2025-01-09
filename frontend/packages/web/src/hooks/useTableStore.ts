import { cloneDeep } from 'lodash-es';

import type { CrmDataTableColumn, TableStorageConfigItem } from '@/components/pure/crm-table/type';

import useAppStore from '@/store/modules/app';

import useLocalForage from './useLocalForage';
import { SpecialColumnEnum, TableKeyEnum } from '@lib/shared/enums/tableEnum';
import { isArraysEqualWithOrder } from '@lib/shared/method/equal';

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
    });
    return columns;
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
          column.forEach((col) => {
            const storedCol = tableColumnsMap.column.find((sc) => sc.key === col.key);
            if (storedCol) {
              col.width = storedCol.width; // 使用上一次拖拽存储的宽度，避免组件里边使用时候初始化到最初的列宽
            }
          });
          // 如果不相等，说明有变动将新的column存入indexDB
          setTableColumnsMap(tableKey, {
            column,
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
        (item) => item.key !== SpecialColumnEnum.OPERATION && item.type !== SpecialColumnEnum.SELECTION
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
        const selectColumn = tableColumnsMap.column.find((i) => i.type === SpecialColumnEnum.SELECTION);
        if (selectColumn) columns.unshift(selectColumn); // 加上选择框列
        if (operationColumn) columns.push(operationColumn); // 加上操作列
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

  return {
    initColumn,
    getCanSetColumns,
    setColumns,
    getShowInTableColumns,
    setPageSize,
    getPageSize,
  };
}
