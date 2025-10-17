import type { CordysAxios } from '@lib/shared/api/http/Axios';
import {
  DeleteFollowPlanUrl,
  DeleteFollowRecordUrl,
  GetFollowPlanPageUrl,
  GetFollowPlanTabUrl,
  GetFollowPlanUrl,
  GetFollowRecordPageUrl,
  GetFollowRecordTabUrl,
  GetFollowRecordUrl,
  UpdateFollowPlanStatusUrl,
  UpdateFollowPlanUrl,
  UpdateFollowRecordUrl,
} from '@lib/shared/api/requrls/follow';
import type { CommonList } from '@lib/shared/models/common';
import type {
  CustomerFollowRecordTableParams,
  CustomerTabHidden,
  FollowDetailItem,
  UpdateCustomerFollowRecordParams,
  UpdateFollowPlanStatusParams,
} from '@lib/shared/models/customer';

export default function useFollowApi(CDR: CordysAxios) {
  // 跟进记录列表
  function getFollowRecordPage(data: CustomerFollowRecordTableParams) {
    return CDR.post<CommonList<FollowDetailItem>>({ url: GetFollowRecordPageUrl, data });
  }

  // 跟进记录详情
  function getFollowRecordDetail(id: string) {
    return CDR.get<FollowDetailItem>({ url: `${GetFollowRecordUrl}/${id}` });
  }

  // 获取tab显隐藏
  function getFollowRecordTab() {
    return CDR.get<CustomerTabHidden>({ url: GetFollowRecordTabUrl });
  }

  function deleteFollowRecord(id: string) {
    return CDR.get({ url: `${DeleteFollowRecordUrl}/${id}` });
  }

  // 跟进计划列表
  function getFollowPLanPage(data: CustomerFollowRecordTableParams) {
    return CDR.post<CommonList<FollowDetailItem>>({ url: GetFollowPlanPageUrl, data });
  }

  // 跟进记录详情
  function getFollowPlanDetail(id: string) {
    return CDR.get<FollowDetailItem>({ url: `${GetFollowPlanUrl}/${id}` });
  }

  function updateFollowRecord(data: UpdateCustomerFollowRecordParams) {
    return CDR.post({ url: UpdateFollowRecordUrl, data });
  }

  // 获取tab显隐藏
  function getFollowPlanTab() {
    return CDR.get<CustomerTabHidden>({ url: GetFollowPlanTabUrl });
  }

  function deleteFollowPlan(id: string) {
    return CDR.get({ url: `${DeleteFollowPlanUrl}/${id}` });
  }

  function updateFollowPlanStatus(data: UpdateFollowPlanStatusParams) {
    return CDR.post({ url: UpdateFollowPlanStatusUrl, data });
  }

  function updateFollowPlan(data: UpdateCustomerFollowRecordParams) {
    return CDR.post({ url: UpdateFollowPlanUrl, data });
  }

  return {
    getFollowPlanDetail,
    getFollowPLanPage,
    getFollowRecordDetail,
    getFollowRecordPage,
    deleteFollowRecord,
    getFollowRecordTab,
    getFollowPlanTab,
    deleteFollowPlan,
    updateFollowPlanStatus,
    updateFollowPlan,
    updateFollowRecord,
  };
}
