import { FilterResult } from '@/components/pure/crm-advance-filter/type';

export interface StageBoardStage {
  id: string;
  name: string;
  type?: string;
  rate?: string | number;
  [key: string]: any;
}

export interface StageBoardLoadParams {
  current: number;
  pageSize: number;
  stageId: string;
  keyword?: string;
  viewId?: string;
  advanceFilter?: FilterResult;
}

export interface StageBoardLoadResult<T = any> {
  list: T[];
  total: number;
  optionMap?: Record<string, any>;
}

export interface StageBoardStatistic {
  amount?: number;
  averageAmount?: number;
  [key: string]: any;
}

export interface StageBoardMovePayload<T = any> {
  item: T;
  fromStageId?: string;
  toStageId: string;
  previousItem?: T;
  nextItem?: T;
  rawItem?: any;
}

export interface StageBoardMoveCheckPayload<T = any> {
  fromStageId?: string;
  toStageId: string;
  item?: T;
  rawEvent: any;
}

export interface StageBoardBlockedMovePayload<T = any> {
  item: T;
  fromStageId?: string;
  toStageId: string;
  rawItem: any;
}

export interface StageBoardListExpose {
  refreshList: () => void;
  sortItem: (item: any) => Promise<void>;
}

export type OpenDetailType = 'customer' | 'opportunity' | 'order' | 'contract';
