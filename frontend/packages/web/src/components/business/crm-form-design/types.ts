import type { FieldTypeEnum } from '../crm-form-create/enum';

export interface FieldItem {
  type: FieldTypeEnum;
  icon: string;
  name: string;
}

export type FormFooterDirection = 'flex-row' | 'flex-row-reverse' | 'justify-center';
export interface FormConfig {
  formCols: number;
  okText: string;
  continueText: string;
  cancelText: string;
  footerDirectionClass: FormFooterDirection;
}
