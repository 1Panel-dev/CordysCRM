import type { FieldTypeEnum } from '../crm-form-create/enum';

export interface FieldItem {
  type: FieldTypeEnum;
  icon: string;
  name: string;
}

export type FormFooterDirection = 'flex-row' | 'flex-row-reverse' | 'justify-center';
export interface FormActionButton {
  text: string;
  enable: boolean;
}
export interface FormConfig {
  layout: number;
  labelPos: 'vertical' | 'horizontal';
  inputWidth: 'custom' | 'full';
  optBtnContent: FormActionButton[];
  optBtnPos: FormFooterDirection;
}
