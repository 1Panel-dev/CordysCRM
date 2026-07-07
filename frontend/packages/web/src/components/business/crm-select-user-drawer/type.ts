import type { DeptNodeTypeEnum } from '@lib/shared/enums/systemEnum';

export interface Option {
  label: string;
  value: string;
  id?: string;
  nodeType?: DeptNodeTypeEnum;
  parentId?: string;
  disabled?: boolean;
  enable?: boolean;
  commander?: boolean;
  internal?: boolean;
  children?: Option[];
}
