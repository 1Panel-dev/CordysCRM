import CDR from '@/api/http/index';

import {
  addOpportunityRuleUrl,
  deleteOpportunityUrl,
  getModuleNavConfigListUrl,
  getOpportunityListUrl,
  moduleNavListSortUrl,
  switchOpportunityStatusUrl,
  toggleModuleNavStatusUrl,
  updateOpportunityRuleUrl,
} from '@lib/shared/api/requrls/system/module';
import type { ModuleNavBaseInfoItem, ModuleSortParams } from '@lib/shared/models/system/module';

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

// 模块-商机-商机规则列表 TODO 类型
export function getOpportunityList(data: any) {
  return CDR.post({ url: getOpportunityListUrl, data });
}

// 模块-商机-添加商机规则  TODO 类型
export function addOpportunityRule(data: any) {
  return CDR.post({ url: addOpportunityRuleUrl, data });
}

// 模块-商机-更新商机规则 TODO 类型
export function updateOpportunityRule(data: any) {
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
