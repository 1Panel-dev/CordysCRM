import type { CordysAxios } from '@lib/shared/api/http/Axios';
import {
  ApprovalPermissionsUrl,
  AddApprovalProcessUrl,
  UpdateApprovalProcessUrl,
  DeleteApprovalProcessUrl,
  ApprovalProcessDetailUrl,
  ToggleApprovalProcessUrl,
  ApprovalProcessPageUrl,
  GetApprovalConfigDetailUrl,
  GetResourceApprovingDetailUrl,
  ReviewResourceUrl,
  RevokeResourceUrl,
  GetApprovalResourceDetailUrl,
  GetProcessedApprovalTodosUrl,
  GetPendingApprovalTodosUrl,
  GetInitiatedApprovalTodosUrl,
  GetCcApprovalTodosUrl,
  RejectApprovalUrl,
  BackApprovalUrl,
  AddSignApprovalUrl,
  GetTodoStatisticUrl,
  AgreeApprovalUrl,
  RevokeApprovalUrl,
  BatchRejectApprovalUrl,
  BatchApprovalApprovalUrl,
} from '@lib/shared/api/requrls/system/process';
import {
  AddApprovalProcessParams,
  ApprovalPermissionsDetail,
  ApprovalProcessDetail,
  ApprovalProcessItem,
  CommonApprovalActionParams,
  UpdateApprovalProcessParams,
  type ApprovalAddSignParams,
  type ApprovalBackParams,
  type ApprovalDetail,
  type ApprovalOperationParams,
  type ApprovalTodoItem,
  type ApprovalTodoTableParams,
  type BatchApprovalParams,
  type BatchRejectApprovalParams,
  type TodoStatistic,
} from '@lib/shared/models/system/process';
import type { CommonList } from '@lib/shared/models/common';
import type { TableQueryParams } from '@lib/shared/models/common';

export default function useProcessApi(CDR: CordysAxios) {
  // 审批流数据权限
  function getApprovalPermissions(type: string) {
    return CDR.get<ApprovalPermissionsDetail>({ url: `${ApprovalPermissionsUrl}/${type}` });
  }
  // 审批流配置详情 用于列表里边查询对应状态审批流详情
  function getApprovalConfigDetail(type: string) {
    return CDR.get<ApprovalProcessDetail>({ url: `${GetApprovalConfigDetailUrl}/${type}` });
  }
  // 审批流数据权限
  function getApprovalProcessList(data: TableQueryParams) {
    return CDR.post<CommonList<ApprovalProcessItem>>({ url: ApprovalProcessPageUrl, data });
  }
  // 添加审批流
  function addApprovalProcess(data: AddApprovalProcessParams) {
    return CDR.post({ url: AddApprovalProcessUrl, data });
  }
  // 更新审批流
  function updateApprovalProcess(data: UpdateApprovalProcessParams) {
    return CDR.post({ url: UpdateApprovalProcessUrl, data });
  }
  // 审批流详情
  function approvalProcessDetail(id: string) {
    return CDR.get<ApprovalProcessDetail>({ url: `${ApprovalProcessDetailUrl}/${id}` });
  }
  // 删除审批流
  function deleteApprovalProcess(id: string) {
    return CDR.get({ url: `${DeleteApprovalProcessUrl}/${id}` });
  }
  // 切换审批流
  function toggleApprovalProcess(id: string, enable: boolean) {
    return CDR.get({ url: `${ToggleApprovalProcessUrl}/${id}`, params: { enable } });
  }
  // 获取对应资源审批状态详情用于（列表小卡片）
  function getResourceApprovingDetail(sourceId: string) {
    return CDR.get({ url: `${GetResourceApprovingDetailUrl}/${sourceId}` });
  }

  // 获取已处理审批待办列表
  function getProcessedApprovalList(data: ApprovalTodoTableParams) {
    return CDR.post<CommonList<ApprovalTodoItem>>({ url: GetProcessedApprovalTodosUrl, data });
  }

  // 获取待处理审批待办列表
  function getPendingApprovalList(data: ApprovalTodoTableParams) {
    return CDR.post<CommonList<ApprovalTodoItem>>({ url: GetPendingApprovalTodosUrl, data });
  }

  // 获取我发起的审批待办列表
  function getInitiatedApprovalList(data: ApprovalTodoTableParams) {
    return CDR.post<CommonList<ApprovalTodoItem>>({ url: GetInitiatedApprovalTodosUrl, data });
  }

  // 获取抄送我的审批待办列表
  function getCcApprovalList(data: ApprovalTodoTableParams) {
    return CDR.post<CommonList<ApprovalTodoItem>>({ url: GetCcApprovalTodosUrl, data });
  }

  // 驳回
  function rejectApproval(data: ApprovalOperationParams) {
    return CDR.post({ url: RejectApprovalUrl, data });
  }

  // 退回
  function backApproval(data: ApprovalBackParams) {
    return CDR.post({ url: BackApprovalUrl, data });
  }

  // 加签
  function addSignApproval(data: ApprovalAddSignParams) {
    return CDR.post({ url: AddSignApprovalUrl, data });
  }

  // 撤回
  function revokeApproval(data: { id: string }) {
    return CDR.post({ url: RevokeApprovalUrl, data });
  }

  // 同意
  function agreeApproval(data: ApprovalOperationParams) {
    return CDR.post({ url: AgreeApprovalUrl, data });
  }

  // 批量驳回
  function batchRejectApproval(data: BatchRejectApprovalParams) {
    return CDR.post({ url: BatchRejectApprovalUrl, data });
  }

  // 批量同意
  function batchAgreeApproval(data: BatchApprovalParams) {
    return CDR.post({ url: BatchApprovalApprovalUrl, data });
  }

  // 获取审批资源详情
  function getApprovalResourceDetail(id: string) {
    return CDR.get<ApprovalDetail>({ url: `${GetApprovalResourceDetailUrl}/${id}` });
  }

  // 获取待办统计
  function getTodoStatistic() {
    return CDR.get<TodoStatistic>({ url: GetTodoStatisticUrl });
  }

  // 提审
  function reviewResource(data: CommonApprovalActionParams) {
    return CDR.post({ url: ReviewResourceUrl, data });
  }

  // 撤销
  function revokeResource(data: CommonApprovalActionParams) {
    return CDR.post({ url: RevokeResourceUrl, data });
  }

  return {
    getApprovalProcessList,
    getApprovalPermissions,
    addApprovalProcess,
    updateApprovalProcess,
    approvalProcessDetail,
    deleteApprovalProcess,
    toggleApprovalProcess,
    getApprovalConfigDetail,
    getResourceApprovingDetail,
    reviewResource,
    revokeResource,
    getProcessedApprovalList,
    getPendingApprovalList,
    getInitiatedApprovalList,
    getCcApprovalList,
    rejectApproval,
    backApproval,
    addSignApproval,
    getApprovalResourceDetail,
    getTodoStatistic,
    revokeApproval,
    agreeApproval,
    batchRejectApproval,
    batchAgreeApproval,
  };
}
