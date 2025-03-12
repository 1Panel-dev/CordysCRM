import {
  AddCustomerContactUrl,
  AddCustomerFollowPlanUrl,
  AddCustomerFollowRecordUrl,
  AddCustomerOpenSeaUrl,
  AddCustomerUrl,
  DeleteCustomerContactUrl,
  DeleteCustomerOpenSeaUrl,
  DeleteCustomerUrl,
  DisableCustomerContactUrl,
  EnableCustomerContactUrl,
  GetCustomerContactFormConfigUrl,
  GetCustomerContactListUrl,
  GetCustomerContactUrl,
  GetCustomerFollowPlanFormConfigUrl,
  GetCustomerFollowPlanListUrl,
  GetCustomerFollowPlanUrl,
  GetCustomerFollowRecordFormConfigUrl,
  GetCustomerFollowRecordListUrl,
  GetCustomerFollowRecordUrl,
  GetCustomerFormConfigUrl,
  GetCustomerListUrl,
  GetCustomerOpenSeaListUrl,
  GetCustomerUrl,
  IsCustomerOpenSeaNoPickUrl,
  SwitchCustomerOpenSeaUrl,
  UpdateCustomerContactUrl,
  UpdateCustomerFollowPlanUrl,
  UpdateCustomerFollowRecordUrl,
  UpdateCustomerOpenSeaUrl,
  UpdateCustomerUrl,
} from '@lib/shared/api/requrls/customer';
import type { CommonList, TableQueryParams } from '@lib/shared/models/common';
import type {
  CustomerContractListItem,
  CustomerContractTableParams,
  CustomerDetail,
  CustomerFollowPlanListItem,
  CustomerFollowPlanTableParams,
  CustomerFollowRecordListItem,
  CustomerFollowRecordTableParams,
  CustomerListItem,
  CustomerOpenSeaListItem,
  CustomerTableParams,
  FollowDetailItem,
  SaveCustomerContractParams,
  SaveCustomerFollowPlanParams,
  SaveCustomerFollowRecordParams,
  SaveCustomerOpenSeaParams,
  SaveCustomerParams,
  UpdateCustomerContractParams,
  UpdateCustomerFollowPlanParams,
  UpdateCustomerFollowRecordParams,
  UpdateCustomerOpenSeaParams,
  UpdateCustomerParams,
} from '@lib/shared/models/customer';
import type { FormDesignConfigDetailParams } from '@lib/shared/models/system/module';

import CDR from '@/api/http/index';

// 添加客户
export function addCustomer(data: SaveCustomerParams) {
  return CDR.post({ url: AddCustomerUrl, data });
}

// 更新客户
export function updateCustomer(data: UpdateCustomerParams) {
  return CDR.post({ url: UpdateCustomerUrl, data });
}

// 获取客户列表
export function getCustomerList(data: CustomerTableParams) {
  return CDR.post<CommonList<CustomerListItem>>({ url: GetCustomerListUrl, data });
}

// 获取客户表单配置
export function getCustomerFormConfig() {
  return CDR.get<FormDesignConfigDetailParams>({ url: GetCustomerFormConfigUrl });
}

// 获取客户详情
export function getCustomer(id: string) {
  return CDR.get<CustomerDetail>({ url: `${GetCustomerUrl}/${id}` });
}

// 删除客户
export function deleteCustomer(id: string) {
  return CDR.get({ url: `${DeleteCustomerUrl}/${id}` });
}

// 添加客户跟进记录
export function addCustomerFollowRecord(data: SaveCustomerFollowRecordParams) {
  return CDR.post({ url: AddCustomerFollowRecordUrl, data });
}

// 更新客户跟进记录
export function updateCustomerFollowRecord(data: UpdateCustomerFollowRecordParams) {
  return CDR.post({ url: UpdateCustomerFollowRecordUrl, data });
}

// 获取客户跟进记录列表
export function getCustomerFollowRecordList(data: CustomerFollowRecordTableParams) {
  return CDR.post<CommonList<CustomerFollowRecordListItem>>({ url: GetCustomerFollowRecordListUrl, data });
}

// 获取客户跟进记录表单配置
export function getCustomerFollowRecordFormConfig() {
  return CDR.get<FormDesignConfigDetailParams>({ url: GetCustomerFollowRecordFormConfigUrl });
}

// 获取客户跟进记录详情
export function getCustomerFollowRecord(id: string) {
  return CDR.get<CustomerFollowRecordListItem>({ url: `${GetCustomerFollowRecordUrl}/${id}` });
}

// 添加客户跟进计划
export function addCustomerFollowPlan(data: SaveCustomerFollowPlanParams) {
  return CDR.post({ url: AddCustomerFollowPlanUrl, data });
}

// 更新客户跟进计划
export function updateCustomerFollowPlan(data: UpdateCustomerFollowPlanParams) {
  return CDR.post({ url: UpdateCustomerFollowPlanUrl, data });
}

// 获取客户跟进计划列表
export function getCustomerFollowPlanList(data: CustomerFollowPlanTableParams) {
  return CDR.post<CommonList<CustomerFollowPlanListItem>>({ url: GetCustomerFollowPlanListUrl, data });
}

// 获取客户跟进计划表单配置
export function getCustomerFollowPlanFormConfig() {
  return CDR.get<FormDesignConfigDetailParams>({ url: GetCustomerFollowPlanFormConfigUrl });
}

// 获取客户跟进计划详情
export function getCustomerFollowPlan(id: string) {
  return CDR.get<FollowDetailItem>({ url: `${GetCustomerFollowPlanUrl}/${id}` });
}

// 添加客户联系人
export function addCustomerContact(data: SaveCustomerContractParams) {
  return CDR.post({ url: AddCustomerContactUrl, data });
}

// 获取客户联系人列表
export function getCustomerContactList(data: CustomerContractTableParams) {
  return CDR.post<CommonList<CustomerContractListItem>>({ url: GetCustomerContactListUrl, data });
}

// 更新客户联系人
export function updateCustomerContact(data: UpdateCustomerContractParams) {
  return CDR.post({ url: UpdateCustomerContactUrl, data });
}

// 禁用客户联系人
export function disableCustomerContact(id: string, reason: string) {
  return CDR.post({ url: `${DisableCustomerContactUrl}/${id}`, data: { reason } });
}

// 获取客户联系人表单配置
export function getCustomerContactFormConfig() {
  return CDR.get<FormDesignConfigDetailParams>({ url: GetCustomerContactFormConfigUrl });
}

// 获取客户联系人详情
export function getCustomerContact(id: string) {
  return CDR.get<CustomerContractListItem>({ url: `${GetCustomerContactUrl}/${id}` });
}

// 启用客户联系人
export function enableCustomerContact(id: string) {
  return CDR.get({ url: `${EnableCustomerContactUrl}/${id}` });
}

// 删除客户联系人
export function deleteCustomerContact(id: string) {
  return CDR.get({ url: `${DeleteCustomerContactUrl}/${id}` });
}

// 添加公海
export function addCustomerOpenSea(data: SaveCustomerOpenSeaParams) {
  return CDR.post({ url: AddCustomerOpenSeaUrl, data });
}

// 更新公海
export function updateCustomerOpenSea(data: UpdateCustomerOpenSeaParams) {
  return CDR.post({ url: UpdateCustomerOpenSeaUrl, data });
}

// 获取公海列表
export function getCustomerOpenSeaList(data: TableQueryParams) {
  return CDR.post<CommonList<CustomerOpenSeaListItem>>({ url: GetCustomerOpenSeaListUrl, data });
}

// 启用/禁用公海
export function switchCustomerOpenSea(id: string) {
  return CDR.get({ url: `${SwitchCustomerOpenSeaUrl}/${id}` });
}

// 删除公海
export function deleteCustomerOpenSea(id: string) {
  return CDR.get({ url: `${DeleteCustomerOpenSeaUrl}/${id}` });
}

// 公海是否存在未领取线索
export function isCustomerOpenSeaNoPick(id: string) {
  return CDR.get<boolean>({ url: `${IsCustomerOpenSeaNoPickUrl}/${id}` });
}
