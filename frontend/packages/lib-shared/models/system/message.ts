import type { TableQueryParams } from '../common';

export interface AnnouncementSaveParams {
  id?: string;
  subject: string; // 公告标题
  content: string;
  startTime: number;
  endTime: number;
  url: string; // 链接
  organizationId: string;
  deptIds: string[];
  roleIds: string[];
  userIds: string[];
  renameUrl: string;
  range: [number, number] | undefined;
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
  deptIdName: { id: string; name: string }[]; // 部门
  roleIdName: { id: string; name: string }[]; // 角色
  userIdName: { id: string; name: string }[]; // 用户
}

export interface RobotConfigMap {
  receiveType: string; // 接收类型
  enable: boolean; // 是否启用
  useDefaultTemplate: boolean;
  template: string;
  defaultTemplate: string;
  subject: string;
  previewTemplate: string; // 预览模板
}

export interface MessageTaskDetailDTOItem {
  event: string;
  eventName: string;
  receivers: { id: string; name: string }[];
  projectRobotConfigMap: Record<string, RobotConfigMap>;
}

export interface MessageConfigItem {
  type: string; // 消息配置功能
  name: string; // 消息配置功能名称
  enable: boolean;
  messageTaskDetailDTOList: MessageTaskDetailDTOItem[];
}

// TODO 接口参数要调整
export interface SaveMessageConfigParams {
  module: string;
  event: string;
  receiverIds: string[];
  testId: string;
  receiveType: string;
  enable: boolean;
  template: string;
  useDefaultTemplate: boolean;
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
