import { UnwrapRef } from 'vue';
import { cloneDeep } from 'lodash-es';
import dayjs from 'dayjs';

import useTableStore from '@/hooks/useTableStore';
import useAppStore from '@/store/modules/app';

import type { CrmTableDataItem, CrmTableProps } from './type';
import { OperatorEnum } from '@lib/shared/enums/commonEnum';
import type { CommonList, FilterConditionItem, SortParams, TableQueryParams } from '@lib/shared/models/common';
import type { DataTableFilterState, PaginationProps } from 'naive-ui';

const tableStore = useTableStore();
const appStore = useAppStore();

export default function useTable<T>(
  loadListFunc?: (v?: TableQueryParams | any) => Promise<CommonList<CrmTableDataItem<T>> | CrmTableDataItem<T>>,
  props?: Partial<CrmTableProps<T>>,
  // 数据处理的回调函数
  dataTransform?: (item: CrmTableDataItem<T>) => CrmTableDataItem<T> | any
) {
  const defaultProps: CrmTableProps<T> = {
    bordered: false,
    loading: false, // 加载效果
    data: [], // 表格数据
    columns: [],
    tableRowKey: 'id', // 表格行的key
    showPagination: true, // 是否显示分页
    crmPagination: {
      page: 1,
      itemCount: 0,
      pageSize: appStore.pageSize,
      showSizePicker: appStore.showSizePicker,
      showQuickJumper: appStore.showQuickJumper,
    },
    ...props,
  };

  const propsRes = ref<CrmTableProps<T>>(cloneDeep(defaultProps));

  // 如果表格设置了tableKey，设置缓存的分页大小
  if (propsRes.value.showPagination && propsRes.value.tableKey) {
    tableStore.getPageSize(propsRes.value.tableKey).then((res) => {
      if (propsRes.value.crmPagination && res) {
        propsRes.value.crmPagination.pageSize = res;
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
    const { page, pageSize } = propsRes.value.crmPagination as PaginationProps;
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
    if (propsRes.value.crmPagination) {
      propsRes.value.crmPagination.page = page;
      if (total !== undefined) {
        propsRes.value.crmPagination.itemCount = total;
      }
    }
  }

  const tableQueryParams = ref<TableQueryParams>({}); // 表格请求参数集合
  const keyword = ref('');
  const sortItem = ref<SortParams>(); // 排序

  const filterItem = ref<FilterConditionItem[]>([]); // 筛选

  function processRecordItem(item: CrmTableDataItem<T>): CrmTableDataItem<T> {
    if (item.updateTime) {
      item.updateTime = dayjs(item.updateTime).format('YYYY-MM-DD HH:mm:ss');
    }
    if (item.createTime) {
      item.createTime = dayjs(item.createTime).format('YYYY-MM-DD HH:mm:ss');
    }
    if (dataTransform) {
      item = dataTransform(item);
    }
    return item;
  }

  async function loadList() {
    if (!loadListFunc) return;
    setLoading(true);
    try {
      tableQueryParams.value = {
        ...(!propsRes.value.showPagination ? {} : await getPaginationParams()),
        keyword: keyword.value,
        sort: sortItem.value,
        ...loadListParams.value,
        filters: filterItem.value,
      };
      const data = await loadListFunc(tableQueryParams.value);
      if (!propsRes.value.showPagination && Array.isArray(data)) {
        propsRes.value.data = data.map((item: CrmTableDataItem<T>) => {
          return processRecordItem(item);
        }) as unknown as UnwrapRef<CrmTableDataItem<T>[]>;
      } else {
        const tmpArr = data as CommonList<CrmTableDataItem<T>>;
        propsRes.value.data = tmpArr.list?.map((item: CrmTableDataItem<T>) => {
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
      // 如果表格设置了tableKey，缓存分页大小
      if (propsRes.value.tableKey) {
        await tableStore.setPageSize(propsRes.value.tableKey, pageSize);
      }
      loadList();
    },
    // 排序触发
    sorterChange: (sortObj: SortParams) => {
      sortItem.value = sortObj;
      loadList();
    },
    // 筛选触发
    filterChange: (filters: DataTableFilterState) => {
      filterItem.value = Object.entries(filters).map(([key, value]) => ({
        name: key,
        value,
        operator: OperatorEnum.IN,
        multipleValue: Array.isArray(value),
      }));
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
