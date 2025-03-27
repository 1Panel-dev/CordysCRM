import {
  AddClueFollowPlanUrl,
  AddClueFollowRecordUrl,
  AddClueUrl,
  AssignClueUrl,
  BatchAssignClueUrl,
  BatchDeleteCluePoolUrl,
  BatchDeleteClueUrl,
  BatchPickClueUrl,
  BatchToPoolClueUrl,
  BatchTransferClueUrl,
  CancelClueFollowPlanUrl,
  ClueTransitionCustomerUrl,
  ClueTransitionOpportunityUrl,
  DeleteClueFollowPlanUrl,
  DeleteClueFollowRecordUrl,
  DeleteCluePoolUrl,
  DeleteClueUrl,
  GetClueFollowPlanListUrl,
  GetClueFollowPlanUrl,
  GetClueFollowRecordListUrl,
  GetClueFollowRecordUrl,
  GetClueFormConfigUrl,
  GetClueHeaderListUrl,
  GetClueListUrl,
  GetCluePoolFollowRecordListUrl,
  GetCluePoolListUrl,
  GetClueUrl,
  GetPoolClueUrl,
  GetPoolOptionsUrl,
  PickClueUrl,
  UpdateClueFollowPlanUrl,
  UpdateClueFollowRecordUrl,
  UpdateClueStatusUrl,
  UpdateClueUrl,
} from '@lib/shared/api/requrls/clue';
import type {
  AssignClueParams,
  BatchAssignClueParams,
  BatchPickClueParams,
  ClueDetail,
  ClueListItem,
  CluePoolListItem,
  CluePoolTableParams,
  ClueTransitionCustomerParams,
  ClueTransitionOpportunityParams,
  PickClueParams,
  PoolOption,
  SaveClueParams,
  UpdateClueParams,
} from '@lib/shared/models/clue';
import type { CommonList } from '@lib/shared/models/common';
import type {
  CustomerContractTableParams,
  CustomerFollowPlanTableParams,
  CustomerFollowRecordTableParams,
  CustomerTableParams,
  FollowDetailItem,
  SaveCustomerFollowPlanParams,
  SaveCustomerFollowRecordParams,
  TransferParams,
  UpdateCustomerFollowPlanParams,
  UpdateCustomerFollowRecordParams,
} from '@lib/shared/models/customer';
import type { FormDesignConfigDetailParams } from '@lib/shared/models/system/module';

import CDR from '@/api/http/index';

// 添加线索
export function addClue(data: SaveClueParams) {
  return CDR.post({ url: AddClueUrl, data });
}

// 更新线索
export function updateClue(data: UpdateClueParams) {
  return CDR.post({ url: UpdateClueUrl, data });
}

// 更新线索状态
export function updateClueStatus(data: { id: string; stage: string }) {
  return CDR.post({ url: UpdateClueStatusUrl, data });
}

// 获取线索列表
export function getClueList(data: CustomerTableParams) {
  return CDR.post<CommonList<ClueListItem>>({ url: GetClueListUrl, data });
}

// 批量转移线索
export function batchTransferClue(data: TransferParams) {
  return CDR.post({ url: BatchTransferClueUrl, data });
}

// 批量移入线索池
export function batchToCluePool(data: string[]) {
  return CDR.post({ url: BatchToPoolClueUrl, data });
}

// 批量删除线索
export function batchDeleteClue(data: string[]) {
  return CDR.post({ url: BatchDeleteClueUrl, data });
}

// 获取线索表单配置
export function getClueFormConfig() {
  return CDR.get<FormDesignConfigDetailParams>({ url: GetClueFormConfigUrl });
}

// 获取线索详情
export function getClue(id: string) {
  return CDR.get<ClueDetail>({ url: `${GetClueUrl}/${id}` });
}

// 删除线索
export function deleteClue(id: string) {
  return CDR.get({ url: `${DeleteClueUrl}/${id}` });
}

// 转为商机
export function ClueTransitionOpportunity(data: ClueTransitionOpportunityParams) {
  return CDR.post({ url: ClueTransitionOpportunityUrl, data });
}

// 转为客户
export function ClueTransitionCustomer(data: ClueTransitionCustomerParams) {
  return CDR.post({ url: ClueTransitionCustomerUrl, data });
}

// 添加线索跟进记录
export function addClueFollowRecord(data: SaveCustomerFollowRecordParams) {
  return CDR.post({ url: AddClueFollowRecordUrl, data });
}

// 更新线索跟进记录
export function updateClueFollowRecord(data: UpdateCustomerFollowRecordParams) {
  return CDR.post({ url: UpdateClueFollowRecordUrl, data });
}

// 获取线索跟进记录列表
export function getClueFollowRecordList(data: CustomerFollowRecordTableParams) {
  return CDR.post<CommonList<FollowDetailItem>>({ url: GetClueFollowRecordListUrl, data });
}

// 删除线索跟进记录
export function deleteClueFollowRecord(id: string) {
  return CDR.get({ url: `${DeleteClueFollowRecordUrl}/${id}` });
}

// 获取线索跟进记录详情
export function getClueFollowRecord(id: string) {
  return CDR.get<FollowDetailItem>({ url: `${GetClueFollowRecordUrl}/${id}` });
}

// 添加线索跟进计划
export function addClueFollowPlan(data: SaveCustomerFollowPlanParams) {
  return CDR.post({ url: AddClueFollowPlanUrl, data });
}

// 更新线索跟进计划
export function updateClueFollowPlan(data: UpdateCustomerFollowPlanParams) {
  return CDR.post({ url: UpdateClueFollowPlanUrl, data });
}

// 删除线索跟进计划
export function deleteClueFollowPlan(id: string) {
  return CDR.get({ url: `${DeleteClueFollowPlanUrl}/${id}` });
}

// 获取线索跟进计划列表
export function getClueFollowPlanList(data: CustomerFollowPlanTableParams) {
  return CDR.post<CommonList<FollowDetailItem>>({ url: GetClueFollowPlanListUrl, data });
}

// 获取线索跟进计划详情
export function getClueFollowPlan(id: string) {
  return CDR.get<FollowDetailItem>({ url: `${GetClueFollowPlanUrl}/${id}` });
}

// 取消跟进计划
export function cancelClueFollowPlan(id: string) {
  return CDR.get({ url: `${CancelClueFollowPlanUrl}/${id}` });
}

// 获取线索负责人列表
export function getClueHeaderList(data: CustomerContractTableParams) {
  return CDR.get({ url: `${GetClueHeaderListUrl}/${data.sourceId}` });
}

// 线索池领取线索
export function pickClue(data: PickClueParams) {
  return CDR.post({ url: PickClueUrl, data });
}

// 获取线索池线索列表
export function getCluePoolList(data: CluePoolTableParams) {
  return CDR.post<CommonList<CluePoolListItem>>({ url: GetCluePoolListUrl, data });
}

// 批量领取线索池线索
export function batchPickClue(data: BatchPickClueParams) {
  return CDR.post({ url: BatchPickClueUrl, data });
}

// 批量删除线索池线索
export function batchDeleteCluePool(data: string[]) {
  return CDR.post({ url: BatchDeleteCluePoolUrl, data });
}

// 批量分配线索池线索
export function batchAssignClue(data: BatchAssignClueParams) {
  return CDR.post({ url: BatchAssignClueUrl, data });
}

// 分配线索池线索
export function assignClue(data: AssignClueParams) {
  return CDR.post({ url: AssignClueUrl, data });
}

// 获取当前用户线索池选项
export function getPoolOptions() {
  return CDR.get<PoolOption[]>({ url: GetPoolOptionsUrl });
}

// 删除线索池线索
export function deleteCluePool(id: string) {
  return CDR.get({ url: `${DeleteCluePoolUrl}/${id}` });
}

// 获取线索池跟进记录列表
export function getCluePoolFollowRecordList(data: CustomerFollowRecordTableParams) {
  return CDR.post<CommonList<FollowDetailItem>>({ url: GetCluePoolFollowRecordListUrl, data });
}

// 获取线索池详情
export function getPoolClue(id: string) {
  return CDR.get<ClueDetail>({ url: `${GetPoolClueUrl}/${id}` });
}
