import type { FieldDataSourceTypeEnum, FieldRuleEnum, FieldTypeEnum } from './enum';
import type { FormItemRule } from 'naive-ui';
import type { InternalRowData } from 'naive-ui/es/data-table/src/interface';
import type { Option } from 'naive-ui/es/transfer/src/interface';

export interface FormCreateFieldOption extends Option {
  [key: string]: any;
}

export interface FormCreateFieldRule extends FormItemRule {
  key: FieldRuleEnum;
  label: string;
}

export interface FormCreateField {
  // 基础属性
  id: string;
  name: string;
  type: FieldTypeEnum;
  key?: string;
  showLabel: boolean;
  placeholder?: string;
  description: string;
  readable: boolean;
  editable: boolean;
  fieldWidth: number;
  defaultValue?: any;
  rules: FormCreateFieldRule[];
  // 数字输入属性
  max?: number;
  min?: number;
  numberFormat?: 'number' | 'percent'; // 数字格式, number: 数字, percent: 百分比
  decimalPlaces?: boolean; // 保留小数点位
  precision?: number; // 精度
  showThousandsSeparator?: boolean; // 是否显示千分位
  // 日期输入属性
  datetype?: 'month' | 'date' | 'datetime';
  // radio属性
  direction?: 'horizontal' | 'vertical';
  // divider属性
  dividerClass?: string;
  dividerColor?: string;
  titleColor?: string;
  // 图片上传属性
  pictureShowType?: 'card' | 'list';
  uploadLimit?: number;
  uploadLimitEnable?: boolean;
  uploadSizeLimit?: number;
  uploadSizeLimitEnable?: boolean;
  // 地址属性
  locationType?: 'PCD' | 'detail'; // PCD: 省市区, detail: 省市区+详细地址
  // 选择器属性
  multiple?: boolean;
  options?: FormCreateFieldOption[];
  // dataSource属性
  dataSourceType?: FieldDataSourceTypeEnum;
  // 成员属性
  hasCurrentUser?: boolean;
  // 部门属性
  hasCurrentUserDept?: boolean;
  // 前端渲染属性
  icon: string;
  showRules?: FieldRuleEnum[]; // 显示的校验规则
  dataSourceSelectedRows?: InternalRowData[]; // 数据源选中的行，回显用
}
