import {
  AddCustomerCollaborationUrl,
  AddCustomerContactUrl,
  AddCustomerFollowPlanUrl,
  AddCustomerFollowRecordUrl,
  AddCustomerOpenSeaUrl,
  AddCustomerUrl,
  AssignOpenSeaCustomerUrl,
  BatchAssignOpenSeaCustomerUrl,
  BatchDeleteCustomerCollaborationUrl,
  BatchDeleteCustomerUrl,
  BatchDeleteOpenSeaCustomerUrl,
  BatchMoveCustomerUrl,
  BatchPickOpenSeaCustomerUrl,
  BatchTransferCustomerUrl,
  CancelCustomerFollowPlanUrl,
  DeleteCustomerCollaborationUrl,
  DeleteCustomerContactUrl,
  DeleteCustomerOpenSeaUrl,
  DeleteCustomerUrl,
  DeleteOpenSeaCustomerUrl,
  DisableCustomerContactUrl,
  EnableCustomerContactUrl,
  GetCustomerCollaborationListUrl,
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
  GetCustomerHeaderListUrl,
  GetCustomerListUrl,
  GetCustomerOpenSeaListUrl,
  GetCustomerRelationListUrl,
  GetCustomerUrl,
  GetOpenSeaCustomerListUrl,
  GetOpenSeaOptionsUrl,
  IsCustomerOpenSeaNoPickUrl,
  PickOpenSeaCustomerUrl,
  SaveCustomerRelationUrl,
  SwitchCustomerOpenSeaUrl,
  UpdateCustomerCollaborationUrl,
  UpdateCustomerContactUrl,
  UpdateCustomerFollowPlanUrl,
  UpdateCustomerFollowRecordUrl,
  UpdateCustomerOpenSeaUrl,
  UpdateCustomerUrl,
} from '@lib/shared/api/requrls/customer';
import type { CommonList, TableQueryParams } from '@lib/shared/models/common';
import type {
  AddCustomerCollaborationParams,
  AssignOpenSeaCustomerParams,
  BatchAssignOpenSeaCustomerParams,
  BatchOperationOpenSeaCustomerParams,
  CollaborationItem,
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
  OpenSeaCustomerTableParams,
  PickOpenSeaCustomerParams,
  RelationItem,
  RelationListItem,
  SaveCustomerContractParams,
  SaveCustomerFollowPlanParams,
  SaveCustomerFollowRecordParams,
  SaveCustomerOpenSeaParams,
  SaveCustomerParams,
  TransferParams,
  UpdateCustomerCollaborationParams,
  UpdateCustomerContractParams,
  UpdateCustomerFollowPlanParams,
  UpdateCustomerFollowRecordParams,
  UpdateCustomerOpenSeaParams,
  UpdateCustomerParams,
} from '@lib/shared/models/customer';
import type { CluePoolParams, FormDesignConfigDetailParams } from '@lib/shared/models/system/module';

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

// 批量删除客户
export function batchDeleteCustomer(batchIds: (string | number)[]) {
  return CDR.post({ url: BatchDeleteCustomerUrl, data: batchIds });
}

// 批量转移客户
export function batchTransferCustomer(data: TransferParams) {
  return CDR.post({ url: BatchTransferCustomerUrl, data });
}

// 批量移入公海
export function batchMoveCustomer(data: (string | number)[]) {
  return CDR.post({ url: BatchMoveCustomerUrl, data });
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

// 取消客户跟进计划
export function cancelCustomerFollowPlan(id: string) {
  return CDR.get({ url: `${CancelCustomerFollowPlanUrl}/${id}` });
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

// 获取公海客户列表
export function getOpenSeaCustomerList(data: OpenSeaCustomerTableParams) {
  return CDR.post<CommonList<CustomerOpenSeaListItem>>({ url: GetOpenSeaCustomerListUrl, data });
}

// 领取公海客户
export function pickOpenSeaCustomer(data: PickOpenSeaCustomerParams) {
  return CDR.post({ url: PickOpenSeaCustomerUrl, data });
}

// 批量领取公海客户
export function batchPickOpenSeaCustomer(data: BatchOperationOpenSeaCustomerParams) {
  return CDR.post({ url: BatchPickOpenSeaCustomerUrl, data });
}

// 批量删除公海客户
export function batchDeleteOpenSeaCustomer(data: BatchOperationOpenSeaCustomerParams) {
  return CDR.post({ url: BatchDeleteOpenSeaCustomerUrl, data });
}

// 批量分配公海客户
export function batchAssignOpenSeaCustomer(data: BatchAssignOpenSeaCustomerParams) {
  return CDR.post({ url: BatchAssignOpenSeaCustomerUrl, data });
}

// 分配公海客户
export function assignOpenSeaCustomer(data: AssignOpenSeaCustomerParams) {
  return CDR.post({ url: AssignOpenSeaCustomerUrl, data });
}

// 获取公海选项
export function getOpenSeaOptions() {
  return CDR.get<CluePoolParams[]>({ url: GetOpenSeaOptionsUrl });
}

// 删除公海客户
export function deleteOpenSeaCustomer(id: string) {
  return CDR.get({ url: `${DeleteOpenSeaCustomerUrl}/${id}` });
}

// 获取客户负责人列表
export function getCustomerHeaderList(data: CustomerContractTableParams) {
  return CDR.get({ url: `${GetCustomerHeaderListUrl}/${data.sourceId}` });
}

// 保存客户关系
export function saveCustomerRelation(customerId: string, data: RelationItem[]) {
  return CDR.post({ url: `${SaveCustomerRelationUrl}/${customerId}`, data });
}

// 获取客户关系列表
export function getCustomerRelationList() {
  return CDR.get<RelationListItem[]>({ url: GetCustomerRelationListUrl });
}

// 获取客户协作成员列表
export function getCustomerCollaborationList(customerId: string) {
  return CDR.get<CollaborationItem[]>({ url: `${GetCustomerCollaborationListUrl}/${customerId}` });
}

// 批量删除客户协作成员
export function batchDeleteCustomerCollaboration(data: string[]) {
  return CDR.post({ url: BatchDeleteCustomerCollaborationUrl, data });
}

// 更新客户协作成员
export function updateCustomerCollaboration(data: UpdateCustomerCollaborationParams) {
  return CDR.post({ url: UpdateCustomerCollaborationUrl, data });
}

// 添加客户协作成员
export function addCustomerCollaboration(data: AddCustomerCollaborationParams) {
  return CDR.post({ url: AddCustomerCollaborationUrl, data });
}

// 删除客户协作成员
export function deleteCustomerCollaboration(id: string) {
  return CDR.get({ url: `${DeleteCustomerCollaborationUrl}/${id}` });
}
