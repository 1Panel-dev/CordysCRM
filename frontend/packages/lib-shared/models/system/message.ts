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
