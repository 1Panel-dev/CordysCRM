import { useRoute } from 'vue-router';

import { FieldDataSourceTypeEnum, FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
import { getSessionStorageTempState, setSessionStorageTempState } from '@lib/shared/method/local-storage';

import { operatorOptionsMap } from '@/components/pure/crm-advance-filter/index';
import {
  AccordBelowType,
  ConditionsItem,
  FilterForm,
  FilterFormItem,
  FilterResult,
} from '@/components/pure/crm-advance-filter/type';
import { getDefaultOperator, valueIsArray } from '@/components/pure/crm-advance-filter/utils';
import { multipleValueTypeList } from '@/components/business/crm-form-create/config';

export interface ConditionParams {
  viewId: string;
  formModal: FilterForm; // 高级筛选的条件
}

export interface ViewChartResult extends ConditionParams {
  filterResult: FilterResult;
}

export interface CategoryCondition {
  dataIndex: string;
  value: string;
  type: FieldTypeEnum;
  selectedList?: { id: string; name: string }[]; // 数据源类型和标签选择器:tagSelectType里边所包含类型需传递
  dataSourceType?: FieldDataSourceTypeEnum; // 数据源类型需传递
}

interface ViewChartParam {
  conditionParams: ConditionParams; // 当前列表查询的所有条件入参
  categoryCondition: CategoryCondition[];
}

export const STORAGE_VIEW_CHART_KEY = 'viewChartParams';

const tagSelectType = [
  FieldTypeEnum.DEPARTMENT,
  FieldTypeEnum.DEPARTMENT_MULTIPLE,
  FieldTypeEnum.MEMBER,
  FieldTypeEnum.MEMBER_MULTIPLE,
];

export default function useViewChartParams() {
  const route = useRoute();
  // 获取整个存储的参数
  function getAllChartParams(): Record<string, ViewChartParam> | null {
    return getSessionStorageTempState(STORAGE_VIEW_CHART_KEY, true);
  }

  // 存储图表参数对象
  /**
   * @param chartKey 图表url上携带的对应的cartKey,跳转之前生成的随机id  http://localhost:5173/#/opportunity/opt?cartKey=176104198889700000
   * @param data 图表参数
   */
  function setViewChartParams(chartKey: string, data: ViewChartParam) {
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
  function getChartKeyParams(chartKey: string): ViewChartParam | null {
    const allConditionParams = getAllChartParams();
    if (allConditionParams) {
      return allConditionParams[chartKey] || null;
    }
    return null;
  }

  function getOperator(type: FieldTypeEnum) {
    const options = operatorOptionsMap[type] || [];
    const optionsValueList = options.map((optionItem: { value: string; label: string }) => optionItem.value);
    return getDefaultOperator(optionsValueList);
  }

  // 获取图表参数用于table列表回显
  function getChartConditions(chartKey: string): ViewChartResult | null {
    const chartParams = getChartKeyParams(chartKey);
    if (chartParams) {
      const { conditionParams, categoryCondition } = chartParams;

      const filterFormModalList: FilterFormItem[] = (conditionParams?.formModal.list || []) as FilterFormItem[];

      const categoryMap = new Map(categoryCondition.map((item: CategoryCondition) => [item.dataIndex, item]));

      const updatedConditions: FilterFormItem[] = filterFormModalList.map((item) => {
        const filterNameKey = item.dataIndex as string;
        // 处理筛选条件中是否存在分组依据条件，已存在则替换成跳转元素的value
        if (categoryMap.has(filterNameKey)) {
          const categoryItem = categoryMap.get(filterNameKey)!;
          categoryMap.delete(filterNameKey);
          const isArray = valueIsArray(item);
          return {
            ...item,
            value: isArray ? [categoryItem.value] : categoryItem.value,
          };
        }
        return item;
      });

      // 不存在则将分组依据的条件添加到筛选
      categoryMap.forEach((categoryItem: CategoryCondition) => {
        const filterType = categoryItem.type as FieldTypeEnum;

        const isArray = valueIsArray({
          ...categoryItem,
          type: filterType,
          operator: getOperator(filterType),
        });

        updatedConditions.push({
          ...categoryItem,
          operator: getOperator(filterType),
          type: filterType,
          value: isArray ? [categoryItem.value] : categoryItem.value,
          ...(categoryItem?.dataSourceType
            ? {
                selectedRows: categoryItem.selectedList,
                dataSourceProps: {
                  dataSourceType: categoryItem.dataSourceType,
                },
              }
            : {}),
          ...(tagSelectType.includes(filterType)
            ? {
                selectedUserList: categoryItem?.selectedList || [],
              }
            : {}),
        });
      });

      const conditions: ConditionsItem[] = updatedConditions.map((item: any) => ({
        value: item.value,
        operator: item.operator,
        name: item.dataIndex ?? '',
        multipleValue: multipleValueTypeList.includes(item.type),
        type: item.type,
      }));

      const searchMode = conditionParams.formModal.searchMode as AccordBelowType;

      return {
        viewId: conditionParams.viewId,
        formModal: {
          searchMode,
          list: updatedConditions,
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
