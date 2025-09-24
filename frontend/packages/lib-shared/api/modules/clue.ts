import type { CordysAxios } from '@lib/shared/api/http/Axios';
import {
  AddClueFollowPlanUrl,
  AddClueFollowRecordUrl,
  AddClueUrl,
  AddClueViewUrl,
  AssignClueUrl,
  BatchAssignClueUrl,
  BatchDeleteCluePoolUrl,
  BatchDeleteClueUrl,
  BatchPickClueUrl,
  BatchToPoolClueUrl,
  BatchTransferClueUrl,
  BatchUpdateCluePoolUrl,
  CancelClueFollowPlanUrl,
  ClueTransitionCustomerUrl,
  DeleteClueFollowPlanUrl,
  DeleteClueFollowRecordUrl,
  DeleteCluePoolUrl,
  DeleteClueUrl,
  DeleteClueViewUrl,
  DownloadTemplateUrl,
  DragClueViewUrl,
  EnableClueViewUrl,
  ExportClueAllUrl,
  ExportCluePoolAllUrl,
  ExportCluePoolSelectedUrl,
  ExportClueSelectedUrl,
  FixedClueViewUrl,
  GetAdvancedCluePoolListUrl,
  GetAdvancedSearchClueDetailUrl,
  GetAdvancedSearchClueListUrl,
  GetClueFollowPlanListUrl,
  GetClueFollowPlanUrl,
  GetClueFollowRecordListUrl,
  GetClueFollowRecordUrl,
  GetClueFormConfigUrl,
  GetClueHeaderListUrl,
  GetClueListUrl,
  GetCluePoolFollowRecordListUrl,
  GetCluePoolListUrl,
  GetClueTabUrl,
  GetClueTransitionCustomerListUrl,
  GetClueUrl,
  GetClueViewDetailUrl,
  GetClueViewListUrl,
  GetGlobalCluePoolListUrl,
  GetGlobalSearchClueListUrl,
  GetPoolClueUrl,
  GetPoolOptionsUrl,
  ImportLeadUrl,
  MoveToPoolLeadUrl,
  PickClueUrl,
  PreCheckImportUrl,
  ReTransitionCustomerUrl,
  TransformClueUrl,
  UpdateClueFollowPlanStatusUrl,
  UpdateClueFollowPlanUrl,
  UpdateClueFollowRecordUrl,
  UpdateClueStatusUrl,
  UpdateClueUrl,
  UpdateClueViewUrl,
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
  ConvertClueParams,
  PickClueParams,
  SaveClueParams,
  UpdateClueParams,
} from '@lib/shared/models/clue';
import type {
  CommonList,
  TableDraggedParams,
  TableExportParams,
  TableExportSelectedParams,
} from '@lib/shared/models/common';
import type {
  BatchMoveToPublicPoolParams,
  BatchUpdatePoolAccountParams,
  CustomerContractTableParams,
  CustomerFollowPlanTableParams,
  CustomerFollowRecordTableParams,
  CustomerTabHidden,
  CustomerTableParams,
  FollowDetailItem,
  MoveToPublicPoolParams,
  PoolTableExportParams,
  SaveCustomerFollowPlanParams,
  SaveCustomerFollowRecordParams,
  TransferParams,
  UpdateCustomerFollowPlanParams,
  UpdateCustomerFollowRecordParams,
  UpdateFollowPlanStatusParams,
} from '@lib/shared/models/customer';
import type { CluePoolItem, FormDesignConfigDetailParams } from '@lib/shared/models/system/module';
import { ValidateInfo } from '@lib/shared/models/system/org';
import type { ViewItem, ViewParams } from '@lib/shared/models/view';

export default function useProductApi(CDR: CordysAxios) {
  // 添加线索
  function addClue(data: SaveClueParams) {
    return CDR.post({ url: AddClueUrl, data });
  }

  // 更新线索
  function updateClue(data: UpdateClueParams) {
    return CDR.post({ url: UpdateClueUrl, data });
  }

  // 更新线索状态
  function updateClueStatus(data: { id: string; stage: string }) {
    return CDR.post({ url: UpdateClueStatusUrl, data });
  }

  // 获取线索列表
  function getClueList(data: CustomerTableParams) {
    return CDR.post<CommonList<ClueListItem>>({ url: GetClueListUrl, data });
  }

  // 获取线索转为客户列表
  function getClueTransitionCustomerList(data: CustomerTableParams) {
    return CDR.post<CommonList<ClueListItem>>({ url: GetClueTransitionCustomerListUrl, data });
  }

  // 批量转移线索
  function batchTransferClue(data: TransferParams) {
    return CDR.post({ url: BatchTransferClueUrl, data });
  }

  // 线索合并客户
  function reTransitionCustomer(data: { clueIds: (string | number)[]; customerId: string }) {
    return CDR.post({ url: ReTransitionCustomerUrl, data });
  }

  // 批量移入线索池
  function batchToCluePool(data: BatchMoveToPublicPoolParams) {
    return CDR.post({ url: BatchToPoolClueUrl, data });
  }

  // 移入线索池
  function moveToLeadPool(data: MoveToPublicPoolParams) {
    return CDR.post({ url: MoveToPoolLeadUrl, data });
  }

  // 导出全量线索池列表
  function exportCluePoolAll(data: PoolTableExportParams) {
    return CDR.post({ url: ExportCluePoolAllUrl, data });
  }

  // 导出选中线索池列表
  function exportCluePoolSelected(data: TableExportSelectedParams) {
    return CDR.post({ url: ExportCluePoolSelectedUrl, data });
  }

  // 批量删除线索
  function batchDeleteClue(data: string[]) {
    return CDR.post({ url: BatchDeleteClueUrl, data });
  }

  // 获取线索表单配置
  function getClueFormConfig() {
    return CDR.get<FormDesignConfigDetailParams>({ url: GetClueFormConfigUrl });
  }

  // 获取线索详情
  function getClue(id: string) {
    return CDR.get<ClueDetail>({ url: `${GetClueUrl}/${id}` });
  }

  // 删除线索
  function deleteClue(id: string) {
    return CDR.get({ url: `${DeleteClueUrl}/${id}` });
  }

  // 转为客户
  function ClueTransitionCustomer(data: ClueTransitionCustomerParams) {
    return CDR.post({ url: ClueTransitionCustomerUrl, data });
  }

  // 添加线索跟进记录
  function addClueFollowRecord(data: SaveCustomerFollowRecordParams) {
    return CDR.post({ url: AddClueFollowRecordUrl, data });
  }

  // 更新线索跟进记录
  function updateClueFollowRecord(data: UpdateCustomerFollowRecordParams) {
    return CDR.post({ url: UpdateClueFollowRecordUrl, data });
  }

  // 获取线索跟进记录列表
  function getClueFollowRecordList(data: CustomerFollowRecordTableParams) {
    return CDR.post<CommonList<FollowDetailItem>>({ url: GetClueFollowRecordListUrl, data });
  }

  // 删除线索跟进记录
  function deleteClueFollowRecord(id: string) {
    return CDR.get({ url: `${DeleteClueFollowRecordUrl}/${id}` });
  }

  // 获取线索跟进记录详情
  function getClueFollowRecord(id: string) {
    return CDR.get<FollowDetailItem>({ url: `${GetClueFollowRecordUrl}/${id}` });
  }

  // 添加线索跟进计划
  function addClueFollowPlan(data: SaveCustomerFollowPlanParams) {
    return CDR.post({ url: AddClueFollowPlanUrl, data });
  }

  // 更新线索跟进计划
  function updateClueFollowPlan(data: UpdateCustomerFollowPlanParams) {
    return CDR.post({ url: UpdateClueFollowPlanUrl, data });
  }

  // 删除线索跟进计划
  function deleteClueFollowPlan(id: string) {
    return CDR.get({ url: `${DeleteClueFollowPlanUrl}/${id}` });
  }

  // 获取线索跟进计划列表
  function getClueFollowPlanList(data: CustomerFollowPlanTableParams) {
    return CDR.post<CommonList<FollowDetailItem>>({ url: GetClueFollowPlanListUrl, data });
  }

  // 获取线索跟进计划详情
  function getClueFollowPlan(id: string) {
    return CDR.get<FollowDetailItem>({ url: `${GetClueFollowPlanUrl}/${id}` });
  }

  // 取消跟进计划
  function cancelClueFollowPlan(id: string) {
    return CDR.get({ url: `${CancelClueFollowPlanUrl}/${id}` });
  }

  // 获取线索负责人列表
  function getClueHeaderList(data: CustomerContractTableParams) {
    return CDR.get({ url: `${GetClueHeaderListUrl}/${data.sourceId}` });
  }

  // 线索池领取线索
  function pickClue(data: PickClueParams) {
    return CDR.post({ url: PickClueUrl, data });
  }

  // 获取线索池线索列表
  function getCluePoolList(data: CluePoolTableParams) {
    return CDR.post<CommonList<CluePoolListItem>>({ url: GetCluePoolListUrl, data });
  }

  // 批量领取线索池线索
  function batchPickClue(data: BatchPickClueParams) {
    return CDR.post({ url: BatchPickClueUrl, data });
  }

  // 批量删除线索池线索
  function batchDeleteCluePool(data: string[]) {
    return CDR.post({ url: BatchDeleteCluePoolUrl, data });
  }

  // 批量分配线索池线索
  function batchAssignClue(data: BatchAssignClueParams) {
    return CDR.post({ url: BatchAssignClueUrl, data });
  }

  // 批量更新线索池线索
  function batchUpdateCluePool(data: BatchUpdatePoolAccountParams) {
    return CDR.post({ url: BatchUpdateCluePoolUrl, data });
  }

  // 分配线索池线索
  function assignClue(data: AssignClueParams) {
    return CDR.post({ url: AssignClueUrl, data });
  }

  // 获取当前用户线索池选项
  function getPoolOptions() {
    return CDR.get<CluePoolItem[]>({ url: GetPoolOptionsUrl });
  }

  // 删除线索池线索
  function deleteCluePool(id: string) {
    return CDR.get({ url: `${DeleteCluePoolUrl}/${id}` });
  }

  // 获取线索池跟进记录列表
  function getCluePoolFollowRecordList(data: CustomerFollowRecordTableParams) {
    return CDR.post<CommonList<FollowDetailItem>>({ url: GetCluePoolFollowRecordListUrl, data });
  }

  // 获取线索池详情
  function getPoolClue(id: string) {
    return CDR.get<ClueDetail>({ url: `${GetPoolClueUrl}/${id}` });
  }

  // 获取线索tab显隐藏
  function getClueTab() {
    return CDR.get<CustomerTabHidden>({ url: GetClueTabUrl });
  }

  // 更新线索跟进计划状态
  function updateClueFollowPlanStatus(data: UpdateFollowPlanStatusParams) {
    return CDR.post({ url: UpdateClueFollowPlanStatusUrl, data });
  }

  // 导出全量线索列表
  function exportClueAll(data: TableExportParams) {
    return CDR.post({ url: ExportClueAllUrl, data });
  }

  // 导出选中线索列表
  function exportClueSelected(data: TableExportSelectedParams) {
    return CDR.post({ url: ExportClueSelectedUrl, data });
  }

  // 转换线索
  function transformClue(data: ConvertClueParams) {
    return CDR.post({ url: TransformClueUrl, data });
  }

  // 视图
  function addClueView(data: ViewParams) {
    return CDR.post({ url: AddClueViewUrl, data });
  }

  function updateClueView(data: ViewParams) {
    return CDR.post({ url: UpdateClueViewUrl, data });
  }

  function getClueViewList() {
    return CDR.get<ViewItem[]>({ url: GetClueViewListUrl });
  }

  function getClueViewDetail(id: string) {
    return CDR.get({ url: `${GetClueViewDetailUrl}/${id}` });
  }

  function fixedClueView(id: string) {
    return CDR.get({ url: `${FixedClueViewUrl}/${id}` });
  }

  function enableClueView(id: string) {
    return CDR.get({ url: `${EnableClueViewUrl}/${id}` });
  }

  function deleteClueView(id: string) {
    return CDR.get({ url: `${DeleteClueViewUrl}/${id}` });
  }

  function dragClueView(data: TableDraggedParams) {
    return CDR.post({ url: DragClueViewUrl, data });
  }

  function preCheckImportLead(file: File) {
    return CDR.uploadFile<{ data: ValidateInfo }>({ url: PreCheckImportUrl }, { fileList: [file] }, 'file');
  }

  function downloadLeadTemplate() {
    return CDR.get(
      {
        url: DownloadTemplateUrl,
        responseType: 'blob',
      },
      { isTransformResponse: false, isReturnNativeResponse: true }
    );
  }

  function importLead(file: File) {
    return CDR.uploadFile({ url: ImportLeadUrl }, { fileList: [file] }, 'file');
  }

  function getAdvancedSearchClueList(data: CustomerTableParams) {
    return CDR.post<CommonList<ClueListItem>>({ url: GetAdvancedSearchClueListUrl, data }, { ignoreCancelToken: true });
  }

  function getAdvancedSearchClueDetail(data: CustomerTableParams) {
    return CDR.post<CommonList<ClueListItem>>({ url: GetAdvancedSearchClueDetailUrl, data });
  }

  function getAdvancedCluePoolList(data: CluePoolTableParams) {
    return CDR.post<CommonList<CluePoolListItem>>(
      { url: GetAdvancedCluePoolListUrl, data },
      { ignoreCancelToken: true }
    );
  }

  function getGlobalSearchClueList(data: CustomerTableParams) {
    return CDR.post<CommonList<ClueListItem>>({ url: GetGlobalSearchClueListUrl, data }, { ignoreCancelToken: true });
  }

  function getGlobalCluePoolList(data: CluePoolTableParams) {
    return CDR.post<CommonList<CluePoolListItem>>({ url: GetGlobalCluePoolListUrl, data }, { ignoreCancelToken: true });
  }

  return {
    addClue,
    updateClue,
    updateClueStatus,
    getClueList,
    batchTransferClue,
    batchToCluePool,
    batchDeleteClue,
    getClueFormConfig,
    getClue,
    deleteClue,
    ClueTransitionCustomer,
    addClueFollowRecord,
    updateClueFollowRecord,
    getClueFollowRecordList,
    deleteClueFollowRecord,
    getClueFollowRecord,
    addClueFollowPlan,
    updateClueFollowPlan,
    deleteClueFollowPlan,
    getClueFollowPlanList,
    getClueFollowPlan,
    cancelClueFollowPlan,
    getClueHeaderList,
    pickClue,
    getCluePoolList,
    batchPickClue,
    batchDeleteCluePool,
    batchAssignClue,
    assignClue,
    getPoolOptions,
    deleteCluePool,
    getCluePoolFollowRecordList,
    getPoolClue,
    getClueTab,
    updateClueFollowPlanStatus,
    exportClueAll,
    exportClueSelected,
    getClueTransitionCustomerList,
    reTransitionCustomer,
    moveToLeadPool,
    addClueView,
    deleteClueView,
    fixedClueView,
    getClueViewDetail,
    getClueViewList,
    updateClueView,
    enableClueView,
    dragClueView,
    preCheckImportLead,
    downloadLeadTemplate,
    importLead,
    getAdvancedSearchClueList,
    getAdvancedCluePoolList,
    getAdvancedSearchClueDetail,
    getGlobalCluePoolList,
    getGlobalSearchClueList,
    exportCluePoolAll,
    exportCluePoolSelected,
    transformClue,
    batchUpdateCluePool,
  };
}
