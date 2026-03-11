import type { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';

import type { ActionsItem } from '@/components/pure/crm-more-action/type';
import type { FormItemModel } from '@/components/business/crm-batch-form/types';

import type { Ref } from 'vue';

export type StatusBizType = FormDesignKeyEnum.BUSINESS | FormDesignKeyEnum.ORDER;

export interface StatusRowItem {
  id?: string;
  _key: string;
  name: string;
  type: 'AFOOT' | 'END' | string;
  editing?: boolean;
  draggable?: boolean;
  stageHasData?: boolean;
  [key: string]: any;
}

export interface StatusFormModel {
  runningStageRollback: boolean;
  completedStageRollback: boolean;
  list: StatusRowItem[];
}

export interface StatusSwitchConfigItem {
  key: 'runningStageRollback' | 'completedStageRollback';
  label: string;
  tip: string;
}

export interface StatusTextConfig {
  title: string;
  sectionTitle: string;
  columnTitles: string[];
  rollbackTitle: string;
  switches: StatusSwitchConfigItem[];
  stageHasDataTip: string;
}

export interface StatusApiConfig {
  load: () => Promise<any>;
  create: (params: any) => Promise<any>;
  update: (params: any) => Promise<any>;
  remove: (id: string) => Promise<any>;
  sort: (ids: string[]) => Promise<any>;
  rollback: (params: { afootRollBack: boolean; endRollBack: boolean }) => Promise<any>;
}

export interface StatusStrategyConfig {
  formItemModel: FormItemModel[];
  buildCreateParams: (row: StatusRowItem, ctx: { list: StatusRowItem[]; index: number }) => any;
  buildUpdateParams: (row: StatusRowItem) => any;
  normalizeItem?: (item: any) => Partial<StatusRowItem>;
}

export interface UseStatusConfigReturn {
  textConfig: Ref<StatusTextConfig>;
  formItemModel: Ref<FormItemModel[]>;
  form: Ref<StatusFormModel>;

  init: () => Promise<void>;
  handleSave: (element: any, done: () => void, index: number) => Promise<void>;
  handleCancelRow: (index: number) => void;
  handleSwitchChange: () => Promise<void>;
  handleMoreSelect: (
    action: ActionsItem,
    element: StatusRowItem,
    batchFormRef?: { formValidate?: (cb: () => void) => void } | null
  ) => Promise<void>;
  dragEnd: (event: any) => Promise<void>;
  handleMove: (evt: any) => boolean;
  getDropdownOptions: (element: StatusRowItem) => ActionsItem[];
}
