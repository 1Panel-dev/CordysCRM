import type { FilterForm, FilterResult } from '@cordys/web/src/components/pure/crm-advance-filter/type';

export interface ViewParams extends FilterResult {
  id?: string;
  name: string;
}

export interface ViewItem extends FilterForm {
  id: string;
  name: string;
  fixed: boolean;
  enable: boolean;
  type?: string;
  optionMap?: Record<string, any>;
}
