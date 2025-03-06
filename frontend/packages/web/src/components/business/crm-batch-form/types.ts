import { FormItemRule, InputNumberProps, InputProps, SelectProps } from 'naive-ui';

import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';

import { UserTagSelectorProps } from '@/components/business/crm-user-tag-selector/index.vue';

// 自定义检验器，为了传入动态渲染的表单项下标
export interface CustomValidator {
  notRepeat?: boolean;
}

export interface customSelectedProps {
  filterRepeat?: boolean;
}

export interface FormItemModel {
  path: string;
  label?: string;
  rule?: (FormItemRule & CustomValidator)[];
  type: FieldTypeEnum;
  formItemClass?: string;
  defaultValue?: string | string[] | number | number[] | boolean; // 默认值
  inputProps?: Partial<InputProps>;
  numberProps?: Partial<InputNumberProps>;
  selectProps?: Partial<SelectProps & customSelectedProps>;
  userTagSelectorProps?: Partial<UserTagSelectorProps>;
  crmTimeRangePickerProps?: { typePath: string };
}
