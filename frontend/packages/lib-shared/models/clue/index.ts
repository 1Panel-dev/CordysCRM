import type { CustomerSearchTypeEnum } from '../../enums/customerEnum';
import type { TableQueryParams } from '../common';
import type { ModuleField, SaveCustomerParams } from '@lib/shared/models/customer';

export interface SaveClueParams extends SaveCustomerParams {
  contact?: string;
  phone?: string;
}

export interface UpdateClueParams extends SaveClueParams {
  id: string;
}

export interface ClueTransitionCustomerParams extends SaveCustomerParams {
  clueId: string;
}

export interface ClueDetail {
  id: string;
  name: string;
  owner: string;
  ownerName: string;
  contact: string;
  phone: string;
  departmentId: string;
  departmentName: string;
  stage: string;
  lastStage: string;
  createUser: string;
  updateUser: string;
  createTime: number;
  updateTime: number;
  createUserName: string;
  updateUserName: string;
  moduleFields: ModuleField[];
  transitionType?: 'CUSTOMER' | 'OPPORTUNITY';
}

export interface ClueListItem extends ClueDetail {
  inSharedPool: boolean;
  latestFollowUpTime: number;
  collectionTime: number;
  reservedDays: number;
}

export interface CluePoolTableParams extends TableQueryParams {
  searchType?: CustomerSearchTypeEnum;
  poolId?: string;
}

// 线索池线索列表项
export interface CluePoolListItem extends ClueListItem {
  follower: string; // 最新跟进人
  followerName: string; // 最新跟进人名称
  followTime: number; // 最新跟进日期
  poolId: string;
  recyclePoolName: string; // 默认回收公海名称
}

export interface PickClueParams {
  clueId: string;
  poolId: string;
}

// 批量领取线索的请求参数
export interface BatchPickClueParams {
  batchIds: (string | number)[];
  poolId: string;
}

// 分配线索的请求参数
export interface AssignClueParams {
  clueId: string;
  assignUserId: string;
}

// 批量分配线索的请求参数
export interface BatchAssignClueParams {
  batchIds: (string | number)[];
  assignUserId: string;
}
