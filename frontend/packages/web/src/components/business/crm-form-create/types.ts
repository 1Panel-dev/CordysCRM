import type { FieldTypeEnum } from './enum';
import type { FormItemRule } from 'naive-ui';
import type { Option } from 'naive-ui/es/transfer/src/interface';

export interface FormCreateFieldOption extends Option {
  [key: string]: any;
}

export interface FormCreateField {
  id: string;
  name: string;
  type: FieldTypeEnum;
  key?: string;
  showLabel: boolean;
  placeholder?: string;
  tooltip: string;
  readable: boolean;
  editable: boolean;
  fieldWidth: number;
  options?: FormCreateFieldOption;
  defaultValue?: string | number | boolean | (string | number | boolean)[];
  rules: FormItemRule | FormItemRule[];
  [key: string]: any;
}
