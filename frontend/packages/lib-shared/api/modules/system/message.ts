import type { CordysAxios } from '@lib/shared/api/http/Axios';
import {
  AddAnnouncementUrl,
  AddMessageTaskUrl,
  BatchUpdateMessageTaskReceiveMethodUrl,
  CloseMessageUrl,
  DeleteAnnouncementUrl,
  DeleteMessageTaskUrl,
  EnableMessageTaskUrl,
  GetAnnouncementDetailUrl,
  GetAnnouncementListUrl,
  GetHomeMessageUrl,
  GetMessageTaskCustomFormTypesUrl,
  GetMessageTaskPageUrl,
  GetMessageTaskSystemTypesUrl,
  GetNotificationCountUrl,
  GetNotificationListUrl,
  GetUnReadAnnouncement,
  SetAllNotificationReadUrl,
  SetNotificationReadUrl,
  UpdateAnnouncementUrl,
  UpdateMessageTaskUrl,
} from '@lib/shared/api/requrls/system/message';
import type { CommonList } from '@lib/shared/models/common';
import type {
  AnnouncementItemDetail,
  AnnouncementSaveParams,
  AnnouncementTableQueryParams,
  MessageCenterItem,
  MessageCenterQueryParams,
  MessageTaskBatchUpdateReceiveMethodParams,
  MessageTaskCountDTO,
  MessageTaskPageParams,
  MessageTaskResponse,
  MessageTaskSaveParams,
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

  function getMessageTaskSystemTypes() {
    return CDR.get<MessageTaskCountDTO[]>({ url: GetMessageTaskSystemTypesUrl });
  }

  function getMessageTaskCustomFormTypes() {
    return CDR.get<MessageTaskCountDTO[]>({ url: GetMessageTaskCustomFormTypesUrl });
  }

  function getMessageTaskPage(data: MessageTaskPageParams) {
    return CDR.post<CommonList<MessageTaskResponse>>({ url: GetMessageTaskPageUrl, data });
  }

  function addMessageTask(data: MessageTaskSaveParams) {
    return CDR.post({ url: AddMessageTaskUrl, data });
  }

  function updateMessageTask(data: MessageTaskSaveParams & { id: string }) {
    return CDR.post({ url: UpdateMessageTaskUrl, data });
  }

  function batchUpdateMessageTaskReceiveMethod(data: MessageTaskBatchUpdateReceiveMethodParams) {
    return CDR.post({ url: BatchUpdateMessageTaskReceiveMethodUrl, data });
  }

  function deleteMessageTask(id: string) {
    return CDR.get({ url: `${DeleteMessageTaskUrl}/${id}` });
  }

  function enableMessageTask(id: string) {
    return CDR.get({ url: `${EnableMessageTaskUrl}/${id}` });
  }

  // 获取首页消息列表
  function getHomeMessageList() {
    return CDR.get<MessageCenterItem[]>({ url: GetHomeMessageUrl });
  }

  // 关闭订阅消息SSE事件流
  function closeMessageSubscribe(params: { userId: string; clientId: string }) {
    return CDR.get({ url: CloseMessageUrl, params }, { ignoreCancelToken: true });
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
    getMessageTaskSystemTypes,
    getMessageTaskCustomFormTypes,
    getMessageTaskPage,
    addMessageTask,
    updateMessageTask,
    batchUpdateMessageTaskReceiveMethod,
    deleteMessageTask,
    enableMessageTask,
    getHomeMessageList,
    closeMessageSubscribe,
    getUnReadAnnouncement,
  };
}
