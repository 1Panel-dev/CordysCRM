import type { ModuleField, SaveCustomerParams } from '@lib/shared/models/customer';

export interface SaveClueParams extends SaveCustomerParams {
  contact: string;
  phone: string;
}

export interface UpdateClueParams extends SaveClueParams {
  contact: string;
  phone: string;
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
}

export interface ClueListItem extends ClueDetail {
  inSharedPool: boolean;
  latestFollowUpTime: number;
  collectionTime: number;
  reservedDays: number;
}
