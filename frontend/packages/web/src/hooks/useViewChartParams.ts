import { useRoute } from 'vue-router';

import { getSessionStorageTempState, setSessionStorageTempState } from '@lib/shared/method/local-storage';

import {
  AccordBelowType,
  ConditionsItem,
  FilterForm,
  FilterFormItem,
  FilterResult,
} from '@/components/pure/crm-advance-filter/type';
import { multipleValueTypeList } from '@/components/business/crm-form-create/config';

export interface ConditionParams {
  viewId: string;
  formModal: FilterForm; // 高级筛选条件
}

export interface ViewChartResult extends ConditionParams {
  filterResult: FilterResult; // 高级筛选查询参数
}

export const STORAGE_VIEW_CHART_KEY = 'viewChartParams';

export default function useViewChartParams() {
  const route = useRoute();
  // 获取整个存储的参数
  function getAllChartParams(): Record<string, ConditionParams> | null {
    return getSessionStorageTempState(STORAGE_VIEW_CHART_KEY, true);
  }

  // 存储图表参数对象
  /**
   * @param chartKey 图表url上携带的对应的cartKey,跳转之前生成的随机id  http://localhost:5173/#/opportunity/opt?cartKey=176104198889700000
   * @param data 图表参数
   */
  function setViewChartParams(chartKey: string, data: ConditionParams) {
    sessionStorage.removeItem(STORAGE_VIEW_CHART_KEY);
    let allChartParams = getAllChartParams();
    if (!allChartParams) {
      allChartParams = {};
    }

    allChartParams[chartKey] = data;
    const chartData = {
      [chartKey]: data,
    };
    setSessionStorageTempState(STORAGE_VIEW_CHART_KEY, chartData);
  }

  // 获取对应chartKey参数
  function getChartKeyParams(chartKey: string): ConditionParams | null {
    const allConditionParams = getAllChartParams();
    if (allConditionParams) {
      return allConditionParams[chartKey] || null;
    }
    return null;
  }

  // 获取图表参数用于table列表回显
  function getChartConditions(chartKey: string): ViewChartResult | null {
    const chartParams = getChartKeyParams(chartKey);
    if (chartParams) {
      const { viewId, formModal } = chartParams;

      const list: FilterFormItem[] = (formModal.list || []) as FilterFormItem[];

      const conditions: ConditionsItem[] = list.map((item: any) => ({
        value: item.value,
        operator: item.operator,
        name: item.dataIndex ?? '',
        multipleValue: multipleValueTypeList.includes(item.type),
        type: item.type,
      }));

      const searchMode = formModal.searchMode as AccordBelowType;

      return {
        viewId,
        formModal,
        filterResult: {
          searchMode,
          conditions,
        },
      };
    }
    return null;
  }

  const isInitViewChartQuery = ref(true);
  function initTableViewChartParams(callback?: (params: ViewChartResult) => void) {
    if (isInitViewChartQuery.value && route.query.chartKey) {
      const conditionParams = getChartConditions(route.query.chartKey as string);
      if (conditionParams) {
        callback?.(conditionParams);
      }
    }
    isInitViewChartQuery.value = false;
  }

  function getChartViewId() {
    if (route.query.chartKey && isInitViewChartQuery.value) {
      const conditionParams = getChartConditions(route.query.chartKey as string);
      return conditionParams?.viewId ?? '';
    }
  }

  return {
    setViewChartParams,
    getChartConditions,
    initTableViewChartParams,
    isInitViewChartQuery,
    getChartViewId,
  };
}
