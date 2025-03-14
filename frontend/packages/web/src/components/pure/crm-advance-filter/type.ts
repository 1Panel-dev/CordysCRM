import { CascaderProps, InputNumberProps, InputProps, SelectProps, TreeSelectProps } from 'naive-ui';

import { OperatorEnum } from '@lib/shared/enums/commonEnum';
import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';

import { UserTagSelectorProps } from '@/components/business/crm-user-tag-selector/index.vue';

export type AccordBelowType = 'AND' | 'OR';

export interface FilterFormItem {
  dataIndex?: string | null; // 第一列下拉的value
  title?: string; // 第一列下拉显示的label
  operator?: OperatorEnum; // 第二列的值
  type: FieldTypeEnum; // 类型：判断第二列下拉数据和第三列显示形式
  value?: any; // 第三列的值
  inputProps?: Partial<InputProps>;
  numberProps?: Partial<InputNumberProps>;
  selectProps?: Partial<SelectProps>;
  treeSelectProps?: Partial<TreeSelectProps>;
  userTagSelectorProps?: Partial<UserTagSelectorProps>;
  crmTimeRangePickerProps?: { typePath: string };
  cascaderProps?: Partial<CascaderProps>;
}

export type CombineItem = Pick<FilterFormItem, 'value' | 'operator'>;
export interface ConditionsItem extends CombineItem {
  name?: string;
  multipleValue: boolean;
}

export interface FilterResult {
  // 匹配模式支持所有 | 任一
  searchMode?: AccordBelowType;
  // 高级搜索条件
  conditions?: ConditionsItem[];
}

export interface FilterForm {
  searchMode?: AccordBelowType;
  conditions?: ConditionsItem[];
  list: FilterFormItem[];
}
