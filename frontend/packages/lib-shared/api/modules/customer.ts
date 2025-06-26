import type { CordysAxios } from '@lib/shared/api/http/Axios';
import {
  AddCustomerCollaborationUrl,
  AddCustomerContactUrl,
  AddCustomerFollowPlanUrl,
  AddCustomerFollowRecordUrl,
  AddCustomerOpenSeaUrl,
  AddCustomerRelationItemUrl,
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
  CheckOpportunityContactUrl,
  ContactListUnderCustomerUrl,
  DeleteCustomerCollaborationUrl,
  DeleteCustomerContactUrl,
  DeleteCustomerFollowPlanUrl,
  DeleteCustomerFollowRecordUrl,
  DeleteCustomerOpenSeaUrl,
  DeleteCustomerRelationItemUrl,
  DeleteCustomerUrl,
  DeleteOpenSeaCustomerUrl,
  DisableCustomerContactUrl,
  EnableCustomerContactUrl,
  ExportCustomerAllUrl,
  ExportCustomerSelectedUrl,
  GetCustomerCollaborationListUrl,
  GetCustomerContactFormConfigUrl,
  GetCustomerContactListUrl,
  GetCustomerContactTabUrl,
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
  GetCustomerOpenSeaFollowRecordListUrl,
  GetCustomerOpenSeaListUrl,
  GetCustomerOpportunityListUrl,
  GetCustomerOptionsUrl,
  GetCustomerRelationListUrl,
  GetCustomerTabUrl,
  GetCustomerUrl,
  GetOpenSeaCustomerListUrl,
  GetOpenSeaCustomerUrl,
  GetOpenSeaOptionsUrl,
  IsCustomerOpenSeaNoPickUrl,
  PickOpenSeaCustomerUrl,
  SaveCustomerRelationUrl,
  SwitchCustomerOpenSeaUrl,
  UpdateCustomerCollaborationUrl,
  UpdateCustomerContactUrl,
  UpdateCustomerFollowPlanStatusUrl,
  UpdateCustomerFollowPlanUrl,
  UpdateCustomerFollowRecordUrl,
  UpdateCustomerOpenSeaUrl,
  UpdateCustomerRelationItemUrl,
  UpdateCustomerUrl,
} from '@lib/shared/api/requrls/customer';
import type {
  CommonList,
  TableExportParams,
  TableExportSelectedParams,
  TableQueryParams,
} from '@lib/shared/models/common';
import type {
  AddCustomerCollaborationParams,
  AddCustomerRelationItemParams,
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
  CustomerOpportunityTableParams,
  CustomerOptionsItem,
  CustomerTabHidden,
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
  UpdateCustomerRelationItemParams,
  UpdateFollowPlanStatusParams,
} from '@lib/shared/models/customer';
import type { FormDesignConfigDetailParams, OpportunityItem } from '@lib/shared/models/system/module';

export default function useProductApi(CDR: CordysAxios) {
  // 添加客户
  function addCustomer(data: SaveCustomerParams) {
    return CDR.post({ url: AddCustomerUrl, data });
  }

  // 更新客户
  function updateCustomer(data: UpdateCustomerParams) {
    return CDR.post({ url: UpdateCustomerUrl, data });
  }

  // 获取客户列表
  function getCustomerList(data: CustomerTableParams) {
    return CDR.post<CommonList<CustomerListItem>>({ url: GetCustomerListUrl, data });
  }

  // 获取客户表单配置
  function getCustomerFormConfig() {
    return CDR.get<FormDesignConfigDetailParams>({ url: GetCustomerFormConfigUrl });
  }

  // 获取客户详情
  function getCustomer(id: string) {
    return CDR.get<CustomerDetail>({ url: `${GetCustomerUrl}/${id}` });
  }

  // 删除客户
  function deleteCustomer(id: string) {
    return CDR.get({ url: `${DeleteCustomerUrl}/${id}` });
  }

  // 批量删除客户
  function batchDeleteCustomer(batchIds: (string | number)[]) {
    return CDR.post({ url: BatchDeleteCustomerUrl, data: batchIds });
  }

  // 批量转移客户
  function batchTransferCustomer(data: TransferParams) {
    return CDR.post({ url: BatchTransferCustomerUrl, data });
  }

  // 批量移入公海
  function batchMoveCustomer(data: (string | number)[]) {
    return CDR.post({ url: BatchMoveCustomerUrl, data });
  }

  // 添加客户跟进记录
  function addCustomerFollowRecord(data: SaveCustomerFollowRecordParams) {
    return CDR.post({ url: AddCustomerFollowRecordUrl, data });
  }

  // 更新客户跟进记录
  function updateCustomerFollowRecord(data: UpdateCustomerFollowRecordParams) {
    return CDR.post({ url: UpdateCustomerFollowRecordUrl, data });
  }

  // 删除客户跟进记录
  function deleteCustomerFollowRecord(id: string) {
    return CDR.get({ url: `${DeleteCustomerFollowRecordUrl}/${id}` });
  }

  // 获取客户跟进记录列表
  function getCustomerFollowRecordList(data: CustomerFollowRecordTableParams) {
    return CDR.post<CommonList<CustomerFollowRecordListItem>>({ url: GetCustomerFollowRecordListUrl, data });
  }

  // 获取客户跟进记录表单配置
  function getCustomerFollowRecordFormConfig() {
    return CDR.get<FormDesignConfigDetailParams>({ url: GetCustomerFollowRecordFormConfigUrl });
  }

  // 获取客户跟进记录详情
  function getCustomerFollowRecord(id: string) {
    return CDR.get<CustomerFollowRecordListItem>({ url: `${GetCustomerFollowRecordUrl}/${id}` });
  }

  // 添加客户跟进计划
  function addCustomerFollowPlan(data: SaveCustomerFollowPlanParams) {
    return CDR.post({ url: AddCustomerFollowPlanUrl, data });
  }

  // 更新客户跟进计划
  function updateCustomerFollowPlan(data: UpdateCustomerFollowPlanParams) {
    return CDR.post({ url: UpdateCustomerFollowPlanUrl, data });
  }

  // 删除客户跟进计划
  function deleteCustomerFollowPlan(id: string) {
    return CDR.get({ url: `${DeleteCustomerFollowPlanUrl}/${id}` });
  }

  // 获取客户跟进计划列表
  function getCustomerFollowPlanList(data: CustomerFollowPlanTableParams) {
    return CDR.post<CommonList<CustomerFollowPlanListItem>>({ url: GetCustomerFollowPlanListUrl, data });
  }

  // 取消客户跟进计划
  function cancelCustomerFollowPlan(id: string) {
    return CDR.get({ url: `${CancelCustomerFollowPlanUrl}/${id}` });
  }

  // 获取客户跟进计划表单配置
  function getCustomerFollowPlanFormConfig() {
    return CDR.get<FormDesignConfigDetailParams>({ url: GetCustomerFollowPlanFormConfigUrl });
  }

  // 获取客户跟进计划详情
  function getCustomerFollowPlan(id: string) {
    return CDR.get<FollowDetailItem>({ url: `${GetCustomerFollowPlanUrl}/${id}` });
  }

  // 添加客户联系人
  function addCustomerContact(data: SaveCustomerContractParams) {
    return CDR.post({ url: AddCustomerContactUrl, data });
  }

  // 获取客户联系人列表
  function getCustomerContactList(data: CustomerContractTableParams) {
    return CDR.post<CommonList<CustomerContractListItem>>({ url: GetCustomerContactListUrl, data });
  }

  // 更新客户联系人
  function updateCustomerContact(data: UpdateCustomerContractParams) {
    return CDR.post({ url: UpdateCustomerContactUrl, data });
  }

  // 禁用客户联系人
  function disableCustomerContact(id: string, reason: string) {
    return CDR.post({ url: `${DisableCustomerContactUrl}/${id}`, data: { reason } });
  }

  // 获取客户联系人表单配置
  function getCustomerContactFormConfig() {
    return CDR.get<FormDesignConfigDetailParams>({ url: GetCustomerContactFormConfigUrl });
  }

  // 获取客户联系人详情
  function getCustomerContact(id: string) {
    return CDR.get<CustomerContractListItem>({ url: `${GetCustomerContactUrl}/${id}` });
  }

  // 启用客户联系人
  function enableCustomerContact(id: string) {
    return CDR.get({ url: `${EnableCustomerContactUrl}/${id}` });
  }

  // 删除客户联系人
  function deleteCustomerContact(id: string) {
    return CDR.get({ url: `${DeleteCustomerContactUrl}/${id}` });
  }

  // 是否绑定商机
  function checkOpportunity(id: string) {
    return CDR.get({ url: `${CheckOpportunityContactUrl}/${id}` });
  }

  //  客户下的联系人列表
  function getContactListUnderCustomer(data: { id: string }) {
    return CDR.get({ url: `${ContactListUnderCustomerUrl}/${data.id}` });
  }

  // 添加公海
  function addCustomerOpenSea(data: SaveCustomerOpenSeaParams) {
    return CDR.post({ url: AddCustomerOpenSeaUrl, data });
  }

  // 更新公海
  function updateCustomerOpenSea(data: UpdateCustomerOpenSeaParams) {
    return CDR.post({ url: UpdateCustomerOpenSeaUrl, data });
  }

  // 获取公海列表
  function getCustomerOpenSeaList(data: TableQueryParams) {
    return CDR.post<CommonList<CustomerOpenSeaListItem>>({ url: GetCustomerOpenSeaListUrl, data });
  }

  // 启用/禁用公海
  function switchCustomerOpenSea(id: string) {
    return CDR.get({ url: `${SwitchCustomerOpenSeaUrl}/${id}` });
  }

  // 删除公海
  function deleteCustomerOpenSea(id: string) {
    return CDR.get({ url: `${DeleteCustomerOpenSeaUrl}/${id}` });
  }

  // 公海是否存在未领取线索
  function isCustomerOpenSeaNoPick(id: string) {
    return CDR.get<boolean>({ url: `${IsCustomerOpenSeaNoPickUrl}/${id}` });
  }

  // 获取公海客户列表
  function getOpenSeaCustomerList(data: OpenSeaCustomerTableParams) {
    return CDR.post<CommonList<CustomerOpenSeaListItem>>({ url: GetOpenSeaCustomerListUrl, data });
  }

  // 领取公海客户
  function pickOpenSeaCustomer(data: PickOpenSeaCustomerParams) {
    return CDR.post({ url: PickOpenSeaCustomerUrl, data });
  }

  // 批量领取公海客户
  function batchPickOpenSeaCustomer(data: BatchOperationOpenSeaCustomerParams) {
    return CDR.post({ url: BatchPickOpenSeaCustomerUrl, data });
  }

  // 批量删除公海客户
  function batchDeleteOpenSeaCustomer(data: BatchOperationOpenSeaCustomerParams) {
    return CDR.post({ url: BatchDeleteOpenSeaCustomerUrl, data });
  }

  // 批量分配公海客户
  function batchAssignOpenSeaCustomer(data: BatchAssignOpenSeaCustomerParams) {
    return CDR.post({ url: BatchAssignOpenSeaCustomerUrl, data });
  }

  // 分配公海客户
  function assignOpenSeaCustomer(data: AssignOpenSeaCustomerParams) {
    return CDR.post({ url: AssignOpenSeaCustomerUrl, data });
  }

  // 获取公海选项
  function getOpenSeaOptions() {
    return CDR.get<CustomerOptionsItem[]>({ url: GetOpenSeaOptionsUrl });
  }

  // 获取公海客户详情
  function getOpenSeaCustomer(id: string) {
    return CDR.get<CustomerDetail>({ url: `${GetOpenSeaCustomerUrl}/${id}` });
  }

  // 删除公海客户
  function deleteOpenSeaCustomer(id: string) {
    return CDR.get({ url: `${DeleteOpenSeaCustomerUrl}/${id}` });
  }

  // 获取客户负责人列表
  function getCustomerHeaderList(data: CustomerContractTableParams) {
    return CDR.get({ url: `${GetCustomerHeaderListUrl}/${data.sourceId}` });
  }

  // 保存客户关系
  function saveCustomerRelation(customerId: string, data: RelationItem[]) {
    return CDR.post({ url: `${SaveCustomerRelationUrl}/${customerId}`, data });
  }

  // 获取客户关系列表
  function getCustomerRelationList(customerId: string) {
    return CDR.get<RelationListItem[]>({ url: `${GetCustomerRelationListUrl}/${customerId}` });
  }

  // 获取客户协作成员列表
  function getCustomerCollaborationList({ customerId }: { customerId: string }) {
    return CDR.get<CollaborationItem[]>({ url: `${GetCustomerCollaborationListUrl}/${customerId}` });
  }

  // 更新单条客户关系
  function updateCustomerRelationItem(customerId: string, data: UpdateCustomerRelationItemParams) {
    return CDR.post({ url: `${UpdateCustomerRelationItemUrl}/${customerId}`, data });
  }

  // 添加单条客户关系
  function addCustomerRelationItem(customerId: string, data: AddCustomerRelationItemParams) {
    return CDR.post({ url: `${AddCustomerRelationItemUrl}/${customerId}`, data });
  }

  // 删除单条客户关系
  function deleteCustomerRelationItem(id: string) {
    return CDR.get({ url: `${DeleteCustomerRelationItemUrl}/${id}` });
  }

  // 批量删除客户协作成员
  function batchDeleteCustomerCollaboration(data: string[]) {
    return CDR.post({ url: BatchDeleteCustomerCollaborationUrl, data });
  }

  // 更新客户协作成员
  function updateCustomerCollaboration(data: UpdateCustomerCollaborationParams) {
    return CDR.post({ url: UpdateCustomerCollaborationUrl, data });
  }

  // 添加客户协作成员
  function addCustomerCollaboration(data: AddCustomerCollaborationParams) {
    return CDR.post({ url: AddCustomerCollaborationUrl, data });
  }

  // 删除客户协作成员
  function deleteCustomerCollaboration(id: string) {
    return CDR.get({ url: `${DeleteCustomerCollaborationUrl}/${id}` });
  }

  // 获取客户选项列表
  function getCustomerOptions(data: TableQueryParams) {
    return CDR.post<CommonList<CustomerOptionsItem>>({ url: GetCustomerOptionsUrl, data });
  }

  // 获取客户公海跟进记录列表
  function getCustomerOpenSeaFollowRecordList(data: CustomerFollowRecordTableParams) {
    return CDR.post<CommonList<CustomerFollowRecordListItem>>({ url: GetCustomerOpenSeaFollowRecordListUrl, data });
  }

  // 获取客户tab显隐藏
  function getCustomerTab() {
    return CDR.get<CustomerTabHidden>({ url: GetCustomerTabUrl });
  }

  // 获取客户联系人tab显隐藏
  function getCustomerContactTab() {
    return CDR.get<CustomerTabHidden>({ url: GetCustomerContactTabUrl });
  }

  // 更新客户跟进计划状态
  function updateCustomerFollowPlanStatus(data: UpdateFollowPlanStatusParams) {
    return CDR.post({ url: UpdateCustomerFollowPlanStatusUrl, data });
  }

  // 获取客户商机列表
  function getCustomerOpportunityPage(data: CustomerOpportunityTableParams) {
    return CDR.post<CommonList<OpportunityItem>>({ url: GetCustomerOpportunityListUrl, data });
  }

  // 导出全量客户列表
  function exportCustomerAll(data: TableExportParams) {
    return CDR.post({ url: ExportCustomerAllUrl, data });
  }

  // 导出选中客户列表
  function exportCustomerSelected(data: TableExportSelectedParams) {
    return CDR.post({ url: ExportCustomerSelectedUrl, data });
  }

  return {
    addCustomer,
    updateCustomer,
    getCustomerList,
    getCustomerContactTab,
    getCustomerFormConfig,
    getCustomer,
    deleteCustomer,
    batchDeleteCustomer,
    batchTransferCustomer,
    batchMoveCustomer,
    addCustomerFollowRecord,
    updateCustomerFollowRecord,
    deleteCustomerFollowRecord,
    getCustomerFollowRecordList,
    getCustomerFollowRecordFormConfig,
    getCustomerFollowRecord,
    addCustomerFollowPlan,
    updateCustomerFollowPlan,
    deleteCustomerFollowPlan,
    getCustomerFollowPlanList,
    cancelCustomerFollowPlan,
    getCustomerFollowPlanFormConfig,
    getCustomerFollowPlan,
    addCustomerContact,
    getCustomerContactList,
    updateCustomerContact,
    disableCustomerContact,
    getCustomerContactFormConfig,
    getCustomerContact,
    enableCustomerContact,
    deleteCustomerContact,
    checkOpportunity,
    getContactListUnderCustomer,
    addCustomerOpenSea,
    updateCustomerOpenSea,
    getCustomerOpenSeaList,
    switchCustomerOpenSea,
    deleteCustomerOpenSea,
    isCustomerOpenSeaNoPick,
    getOpenSeaCustomerList,
    getCustomerOpportunityPage,
    pickOpenSeaCustomer,
    batchPickOpenSeaCustomer,
    batchDeleteOpenSeaCustomer,
    batchAssignOpenSeaCustomer,
    assignOpenSeaCustomer,
    getOpenSeaOptions,
    getOpenSeaCustomer,
    deleteOpenSeaCustomer,
    getCustomerHeaderList,
    saveCustomerRelation,
    getCustomerRelationList,
    getCustomerCollaborationList,
    batchDeleteCustomerCollaboration,
    updateCustomerCollaboration,
    addCustomerCollaboration,
    deleteCustomerCollaboration,
    getCustomerOptions,
    getCustomerOpenSeaFollowRecordList,
    updateCustomerRelationItem,
    addCustomerRelationItem,
    deleteCustomerRelationItem,
    getCustomerTab,
    updateCustomerFollowPlanStatus,
    exportCustomerAll,
    exportCustomerSelected,
  };
}
