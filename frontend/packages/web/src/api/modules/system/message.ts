import {
  AddAnnouncementUrl,
  DeleteAnnouncementUrl,
  GetAnnouncementDetailUrl,
  GetAnnouncementListUrl,
  GetMessageTaskUrl,
  GetNotificationCountUrl,
  GetNotificationListUrl,
  SaveMessageTaskUrl,
  SetAllNotificationReadUrl,
  SetNotificationReadUrl,
  UpdateAnnouncementUrl,
} from '@lib/shared/api/requrls/system/message';
import type { CommonList } from '@lib/shared/models/common';
import type {
  AnnouncementItemDetail,
  AnnouncementSaveParams,
  AnnouncementTableQueryParams,
  MessageCenterItem,
  MessageCenterQueryParams,
  MessageConfigItem,
  SaveMessageConfigParams,
} from '@lib/shared/models/system/message';

import CDR from '@/api/http/index';

// 公告
// 添加公告
export function addAnnouncement(data: AnnouncementSaveParams) {
  return CDR.post({ url: AddAnnouncementUrl, data });
}

// 更新公告
export function updateAnnouncement(data: AnnouncementSaveParams) {
  return CDR.post({ url: UpdateAnnouncementUrl, data });
}

// 获取公告列表
export function getAnnouncementList(data: AnnouncementTableQueryParams) {
  return CDR.post<CommonList<AnnouncementItemDetail>>({ url: GetAnnouncementListUrl, data });
}

// 公告详情
export function getAnnouncementDetail(id: string) {
  return CDR.get<AnnouncementItemDetail>({ url: `${GetAnnouncementDetailUrl}/${id}` });
}

// 删除公告
export function deleteAnnouncement(id: string) {
  return CDR.get({ url: `${DeleteAnnouncementUrl}/${id}` });
}

// 消息中心
// 消息列表
export function getNotificationList(data: MessageCenterQueryParams) {
  return CDR.post<CommonList<MessageCenterItem>>({ url: GetNotificationListUrl, data });
}

// 具体消息类型具体状态的数量 TODOts
export function getNotificationCount(data: MessageCenterQueryParams) {
  return CDR.post<any>({ url: GetNotificationCountUrl, data });
}

// 设置消息已读
export function setNotificationRead(id: string) {
  return CDR.get({ url: `${SetNotificationReadUrl}/${id}` });
}

// 所有信息设置为已读消息
export function setAllNotificationRead() {
  return CDR.get({ url: SetAllNotificationReadUrl });
}

// 获取消息设置
export function getMessageTask() {
  return CDR.get<MessageConfigItem[]>({ url: GetMessageTaskUrl });
}
// 保存消息设置
export function saveMessageTask(data: SaveMessageConfigParams) {
  return CDR.post<MessageConfigItem>({ url: SaveMessageTaskUrl, data });
}
