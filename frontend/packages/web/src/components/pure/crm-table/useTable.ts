import { UnwrapRef } from 'vue';
import { cloneDeep } from 'lodash-es';
import dayjs from 'dayjs';

import type { CommonList, FilterConditionItem, SortParams, TableQueryParams } from '@lib/shared/models/common';

import { FilterResult } from '@/components/pure/crm-advance-filter/type';

import useTableStore from '@/hooks/useTableStore';
import useAppStore from '@/store/modules/app';

import type { CrmTableDataItem, CrmTableProps } from './type';
import type { PaginationProps } from 'naive-ui';

const tableStore = useTableStore();
const appStore = useAppStore();

export default function useTable<T>(
  loadListFunc?: (v?: TableQueryParams | any) => Promise<CommonList<CrmTableDataItem<T>> | CrmTableDataItem<T>>,
  props?: Partial<CrmTableProps<T>>,
  // 数据处理的回调函数
  dataTransform?: (
    item: CrmTableDataItem<T>,
    originalData?: CommonList<CrmTableDataItem<T>>
  ) => CrmTableDataItem<T> | any
) {
  const defaultProps: CrmTableProps<T> = {
    bordered: false,
    loading: false, // 加载效果
    data: [], // 表格数据
    columns: [],
    tableRowKey: 'id', // 表格行的key
    showPagination: true, // 是否显示分页
    ...props,
    crmPagination: {
      page: 1,
      itemCount: 0,
      pageSize: appStore.pageSize,
      showSizePicker: appStore.showSizePicker,
      showQuickJumper: appStore.showQuickJumper,
      ...props?.crmPagination,
    },
  };

  const propsRes = ref<CrmTableProps<T>>(cloneDeep(defaultProps));

  const filterItem = ref<FilterConditionItem[]>([]); // 筛选

  const keyword = ref('');

  const code = ref(0); // 状态码

  // 高级筛选
  const advanceFilter = reactive<FilterResult>({ searchMode: 'AND', conditions: [] });

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
    if (propsRes.value.crmPagination) {
      propsRes.value.crmPagination.page = 1;
    }
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

  // 重置表头筛选
  function resetFilterParams() {
    filterItem.value = [];
  }

  // 设置 advanceFilter
  function setAdvanceFilter(v: FilterResult) {
    advanceFilter.searchMode = v.searchMode;
    advanceFilter.conditions = v.conditions;

    // 基础筛选都清空
    loadListParams.value.filter = {};
    loadListParams.value.keyword = '';
    keyword.value = '';
    resetFilterParams();
    if (propsRes.value.crmPagination) {
      propsRes.value.crmPagination.page = 1;
    }
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

  const sortItem = ref<SortParams>(); // 排序

  function processRecordItem(
    item: CrmTableDataItem<T>,
    originalData?: CommonList<CrmTableDataItem<T>>
  ): CrmTableDataItem<T> {
    if (item.updateTime) {
      item.updateTime = dayjs(item.updateTime).format('YYYY-MM-DD HH:mm:ss');
    }
    if (item.createTime) {
      item.createTime = dayjs(item.createTime).format('YYYY-MM-DD HH:mm:ss');
    }
    if (dataTransform) {
      item = dataTransform(item, originalData);
    }
    return item;
  }

  async function loadList(isPageChange = false) {
    if (!loadListFunc || propsRes.value.loading) return;
    setLoading(true);
    try {
      tableQueryParams.value = {
        ...(!propsRes.value.showPagination ? {} : await getPaginationParams()),
        sort: sortItem.value,
        combineSearch: advanceFilter,
        ...loadListParams.value,
        filters: filterItem.value,
      };
      const res = await loadListFunc(tableQueryParams.value);
      if (props?.isReturnNativeResponse) {
        code.value = res.data.code;
      }
      const data = props?.isReturnNativeResponse ? res.data.data : res;
      if (!propsRes.value.showPagination && Array.isArray(data)) {
        if (isPageChange) {
          data.forEach((item: CrmTableDataItem<T>) => {
            propsRes.value.data.push(processRecordItem(item));
          }) as unknown as UnwrapRef<CrmTableDataItem<T>[]>;
        } else {
          propsRes.value.data = data.map((item: CrmTableDataItem<T>) =>
            processRecordItem(item)
          ) as unknown as UnwrapRef<CrmTableDataItem<T>[]>;
        }
      } else {
        const tmpArr = data as CommonList<CrmTableDataItem<T>>;
        if (isPageChange) {
          tmpArr.list?.forEach((item: CrmTableDataItem<T>) => {
            propsRes.value.data.push(processRecordItem(item, tmpArr));
          }) as unknown as UnwrapRef<CrmTableDataItem<T>[]>;
        } else {
          propsRes.value.data = tmpArr.list?.map((item: CrmTableDataItem<T>) =>
            processRecordItem(item, tmpArr)
          ) as unknown as UnwrapRef<CrmTableDataItem<T>[]>;
        }
        // 设置分页
        setPagination(tmpArr.current, tmpArr.total);
      }
    } catch (error: any) {
      propsRes.value.data = [];
      // eslint-disable-next-line no-console
      console.error(error);
      if (props?.isReturnNativeResponse) {
        code.value = error.code;
      }
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
      await loadList(true);
    },
    // 修改每页显示条数触发
    pageSizeChange: async (pageSize: number) => {
      if (propsRes.value.crmPagination && typeof propsRes.value.crmPagination === 'object') {
        propsRes.value.crmPagination.pageSize = pageSize;
        // 如果表格设置了tableKey，缓存分页大小
        if (propsRes.value.tableKey) {
          await tableStore.setPageSize(propsRes.value.tableKey, pageSize);
        }
      }
      loadList();
    },
    // 排序触发
    sorterChange: (sortObj: SortParams) => {
      sortItem.value = sortObj;
      setPagination(1);
      loadList();
    },
    // 筛选触发
    filterChange: (filters: FilterConditionItem[]) => {
      filterItem.value = filters.map((item) => ({
        ...item,
        multipleValue: item.multipleValue ?? filterItem.value.find((i) => i.name === item.name)?.multipleValue ?? false,
      }));
      setPagination(1);
      loadList();
    },
  });

  return {
    propsRes,
    propsEvent,
    tableQueryParams,
    setLoading,
    setLoadListParams,
    loadList,
    setPagination,
    advanceFilter,
    setAdvanceFilter,
    code,
  };
}
