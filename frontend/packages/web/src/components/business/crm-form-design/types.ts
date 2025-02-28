import type { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';

export interface FieldItem {
  type: FieldTypeEnum;
  icon: string;
  name: string;
}
