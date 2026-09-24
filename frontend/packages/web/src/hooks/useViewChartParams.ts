import { useRoute } from 'vue-router';

import { OperatorEnum } from '@lib/shared/enums/commonEnum';
import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
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
  formModel: FilterForm; // 高级筛选条件
}

export interface ViewChartResult extends ConditionParams {
  filterResult: FilterResult; // 高级筛选查询参数
}

export const STORAGE_VIEW_CHART_KEY = 'viewChartParams';

// 按数字类型提交的字段: 统计字段的值是聚合出的数字, 取值与数字字段保持一致
const NUMBER_VALUE_FIELD_TYPES = [FieldTypeEnum.INPUT_NUMBER, FieldTypeEnum.STATISTIC];

export default function useViewChartParams() {
  const route = useRoute();
  // 获取整个存储的参数
  function getAllChartParams(): Record<string, ConditionParams> | null {
    return getSessionStorageTempState(STORAGE_VIEW_CHART_KEY, true);
  }

  // 存储图表参数对象
  /**
   * @param chartKey 图表url上携带的对应的cartKey,跳转之前生成的随机id  http://localhost:5173/#/opportunity/opt?chartKey=176104198889700000
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
      const { viewId, formModel } = chartParams;

      const list: FilterFormItem[] = (formModel.list || []) as FilterFormItem[];

      // 数字类字段(数值、统计)回填时统一转成数字, 与筛选弹窗中数字输入框提交的类型保持一致
      const filterFormList = list.map((e) => {
        if (NUMBER_VALUE_FIELD_TYPES.includes(e.type)) {
          return { ...e, value: e.operator === OperatorEnum.EMPTY ? '' : Number(e.value) };
        }
        return e;
      });

      const conditions: ConditionsItem[] = filterFormList.map((item: any) => ({
        value: item.value,
        operator: item.operator,
        name: item.dataIndex ?? '',
        multipleValue: multipleValueTypeList.includes(item.type),
        type: item.type,
      }));

      const searchMode = formModel.searchMode as AccordBelowType;

      return {
        viewId,
        formModel: {
          searchMode,
          list: filterFormList,
        },
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
