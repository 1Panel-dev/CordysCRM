import {
  AddOptFollowPlanUrl,
  AddOptFollowRecordUrl,
  GetOptFollowPlanUrl,
  GetOptFollowRecordUrl,
  GetOptFormConfigUrl,
  OptAddUrl,
  OptFollowPlanPageUrl,
  OptFollowRecordListUrl,
  OptPageUrl,
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
  UpdateCustomerFollowPlanParams,
  UpdateCustomerFollowRecordParams,
} from '@lib/shared/models/customer';
import type { OpportunityItem, SaveOpportunityParams } from '@lib/shared/models/opportunity';
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
