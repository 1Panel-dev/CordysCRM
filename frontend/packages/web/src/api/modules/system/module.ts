import CDR from '@/api/http/index';

import {
  AddCustomerPoolUrl,
  AddLeadPoolUrl,
  addOpportunityRuleUrl,
  DeleteCustomerPoolUrl,
  DeleteLeadPoolUrl,
  deleteOpportunityUrl,
  GetCustomerCapacityPageUrl,
  GetCustomerPoolPageUrl,
  GetLeadCapacityPageUrl,
  GetLeadPoolPageUrl,
  getModuleNavConfigListUrl,
  getOpportunityListUrl,
  moduleNavListSortUrl,
  SaveCustomerCapacityUrl,
  SaveLeadCapacityUrl,
  SwitchCustomerPoolStatusUrl,
  SwitchLeadPoolStatusUrl,
  switchOpportunityStatusUrl,
  toggleModuleNavStatusUrl,
  UpdateCustomerPoolUrl,
  UpdateLeadPoolUrl,
  updateOpportunityRuleUrl,
} from '@lib/shared/api/requrls/system/module';
import { ModuleConfigEnum } from '@lib/shared/enums/moduleEnum';
import type { CommonList, TableQueryParams } from '@lib/shared/models/common';
import type {
  CapacityItem,
  CapacityParams,
  LeadPoolItem,
  LeadPoolParams,
  ModuleNavBaseInfoItem,
  ModuleSortParams,
  OpportunityItem,
  OpportunityParams,
} from '@lib/shared/models/system/module';

// 模块首页-导航模块列表
export function getModuleNavConfigList(data: { organizationId: string }) {
  return CDR.post<ModuleNavBaseInfoItem[]>({ url: getModuleNavConfigListUrl, data });
}

// 模块首页-导航模块排序
export function moduleNavListSort(data: ModuleSortParams) {
  return CDR.post({ url: moduleNavListSortUrl, data });
}

// 模块首页-导航模块状态切换
export function toggleModuleNavStatus(id: string) {
  return CDR.get({ url: `${toggleModuleNavStatusUrl}/${id}` });
}

// 模块-商机-商机规则列表
export function getOpportunityList(data: TableQueryParams) {
  return CDR.post<CommonList<OpportunityItem>>({ url: getOpportunityListUrl, data });
}

// 模块-商机-添加商机规则
export function addOpportunityRule(data: OpportunityParams) {
  return CDR.post({ url: addOpportunityRuleUrl, data });
}

// 模块-商机-更新商机规则
export function updateOpportunityRule(data: OpportunityParams) {
  return CDR.post({ url: updateOpportunityRuleUrl, data });
}

// 模块-商机-更新商机规则状态
export function switchOpportunityStatus(ruleId: string) {
  return CDR.get({ url: `${switchOpportunityStatusUrl}/${ruleId}` });
}

// 模块-商机-删除商机规则
export function deleteOpportunity(ruleId: string) {
  return CDR.get({ url: `${deleteOpportunityUrl}/${ruleId}` });
}

// 线索池相关API
export function getLeadPoolPage(data: TableQueryParams) {
  return CDR.post<CommonList<LeadPoolItem>>({ url: GetLeadPoolPageUrl, data });
}

export function addLeadPool(data: LeadPoolParams) {
  return CDR.post({ url: AddLeadPoolUrl, data });
}

export function updateLeadPool(data: LeadPoolParams) {
  return CDR.post({ url: UpdateLeadPoolUrl, data });
}

export function switchLeadPoolStatus(id: string) {
  return CDR.get({ url: `${SwitchLeadPoolStatusUrl}/${id}` });
}

export function deleteLeadPool(id: string) {
  return CDR.get({ url: `${DeleteLeadPoolUrl}/${id}` });
}

// 库容相关API
export function getCapacityPage(type: ModuleConfigEnum) {
  return CDR.post<CapacityItem[]>({
    url: type === ModuleConfigEnum.CLUE_MANAGEMENT ? GetLeadCapacityPageUrl : GetCustomerCapacityPageUrl,
  });
}

export function saveCapacity(data: CapacityParams[], type: ModuleConfigEnum) {
  return CDR.post({
    url: type === ModuleConfigEnum.CLUE_MANAGEMENT ? SaveLeadCapacityUrl : SaveCustomerCapacityUrl,
    data,
  });
}

// 公海相关API
export function getCustomerPoolPage(data: TableQueryParams) {
  return CDR.post<CommonList<LeadPoolItem>>({ url: GetCustomerPoolPageUrl, data });
}

export function addCustomerPool(data: LeadPoolParams) {
  return CDR.post({ url: AddCustomerPoolUrl, data });
}

export function updateCustomerPool(data: LeadPoolParams) {
  return CDR.post({ url: UpdateCustomerPoolUrl, data });
}

export function switchCustomerPoolStatus(id: string) {
  return CDR.get({ url: `${SwitchCustomerPoolStatusUrl}/${id}` });
}

export function deleteCustomerPool(id: string) {
  return CDR.get({ url: `${DeleteCustomerPoolUrl}/${id}` });
}
