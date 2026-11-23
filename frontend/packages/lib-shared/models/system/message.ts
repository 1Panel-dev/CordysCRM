import type { TableQueryParams } from '../common';
import { SelectedUsersItem } from '@lib/shared/models/system/module';

export interface AnnouncementSaveParams {
  id?: string;
  subject: string; // 公告标题
  content: string;
  startTime: number;
  endTime: number;
  url: string; // 链接
  organizationId: string;
  deptIds: string[];
  userIds: string[];
  renameUrl: string;
  range: [number, number] | undefined;
  ownerIds: SelectedUsersItem[];
}

export interface AnnouncementTableQueryParams extends TableQueryParams {
  organizationId: string;
}

export interface AnnouncementItemDetail {
  id: string;
  createUser: string;
  updateUser: string;
  createTime: number;
  updateTime: number;
  subject: string; // 公告标题
  content: string; // 公告内容
  startTime: number; // 公告开始时间
  endTime: number; // 公告结束时间
  url: string; // 公告链接
  receiver: string; // 接收人
  organizationId: string; // 组织ID
  notice: boolean; // 是否为通知公告
  receiveType: string; // 接收类型
  contentText: string; // 公告文本内容
  createUserName: string;
  updateUserName: string;
  renameUrl: string;
  deptIdName: SelectedUsersItem[]; // 部门
  userIdName: SelectedUsersItem[]; // 用户
}

export interface MessageTaskCountDTO {
  taskType: string;
  count: number;
  name?: string;
}

export interface MessageTaskPageParams {
  current: number;
  pageSize: number;
  taskType: string;
  sort: Record<string, unknown>;
  combineSearch: {
    searchMode: string;
    conditions: unknown[];
  };
  keyword: string;
  viewId: string;
  filters: unknown[];
}

export interface MessageTaskScopeMember {
  id: string;
  scope: string;
  name: string;
}

export interface MessageTaskTriggerConfig {
  actionTypeConfig?: string;
  scheduleTypeConfig?: {
    fieldId?: string;
    day?: string;
    time?: string;
    remindType?: string;
  };
  fieldTypeConfig?: {
    name: string;
    value: unknown;
    operator: string;
    multipleValue?: boolean;
    type?: string;
    containChildIds?: string[];
  }[];
}

export interface MessageTaskResponse {
  id: string;
  name: string;
  taskType: string;
  type: 'SYSTEM' | 'CUSTOM';
  triggerType: string;
  scopeId: string[];
  members: MessageTaskScopeMember[];
  receiveMethod: string;
  value: string;
  config: MessageTaskTriggerConfig;
  createUser?: string;
  createUserName?: string;
  updateUser?: string;
  updateUserName?: string;
  createTime?: number;
  updateTime?: number;
  template: string;
  templateValue: string[];
  enable: boolean;
}

export interface MessageTaskSaveParams {
  id?: string;
  name: string;
  taskType: string;
  scopeId: string[];
  receiveMethod: string;
  triggerType: string;
  config: MessageTaskTriggerConfig;
  template: string;
  type: 'SYSTEM' | 'CUSTOM';
}

export interface MessageCenterItem {
  id: string;
  type: string; // 通知类型
  receiver: string;
  subject: string;
  status: string;
  operator: string;
  operation: string;
  organizationId: string;
  resourceId: string;
  resourceType: string; // 资源类型
  resourceName: string; // 资源名称
  content: string; // 通知内容
  createUser: string;
  updateUser: string;
  createTime: number;
  updateTime: number;
  contentText: string;
  triggerType?: string;
}

export interface MessageCenterQueryParams extends TableQueryParams {
  type: string;
  status: string;
  resourceType: string;
  createTime: number | null;
  endTime: number | null;
  triggerType?: string;
}

export type MessageCenterSubsetParams = Pick<
  MessageCenterQueryParams,
  'type' | 'status' | 'resourceType' | 'createTime' | 'endTime' | 'triggerType'
>;
