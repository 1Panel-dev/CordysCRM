import {
  AddOptFollowPlanUrl,
  AddOptFollowRecordUrl,
  CancelOptFollowPlanUrl,
  GetOptFollowPlanUrl,
  GetOptFollowRecordUrl,
  GetOptFormConfigUrl,
  GetOptStageDetailUrl,
  OptAddUrl,
  OptBatchDeleteUrl,
  OptBatchTransferUrl,
  OptDeleteUrl,
  OptFollowPlanPageUrl,
  OptFollowRecordListUrl,
  OptPageUrl,
  OptUpdateStageUrl,
  OptUpdateUrl,
  UpdateOptFollowPlanUrl,
  UpdateOptFollowRecordUrl,
} from '@lib/shared/api/requrls/opportunity';
import type { CommonList, TableQueryParams } from '@lib/shared/models/common';
import type {
  CustomerFollowPlanTableParams,
  CustomerFollowRecordTableParams,
  FollowDetailItem,
  SaveCustomerFollowPlanParams,
  SaveCustomerFollowRecordParams,
  TransferParams,
  UpdateCustomerFollowPlanParams,
  UpdateCustomerFollowRecordParams,
} from '@lib/shared/models/customer';
import type { OpportunityItem, SaveOpportunityParams, UpdateOpportunityParams } from '@lib/shared/models/opportunity';
import type { FormDesignConfigDetailParams } from '@lib/shared/models/system/module';

import CDR from '@/api/http/index';

// 商机列表
export function getOpportunityList(data: TableQueryParams) {
  return CDR.post<CommonList<OpportunityItem>>({ url: OptPageUrl, data });
}

// 添加商机
export function addOpportunity(data: SaveOpportunityParams) {
  return CDR.post({ url: OptAddUrl, data });
}

// 更新商机
export function updateOpportunity(data: UpdateOpportunityParams) {
  return CDR.post({ url: OptUpdateUrl, data });
}

// 获取商机表单配置
export function getOptFormConfig() {
  return CDR.get<FormDesignConfigDetailParams>({ url: GetOptFormConfigUrl });
}

// 商机跟进记录列表
export function getOptFollowRecordList(data: CustomerFollowRecordTableParams) {
  return CDR.post<CommonList<FollowDetailItem>>({ url: OptFollowRecordListUrl, data });
}

// 添加商机跟进记录
export function addOptFollowRecord(data: SaveCustomerFollowRecordParams) {
  return CDR.post({ url: AddOptFollowRecordUrl, data });
}

// 更新商机跟进记录
export function updateOptFollowRecord(data: UpdateCustomerFollowRecordParams) {
  return CDR.post({ url: UpdateOptFollowRecordUrl, data });
}

// 获取商机跟进记录详情
export function getOptFollowRecord(id: string) {
  return CDR.get<FollowDetailItem>({ url: `${GetOptFollowRecordUrl}/${id}` });
}

// 跟进计划列表
export function getOptFollowPlanList(data: CustomerFollowPlanTableParams) {
  return CDR.post<CommonList<FollowDetailItem>>({ url: OptFollowPlanPageUrl, data });
}

// 添加商机跟进计划
export function addOptFollowPlan(data: SaveCustomerFollowPlanParams) {
  return CDR.post({ url: AddOptFollowPlanUrl, data });
}

// 更新商机跟进计划
export function updateOptFollowPlan(data: UpdateCustomerFollowPlanParams) {
  return CDR.post({ url: UpdateOptFollowPlanUrl, data });
}

// 获取商机跟进计划详情
export function getOptFollowPlan(id: string) {
  return CDR.get<FollowDetailItem>({ url: `${GetOptFollowPlanUrl}/${id}` });
}

// 取消商机跟进计划
export function cancelOptFollowPlan(id: string) {
  return CDR.get({ url: `${CancelOptFollowPlanUrl}/${id}` });
}

// 批量转移商机
export function transferOpt(data: TransferParams) {
  return CDR.post({ url: OptBatchTransferUrl, data });
}

// 批量删除商机
export function batchDeleteOpt(data: (string | number)[]) {
  return CDR.post({ url: OptBatchDeleteUrl, data });
}

// 删除商机
export function deleteOpt(id: string) {
  return CDR.get({ url: `${OptDeleteUrl}/${id}` });
}

// 更新商机阶段
export function updateOptStage(data: { id: string; stage: string }) {
  return CDR.post({ url: OptUpdateStageUrl, data });
}

// 获取商机阶段详情 TODO ts类型
export function getOptStageDetail(id: string) {
  return CDR.get<any>({ url: `${GetOptStageDetailUrl}/${id}` });
}
