import type { CordysAxios } from '@lib/shared/api/http/Axios';
import {
  AddFollowPlanViewUrl,
  AddFollowRecordViewUrl,
  DeleteFollowPlanUrl,
  DeleteFollowPlanViewUrl,
  DeleteFollowRecordUrl,
  DeleteFollowRecordViewUrl,
  DragFollowPlanViewUrl,
  DragFollowRecordViewUrl,
  EnableFollowPlanViewUrl,
  EnableFollowRecordViewUrl,
  FixedFollowPlanViewUrl,
  FixedFollowRecordViewUrl,
  GetFollowPlanPageUrl,
  GetFollowPlanTabUrl,
  GetFollowPlanUrl,
  GetFollowPlanViewDetailUrl,
  GetFollowPlanViewListUrl,
  GetFollowRecordPageUrl,
  GetFollowRecordTabUrl,
  GetFollowRecordUrl,
  GetFollowRecordViewDetailUrl,
  GetFollowRecordViewListUrl,
  UpdateFollowPlanStatusUrl,
  UpdateFollowPlanUrl,
  UpdateFollowPlanViewUrl,
  UpdateFollowRecordUrl,
  AddFollowRecordUrl,
  AddFollowPlanUrl,
  UpdateFollowRecordViewUrl,
} from '@lib/shared/api/requrls/follow';
import type { CommonList, TableDraggedParams } from '@lib/shared/models/common';
import type {
  CustomerFollowRecordTableParams,
  CustomerTabHidden,
  FollowDetailItem,
  UpdateCustomerFollowRecordParams,
  UpdateFollowPlanStatusParams,
} from '@lib/shared/models/customer';
import type { ViewItem, ViewParams } from '@lib/shared/models/view';

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

  function addFollowRecord(data: UpdateCustomerFollowRecordParams) {
    return CDR.post({ url: AddFollowRecordUrl, data });
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

  function addFollowPlan(data: UpdateCustomerFollowRecordParams) {
    return CDR.post({ url: AddFollowPlanUrl, data });
  }

  // 视图
  function addFollowRecordView(data: ViewParams) {
    return CDR.post({ url: AddFollowRecordViewUrl, data });
  }

  function updateFollowRecordView(data: ViewParams) {
    return CDR.post({ url: UpdateFollowRecordViewUrl, data });
  }

  function getFollowRecordViewList() {
    return CDR.get<ViewItem[]>({ url: GetFollowRecordViewListUrl });
  }

  function getFollowRecordViewDetail(id: string) {
    return CDR.get({ url: `${GetFollowRecordViewDetailUrl}/${id}` });
  }

  function fixedFollowRecordView(id: string) {
    return CDR.get({ url: `${FixedFollowRecordViewUrl}/${id}` });
  }

  function enableFollowRecordView(id: string) {
    return CDR.get({ url: `${EnableFollowRecordViewUrl}/${id}` });
  }

  function deleteFollowRecordView(id: string) {
    return CDR.get({ url: `${DeleteFollowRecordViewUrl}/${id}` });
  }

  function dragFollowRecordView(data: TableDraggedParams) {
    return CDR.post({ url: DragFollowRecordViewUrl, data });
  }

  // 跟进计划视图
  function addFollowPlanView(data: ViewParams) {
    return CDR.post({ url: AddFollowPlanViewUrl, data });
  }

  function updateFollowPlanView(data: ViewParams) {
    return CDR.post({ url: UpdateFollowPlanViewUrl, data });
  }

  function getFollowPlanViewList() {
    return CDR.get<ViewItem[]>({ url: GetFollowPlanViewListUrl });
  }

  function getFollowPlanViewDetail(id: string) {
    return CDR.get({ url: `${GetFollowPlanViewDetailUrl}/${id}` });
  }

  function fixedFollowPlanView(id: string) {
    return CDR.get({ url: `${FixedFollowPlanViewUrl}/${id}` });
  }

  function enableFollowPlanView(id: string) {
    return CDR.get({ url: `${EnableFollowPlanViewUrl}/${id}` });
  }

  function deleteFollowPlanView(id: string) {
    return CDR.get({ url: `${DeleteFollowPlanViewUrl}/${id}` });
  }

  function dragFollowPlanView(data: TableDraggedParams) {
    return CDR.post({ url: DragFollowPlanViewUrl, data });
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
    addFollowRecord,
    addFollowPlan,
    addFollowRecordView,
    updateFollowRecordView,
    getFollowRecordViewList,
    getFollowRecordViewDetail,
    fixedFollowRecordView,
    enableFollowRecordView,
    deleteFollowRecordView,
    dragFollowRecordView,
    addFollowPlanView,
    updateFollowPlanView,
    getFollowPlanViewList,
    getFollowPlanViewDetail,
    fixedFollowPlanView,
    enableFollowPlanView,
    deleteFollowPlanView,
    dragFollowPlanView,
  };
}
