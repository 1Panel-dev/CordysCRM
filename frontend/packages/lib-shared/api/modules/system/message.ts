import type { CordysAxios } from '@lib/shared/api/http/Axios';
import {
  AddAnnouncementUrl,
  BatchSaveMessageTaskUrl,
  CloseMessageUrl,
  DeleteAnnouncementUrl,
  GetAnnouncementDetailUrl,
  GetAnnouncementListUrl,
  GetHomeMessageUrl,
  GetMessageTaskUrl,
  GetNotificationCountUrl,
  GetNotificationListUrl,
  GetUnReadAnnouncement,
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

export default function useProductApi(CDR: CordysAxios) {
  // 公告
  // 添加公告
  function addAnnouncement(data: AnnouncementSaveParams) {
    return CDR.post({ url: AddAnnouncementUrl, data });
  }

  // 更新公告
  function updateAnnouncement(data: AnnouncementSaveParams) {
    return CDR.post({ url: UpdateAnnouncementUrl, data });
  }

  // 获取公告列表
  function getAnnouncementList(data: AnnouncementTableQueryParams) {
    return CDR.post<CommonList<AnnouncementItemDetail>>({ url: GetAnnouncementListUrl, data });
  }

  // 公告详情
  function getAnnouncementDetail(id: string) {
    return CDR.get<AnnouncementItemDetail>({ url: `${GetAnnouncementDetailUrl}/${id}` });
  }

  // 删除公告
  function deleteAnnouncement(id: string) {
    return CDR.get({ url: `${DeleteAnnouncementUrl}/${id}` });
  }

  // 消息中心
  // 消息列表
  function getNotificationList(data: MessageCenterQueryParams) {
    return CDR.post<CommonList<MessageCenterItem>>({ url: GetNotificationListUrl, data });
  }

  // 具体消息类型具体状态的数量
  function getNotificationCount(data: MessageCenterQueryParams) {
    return CDR.post<{ key: string; count: number }[]>({ url: GetNotificationCountUrl, data });
  }

  // 设置消息已读
  function setNotificationRead(id: string) {
    return CDR.get({ url: `${SetNotificationReadUrl}/${id}` });
  }

  // 所有信息设置为已读消息
  function setAllNotificationRead() {
    return CDR.get({ url: SetAllNotificationReadUrl });
  }

  // 获取消息设置
  function getMessageTask() {
    return CDR.get<MessageConfigItem[]>({ url: GetMessageTaskUrl });
  }

  // 获取首页消息列表
  function getHomeMessageList() {
    return CDR.get<MessageCenterItem[]>({ url: GetHomeMessageUrl });
  }

  // 保存消息设置
  function saveMessageTask(data: SaveMessageConfigParams) {
    return CDR.post({ url: SaveMessageTaskUrl, data });
  }

  // 批量编辑消息设置
  function batchSaveMessageTask(data: Pick<SaveMessageConfigParams, 'emailEnable' | 'sysEnable'>) {
    return CDR.post({ url: BatchSaveMessageTaskUrl, data });
  }

  // 关闭订阅消息SSE事件流
  function closeMessageSubscribe(params: { userId: string; clientId: string }) {
    return CDR.get({ url: CloseMessageUrl, params });
  }

  // 获取未读公告
  function getUnReadAnnouncement() {
    return CDR.get<MessageCenterItem[]>({ url: GetUnReadAnnouncement });
  }

  return {
    addAnnouncement,
    updateAnnouncement,
    getAnnouncementList,
    getAnnouncementDetail,
    deleteAnnouncement,
    getNotificationList,
    getNotificationCount,
    setNotificationRead,
    setAllNotificationRead,
    getMessageTask,
    saveMessageTask,
    batchSaveMessageTask,
    getHomeMessageList,
    closeMessageSubscribe,
    getUnReadAnnouncement,
  };
}
