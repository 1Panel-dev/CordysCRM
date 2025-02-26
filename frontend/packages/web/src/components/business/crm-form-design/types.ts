import type { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';

import type { LabelPlacement } from 'naive-ui/es/form/src/interface';

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
  labelPos: LabelPlacement;
  inputWidth: 'custom' | 'full';
  optBtnContent: FormActionButton[];
  optBtnPos: FormFooterDirection;
}
