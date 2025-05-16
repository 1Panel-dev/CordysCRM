import { CustomerFollowPlanStatusEnum } from '@lib/shared/enums/customerEnum';
import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import type { CommonList } from '@lib/shared/models/common';
import type { FollowDetailItem } from '@lib/shared/models/customer';

import {
  cancelClueFollowPlan,
  cancelCustomerFollowPlan,
  cancelOptFollowPlan,
  deleteClueFollowPlan,
  deleteClueFollowRecord,
  deleteCustomerFollowPlan,
  deleteCustomerFollowRecord,
  deleteOptFollowPlan,
  deleteOptFollowRecord,
  getClueFollowPlanList,
  getClueFollowRecordList,
  getCluePoolFollowRecordList,
  getCustomerFollowPlanList,
  getCustomerFollowRecordList,
  getCustomerOpenSeaFollowRecordList,
  getOptFollowPlanList,
  getOptFollowRecordList,
} from '@/api/modules';

export type StatusTagKey = Exclude<CustomerFollowPlanStatusEnum, CustomerFollowPlanStatusEnum.ALL>;
export type StatusTag = {
  label: string;
  value: CustomerFollowPlanStatusEnum;
  icon: string;
  color: string;
  iconColor?: string;
  bgColor?: string;
};
export const statusMap: Record<StatusTagKey, StatusTag> = {
  [CustomerFollowPlanStatusEnum.PREPARED]: {
    label: 'common.notStarted',
    value: CustomerFollowPlanStatusEnum.PREPARED,
    icon: 'iconicon_block_filled',
    color: 'var(--text-n1)',
    iconColor: 'var(--text-n4)',
    bgColor: 'var(--text-n6)',
  },
  [CustomerFollowPlanStatusEnum.UNDERWAY]: {
    label: 'common.inProgress',
    value: CustomerFollowPlanStatusEnum.UNDERWAY,
    icon: 'iconicon_testing',
    color: 'var(--info-blue)',
  },
  [CustomerFollowPlanStatusEnum.COMPLETED]: {
    label: 'common.completed',
    value: CustomerFollowPlanStatusEnum.COMPLETED,
    icon: 'iconicon_succeed_filled',
    color: 'var(--success-green)',
  },
  [CustomerFollowPlanStatusEnum.CANCELLED]: {
    label: 'common.canceled',
    value: CustomerFollowPlanStatusEnum.CANCELLED,
    icon: 'iconicon_block_filled',
    color: 'var(--text-n6)',
    bgColor: 'var(--text-n4)',
  },
};

export type RecordEnumType =
  | FormDesignKeyEnum.FOLLOW_RECORD_CUSTOMER
  | FormDesignKeyEnum.FOLLOW_RECORD_CLUE
  | FormDesignKeyEnum.CLUE_POOL
  | FormDesignKeyEnum.CUSTOMER_OPEN_SEA
  | FormDesignKeyEnum.FOLLOW_RECORD_BUSINESS;

export const followRecordApiMap: {
  list: Record<RecordEnumType, (params: any) => Promise<CommonList<FollowDetailItem>>>;
  delete: Partial<Record<RecordEnumType, (params: string) => Promise<any>>>;
} = {
  list: {
    [FormDesignKeyEnum.FOLLOW_RECORD_CUSTOMER]: getCustomerFollowRecordList,
    [FormDesignKeyEnum.FOLLOW_RECORD_CLUE]: getClueFollowRecordList,
    [FormDesignKeyEnum.CLUE_POOL]: getCluePoolFollowRecordList,
    [FormDesignKeyEnum.FOLLOW_RECORD_BUSINESS]: getOptFollowRecordList,
    [FormDesignKeyEnum.CUSTOMER_OPEN_SEA]: getCustomerOpenSeaFollowRecordList,
  },
  delete: {
    [FormDesignKeyEnum.FOLLOW_RECORD_CUSTOMER]: deleteCustomerFollowRecord,
    [FormDesignKeyEnum.FOLLOW_RECORD_CLUE]: deleteClueFollowRecord,
    [FormDesignKeyEnum.FOLLOW_RECORD_BUSINESS]: deleteOptFollowRecord,
  },
};

export type PlanEnumType =
  | FormDesignKeyEnum.FOLLOW_PLAN_CUSTOMER
  | FormDesignKeyEnum.FOLLOW_PLAN_CLUE
  | FormDesignKeyEnum.FOLLOW_PLAN_BUSINESS;
export const followPlanApiMap = {
  list: {
    [FormDesignKeyEnum.FOLLOW_PLAN_CUSTOMER]: getCustomerFollowPlanList,
    [FormDesignKeyEnum.FOLLOW_PLAN_CLUE]: getClueFollowPlanList,
    [FormDesignKeyEnum.FOLLOW_PLAN_BUSINESS]: getOptFollowPlanList,
  },
  delete: {
    [FormDesignKeyEnum.FOLLOW_PLAN_CUSTOMER]: deleteCustomerFollowPlan,
    [FormDesignKeyEnum.FOLLOW_PLAN_CLUE]: deleteClueFollowPlan,
    [FormDesignKeyEnum.FOLLOW_PLAN_BUSINESS]: deleteOptFollowPlan,
  },
  cancel: {
    [FormDesignKeyEnum.FOLLOW_PLAN_CUSTOMER]: cancelCustomerFollowPlan,
    [FormDesignKeyEnum.FOLLOW_PLAN_CLUE]: cancelClueFollowPlan,
    [FormDesignKeyEnum.FOLLOW_PLAN_BUSINESS]: cancelOptFollowPlan,
  },
};
