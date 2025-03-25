import {
  AddAnnouncementUrl,
  DeleteAnnouncementUrl,
  GetAnnouncementDetailUrl,
  GetAnnouncementListUrl,
  UpdateAnnouncementUrl,
} from '@lib/shared/api/requrls/system/message';
import type { CommonList } from '@lib/shared/models/common';
import type {
  AnnouncementItemDetail,
  AnnouncementSaveParams,
  AnnouncementTableQueryParams,
} from '@lib/shared/models/system/message';

import CDR from '@/api/http/index';

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
