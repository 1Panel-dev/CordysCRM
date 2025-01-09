import { UnwrapRef } from 'vue';
import { cloneDeep } from 'lodash-es';
import dayjs from 'dayjs';

import useTableStore from '@/hooks/useTableStore';
import useAppStore from '@/store/modules/app';

import type { CrmTableDataItem, CrmTableProps } from './type';
import type { CommonList, TableQueryParams } from '@lib/shared/models/common';
import type { DataTableFilterState, PaginationProps } from 'naive-ui';

const tableStore = useTableStore();
const appStore = useAppStore();

export default function useTable<T>(
  loadListFunc?: (v?: TableQueryParams | any) => Promise<CommonList<CrmTableDataItem<T>> | CrmTableDataItem<T>>,
  props?: Partial<CrmTableProps<T>>
) {
  const defaultProps: CrmTableProps<T> = {
    bordered: false,
    loading: false, // 加载效果
    data: [], // 表格数据
    columns: [],
    tableRowKey: 'id', // 表格行的key
    pagination: {
      page: 1,
      itemCount: 0,
      pageSize: appStore.pageSize,
      pageSizes: appStore.pageSizes,
      showSizePicker: appStore.showSizePicker,
      showQuickJumper: appStore.showQuickJumper,
    }, // false | PaginationProps; false表示不分页
    ...props,
  };

  const propsRes = ref<CrmTableProps<T>>(cloneDeep(defaultProps));

  // 如果表格设置了tableKey，设置缓存的分页大小
  if (propsRes.value.pagination && typeof propsRes.value.pagination === 'object' && propsRes.value.tableKey) {
    tableStore.getPageSize(propsRes.value.tableKey).then((res) => {
      if (propsRes.value.pagination && res) {
        propsRes.value.pagination.pageSize = res;
      }
    });
  }

  // 加载效果
  function setLoading(status: boolean) {
    propsRes.value.loading = status;
  }

  // 设置请求参数
  const loadListParams = ref<TableQueryParams>({});
  function setLoadListParams(params?: TableQueryParams) {
    loadListParams.value = params || {};
  }

  // 获取分页参数
  async function getPaginationParams() {
    const { page, pageSize } = propsRes.value.pagination as PaginationProps;
    if (propsRes.value.tableKey) {
      const cachedPageSize = await tableStore.getPageSize(propsRes.value.tableKey);
      return { current: page, pageSize: cachedPageSize || pageSize };
    }
    return { current: page, pageSize };
  }

  /**
   * 分页设置
   * @param page 当前页
   * @param total 总页数
   */
  function setPagination(page: number, total?: number) {
    if (propsRes.value.pagination && typeof propsRes.value.pagination === 'object') {
      propsRes.value.pagination.page = page;
      if (total !== undefined) {
        propsRes.value.pagination.itemCount = total;
      }
    }
  }

  const tableQueryParams = ref<TableQueryParams>({}); // 表格请求参数集合
  const keyword = ref('');
  const sortItem = ref<Record<string, any>>({}); // 排序

  const filterItem = ref<Record<string, any>>({}); // 筛选

  function processRecordItem(item: CrmTableDataItem<T>): CrmTableDataItem<T> {
    if (item.updateTime) {
      item.updateTime = dayjs(item.updateTime).format('YYYY-MM-DD HH:mm:ss');
    }
    if (item.createTime) {
      item.createTime = dayjs(item.createTime).format('YYYY-MM-DD HH:mm:ss');
    }
    return item;
  }

  async function loadList() {
    if (!loadListFunc) return;
    setLoading(true);
    try {
      tableQueryParams.value = {
        ...(!propsRes.value.pagination ? {} : await getPaginationParams()),
        keyword: keyword.value,
        sort: sortItem.value,
        ...loadListParams.value,
        filter: filterItem.value,
      };
      const data = await loadListFunc(tableQueryParams.value);
      if (!propsRes.value.pagination && Array.isArray(data)) {
        propsRes.value.data = data.map((item: CrmTableDataItem<T>) => {
          return processRecordItem(item);
        }) as unknown as UnwrapRef<CrmTableDataItem<T>[]>;
      } else {
        const tmpArr = data as CommonList<CrmTableDataItem<T>>;
        propsRes.value.data = tmpArr.list.map((item: CrmTableDataItem<T>) => {
          return processRecordItem(item);
        }) as unknown as UnwrapRef<CrmTableDataItem<T>[]>;
        // 设置分页
        setPagination(tmpArr.current, tmpArr.total);
      }
    } catch (error) {
      propsRes.value.data = [];
      // eslint-disable-next-line no-console
      console.error(error);
      throw error;
    } finally {
      setLoading(false);
    }
  }

  // 事件触发组
  const propsEvent = ref({
    // 分页触发
    pageChange: async (page: number) => {
      setPagination(page);
      await loadList();
    },
    // 修改每页显示条数触发
    pageSizeChange: async (pageSize: number) => {
      if (propsRes.value.pagination && typeof propsRes.value.pagination === 'object') {
        propsRes.value.pagination.pageSize = pageSize;
        // 如果表格设置了tableKey，缓存分页大小
        if (propsRes.value.tableKey) {
          await tableStore.setPageSize(propsRes.value.tableKey, pageSize);
        }
      }
      loadList();
    },
    // 排序触发
    sorterChange: (sortObj: { [key: string]: string }) => {
      sortItem.value = sortObj;
      loadList();
    },
    // 筛选触发
    filterChange: (filters: DataTableFilterState) => {
      filterItem.value = { ...filters };
      loadList();
    },
  });

  return {
    propsRes,
    propsEvent,
    setLoading,
    setLoadListParams,
    loadList,
    setPagination,
  };
}
