import { CascaderProps, FormItemRule, InputNumberProps, InputProps, SelectProps, TreeSelectProps } from 'naive-ui';

import { OperatorEnum } from '@lib/shared/enums/commonEnum';
import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
import { SelectedUsersItem } from '@lib/shared/models/system/module';

import type { DataSourceProps } from '../../business/crm-data-source-select/type';
import type { UserTagSelectorProps } from '../../business/crm-user-tag-selector/type';
import { InternalRowData } from 'naive-ui/es/data-table/src/interface';

export interface ScopeProps {
  disabled: boolean;
}

export type AccordBelowType = 'AND' | 'OR';

export type CrmTreeSelectProps = {
  showContainChildModule?: boolean;
} & TreeSelectProps;

export interface FilterFormItem {
  id?: string | null | undefined;
  dataIndex?: string | null; // 第一列下拉的value
  title?: string; // 第一列下拉显示的label
  operator?: OperatorEnum; // 第二列的值
  operatorOption?: { value: string; label: string }[]; // operatorOptionsMap里设置的下拉数据不符合业务时，可以通过这个字段传入
  type: FieldTypeEnum; // 类型：判断第二列下拉数据和第三列显示形式
  showScope?: boolean;
  scope?: string[];
  value?: any; // 第三列的值
  selectedRows?: InternalRowData[];
  selectedUserList?: SelectedUsersItem[];
  inputProps?: Partial<InputProps>;
  numberProps?: Partial<InputNumberProps>;
  selectProps?: Partial<SelectProps>;
  treeSelectProps?: Partial<CrmTreeSelectProps>;
  userTagSelectorProps?: Partial<UserTagSelectorProps>;
  cascaderProps?: Partial<CascaderProps>;
  scopeProps?: Partial<ScopeProps>;
  dataSourceProps?: Partial<DataSourceProps>; // 数据源
  rule?: FormItemRule[];
  containChildIds?: string[]; // 用于树形选择器，存储包含子级新增的节点 id 集合
}

export type CombineItem = Pick<FilterFormItem, 'value' | 'operator'>;
export interface ConditionsItem extends CombineItem {
  name?: string;
  multipleValue: boolean;
  type?: FieldTypeEnum;
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
