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

export interface MessageTaskDetailDTOItem {
  event: string;
  eventName: string;
  sysEnable: boolean;
  emailEnable: boolean;
}

export interface MessageConfigItem extends MessageTaskDetailDTOItem {
  id: string;
  module: string; // 消息配置功能
  moduleName: string; // 消息配置功能名称
  messageTaskDetailDTOList: MessageTaskDetailDTOItem[];
}

export interface SaveMessageConfigParams {
  module: string;
  event: string;
  emailEnable: boolean;
  sysEnable: boolean;
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
}

export interface MessageCenterQueryParams extends TableQueryParams {
  type: string;
  status: string;
  resourceType: string;
  createTime: number | null;
  endTime: number | null;
}

export type MessageCenterSubsetParams = Pick<
  MessageCenterQueryParams,
  'type' | 'status' | 'resourceType' | 'createTime' | 'endTime'
>;
