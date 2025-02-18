import type { FieldTypeEnum } from './enum';
import type { FormItemRule } from 'naive-ui';
import type { Option } from 'naive-ui/es/transfer/src/interface';

export interface FormCreateFieldOption extends Option {
  [key: string]: any;
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
  options?: FormCreateFieldOption[];
  defaultValue?: any;
  rules: FormItemRule | FormItemRule[];
  // 数字输入属性
  max?: number;
  min?: number;
  numberFormat?: 'number' | 'percent'; // 数字格式, number: 数字, percent: 百分比
  precision?: number; // 精度
  showThousandsSeparator?: boolean; // 是否显示千分位
  // 日期输入属性
  datetype?: 'month' | 'date' | 'datetime';
  // radio属性
  direction?: 'horizontal' | 'vertical';
  // divider属性
  dividerClass?: string;
  // 图片上传属性
  uploadLimit?: number;
  uploadSizeLimit?: number;
  // 地址属性
  hasDetail?: boolean;
  [key: string]: any;
}
