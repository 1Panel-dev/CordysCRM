import {
  AddCluePoolUrl,
  AddCustomerPoolUrl,
  addOpportunityRuleUrl,
  DeleteCluePoolUrl,
  DeleteCustomerPoolUrl,
  deleteOpportunityUrl,
  GetClueCapacityPageUrl,
  GetCluePoolPageUrl,
  GetCustomerCapacityPageUrl,
  GetCustomerPoolPageUrl,
  GetFormDesignConfigUrl,
  getModuleNavConfigListUrl,
  getOpportunityListUrl,
  moduleNavListSortUrl,
  ModuleRoleTreeUrl,
  ModuleUserDeptTreeUrl,
  NoPickCluePoolUrl,
  NoPickCustomerPoolUrl,
  SaveClueCapacityUrl,
  SaveCustomerCapacityUrl,
  SaveFormDesignConfigUrl,
  SwitchCluePoolStatusUrl,
  SwitchCustomerPoolStatusUrl,
  switchOpportunityStatusUrl,
  toggleModuleNavStatusUrl,
  UpdateCluePoolUrl,
  UpdateCustomerPoolUrl,
  updateOpportunityRuleUrl,
} from '@lib/shared/api/requrls/system/module';
import { ModuleConfigEnum } from '@lib/shared/enums/moduleEnum';
import type { CommonList, TableQueryParams } from '@lib/shared/models/common';
import type {
  CapacityItem,
  CapacityParams,
  CluePoolItem,
  CluePoolParams,
  FormDesignConfigDetailParams,
  ModuleNavBaseInfoItem,
  ModuleSortParams,
  OpportunityItem,
  OpportunityParams,
  SaveFormDesignConfigParams,
} from '@lib/shared/models/system/module';
import type { DeptUserTreeNode } from '@lib/shared/models/system/role';

import CDR from '@/api/http/index';

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

// 获取部门用户树
export function getModuleUserDeptTree() {
  return CDR.get<DeptUserTreeNode[]>({ url: ModuleUserDeptTreeUrl });
}
// 获取角色树
export function getModuleRoleTree() {
  return CDR.get<DeptUserTreeNode[]>({ url: ModuleRoleTreeUrl });
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
export function getCluePoolPage(data: TableQueryParams) {
  return CDR.post<CommonList<CluePoolItem>>({ url: GetCluePoolPageUrl, data });
}

export function addCluePool(data: CluePoolParams) {
  return CDR.post({ url: AddCluePoolUrl, data });
}

export function updateCluePool(data: CluePoolParams) {
  return CDR.post({ url: UpdateCluePoolUrl, data });
}

export function switchCluePoolStatus(id: string) {
  return CDR.get({ url: `${SwitchCluePoolStatusUrl}/${id}` });
}

export function deleteCluePool(id: string) {
  return CDR.get({ url: `${DeleteCluePoolUrl}/${id}` });
}

export function noPickCluePool(id: string) {
  return CDR.get({ url: `${NoPickCluePoolUrl}/${id}` });
}

// 库容相关API
export function getCapacityPage(type: ModuleConfigEnum) {
  return CDR.get<CapacityItem[]>({
    url: type === ModuleConfigEnum.CLUE_MANAGEMENT ? GetClueCapacityPageUrl : GetCustomerCapacityPageUrl,
  });
}

export function saveCapacity(data: CapacityParams[], type: ModuleConfigEnum) {
  return CDR.post({
    url: type === ModuleConfigEnum.CLUE_MANAGEMENT ? SaveClueCapacityUrl : SaveCustomerCapacityUrl,
    data,
  });
}

// 公海相关API
export function getCustomerPoolPage(data: TableQueryParams) {
  return CDR.post<CommonList<CluePoolItem>>({ url: GetCustomerPoolPageUrl, data });
}

export function addCustomerPool(data: CluePoolParams) {
  return CDR.post({ url: AddCustomerPoolUrl, data });
}

export function updateCustomerPool(data: CluePoolParams) {
  return CDR.post({ url: UpdateCustomerPoolUrl, data });
}

export function switchCustomerPoolStatus(id: string) {
  return CDR.get({ url: `${SwitchCustomerPoolStatusUrl}/${id}` });
}

export function deleteCustomerPool(id: string) {
  return CDR.get({ url: `${DeleteCustomerPoolUrl}/${id}` });
}

export function noPickCustomerPool(id: string) {
  return CDR.get({ url: `${NoPickCustomerPoolUrl}/${id}` });
}

// 表单设计
export function saveFormDesignConfig(data: SaveFormDesignConfigParams) {
  return CDR.post({ url: SaveFormDesignConfigUrl, data });
}

export function getFormDesignConfig(id: string) {
  return CDR.get<FormDesignConfigDetailParams>({ url: `${GetFormDesignConfigUrl}/${id}` });
}
