import type { CordysAxios } from '@lib/shared/api/http/Axios';
import {
  AddBusinessViewUrl,
  AddOpportunityStageUrl,
  AddOptFollowPlanUrl,
  AddOptFollowRecordUrl,
  AdvancedSearchOptDetailUrl,
  AdvancedSearchOptPageUrl,
  BatchUpdateOpportunityUrl,
  CancelOptFollowPlanUrl,
  DeleteBusinessViewUrl,
  DeleteOpportunityStageUrl,
  DeleteOptFollowPlanUrl,
  DeleteOptFollowRecordUrl,
  DownloadOptTemplateUrl,
  DragBusinessViewUrl,
  EnableBusinessViewUrl,
  ExportOpportunityAllUrl,
  ExportOpportunitySelectedUrl,
  FixedBusinessViewUrl,
  GenerateOpportunityChartUrl,
  GetBusinessViewDetailUrl,
  GetBusinessViewListUrl,
  GetOpportunityContactListUrl,
  GetOpportunityStageConfigUrl,
  GetOptDetailUrl,
  GetOptFollowPlanUrl,
  GetOptFollowRecordUrl,
  GetOptFormConfigUrl,
  GetOptStatisticUrl,
  GetOptTabUrl,
  GetQuotationTabUrl,
  GlobalSearchOptPageUrl,
  ImportOpportunityUrl,
  OptAddUrl,
  OptBatchDeleteUrl,
  OptBatchTransferUrl,
  OptDeleteUrl,
  OptFollowPlanPageUrl,
  OptFollowRecordListUrl,
  OptPageUrl,
  OptUpdateStageUrl,
  OptUpdateUrl,
  PreCheckOptImportUrl,
  SortOpportunityStageUrl,
  SortOpportunityUrl,
  UpdateBusinessViewUrl,
  UpdateOpportunityStageRollbackUrl,
  UpdateOpportunityStageUrl,
  UpdateOptFollowPlanStatusUrl,
  UpdateOptFollowPlanUrl,
  UpdateOptFollowRecordUrl,
  GetQuotationViewListUrl,
  AddQuotationViewUrl,
  UpdateQuotationViewUrl,
  DeleteQuotationViewUrl,
  FixedQuotationViewUrl,
  EnableQuotationViewUrl,
  DragQuotationViewUrl,
  GetQuotationViewDetailUrl,
  QuotationPageUrl,
  AddQuotationUrl,
  UpdateQuotationUrl,
  GetQuotationDetailUrl,
  GetQuotationFormConfigUrl,
  ApprovalQuotationUrl,
  VoidQuotationUrl,
  DeleteQuotationUrl,
  RevokeQuotationUrl,
  BatchApproveUrl,
  BatchVoidedUrl,
  GetQuotationSnapshotFormConfigUrl,
} from '@lib/shared/api/requrls/opportunity';
import type {
  ChartResponseDataItem,
  CommonList,
  GenerateChartParams,
  TableDraggedParams,
  TableExportParams,
  TableExportSelectedParams,
  TableQueryParams,
} from '@lib/shared/models/common';
import type {
  BatchUpdatePoolAccountParams,
  CustomerContractTableParams,
  CustomerFollowPlanTableParams,
  CustomerFollowRecordTableParams,
  CustomerTabHidden,
  FollowDetailItem,
  SaveCustomerFollowPlanParams,
  SaveCustomerFollowRecordParams,
  TransferParams,
  UpdateCustomerFollowPlanParams,
  UpdateCustomerFollowRecordParams,
  UpdateFollowPlanStatusParams,
} from '@lib/shared/models/customer';
import type {
  AddOpportunityStageParams,
  ApproveQuotation,
  BatchApproveParams,
  OpportunityBillboardDraggedParams,
  OpportunityDetail,
  OpportunityItem,
  OpportunityPageQueryParams,
  OpportunityStageConfig,
  QuotationItem,
  QuotationQueryParams,
  SaveOpportunityParams,
  SaveQuotationParams,
  UpdateOpportunityParams,
  UpdateOpportunityStageParams,
  UpdateOpportunityStageRollbackParams,
  UpdateQuotationParams,
  BatchOperationResult,
} from '@lib/shared/models/opportunity';
import type { FormDesignConfigDetailParams } from '@lib/shared/models/system/module';
import { ValidateInfo } from '@lib/shared/models/system/org';
import type { ViewItem, ViewParams } from '@lib/shared/models/view';

export default function useProductApi(CDR: CordysAxios) {
  // 商机列表
  function getOpportunityList(data: OpportunityPageQueryParams) {
    return CDR.post<CommonList<OpportunityItem>>({ url: OptPageUrl, data }, { ignoreCancelToken: true });
  }

  // 添加商机
  function addOpportunity(data: SaveOpportunityParams) {
    return CDR.post({ url: OptAddUrl, data });
  }

  // 更新商机
  function updateOpportunity(data: UpdateOpportunityParams) {
    return CDR.post({ url: OptUpdateUrl, data });
  }

  // 商机详情
  function getOpportunityDetail(id: string) {
    return CDR.get<OpportunityDetail>({ url: `${GetOptDetailUrl}/${id}` });
  }

  // 商机看板拖拽排序
  function sortOpportunity(data: OpportunityBillboardDraggedParams) {
    return CDR.post({ url: SortOpportunityUrl, data });
  }

  // 获取商机表单配置
  function getOptFormConfig() {
    return CDR.get<FormDesignConfigDetailParams>({ url: GetOptFormConfigUrl });
  }

  // 商机跟进记录列表
  function getOptFollowRecordList(data: CustomerFollowRecordTableParams) {
    return CDR.post<CommonList<FollowDetailItem>>({ url: OptFollowRecordListUrl, data });
  }

  // 删除商机跟进记录
  function deleteOptFollowRecord(id: string) {
    return CDR.get({ url: `${DeleteOptFollowRecordUrl}/${id}` });
  }

  // 添加商机跟进记录
  function addOptFollowRecord(data: SaveCustomerFollowRecordParams) {
    return CDR.post({ url: AddOptFollowRecordUrl, data });
  }

  // 更新商机跟进记录
  function updateOptFollowRecord(data: UpdateCustomerFollowRecordParams) {
    return CDR.post({ url: UpdateOptFollowRecordUrl, data });
  }

  // 获取商机跟进记录详情
  function getOptFollowRecord(id: string) {
    return CDR.get<FollowDetailItem>({ url: `${GetOptFollowRecordUrl}/${id}` });
  }

  // 跟进计划列表
  function getOptFollowPlanList(data: CustomerFollowPlanTableParams) {
    return CDR.post<CommonList<FollowDetailItem>>({ url: OptFollowPlanPageUrl, data });
  }

  // 添加商机跟进计划
  function addOptFollowPlan(data: SaveCustomerFollowPlanParams) {
    return CDR.post({ url: AddOptFollowPlanUrl, data });
  }

  // 更新商机跟进计划
  function updateOptFollowPlan(data: UpdateCustomerFollowPlanParams) {
    return CDR.post({ url: UpdateOptFollowPlanUrl, data });
  }

  // 删除商机跟进计划
  function deleteOptFollowPlan(id: string) {
    return CDR.get({ url: `${DeleteOptFollowPlanUrl}/${id}` });
  }

  // 获取商机跟进计划详情
  function getOptFollowPlan(id: string) {
    return CDR.get<FollowDetailItem>({ url: `${GetOptFollowPlanUrl}/${id}` });
  }

  // 取消商机跟进计划
  function cancelOptFollowPlan(id: string) {
    return CDR.get({ url: `${CancelOptFollowPlanUrl}/${id}` });
  }

  // 批量转移商机
  function transferOpt(data: TransferParams) {
    return CDR.post({ url: OptBatchTransferUrl, data });
  }

  // 批量删除商机
  function batchDeleteOpt(data: (string | number)[]) {
    return CDR.post({ url: OptBatchDeleteUrl, data });
  }

  // 删除商机
  function deleteOpt(id: string) {
    return CDR.get({ url: `${OptDeleteUrl}/${id}` });
  }

  // 更新商机阶段
  function updateOptStage(data: { id: string; stage: string; failureReason?: string | null }) {
    return CDR.post({ url: OptUpdateStageUrl, data });
  }

  // 获取商机tab显隐藏
  function getOptTab() {
    return CDR.get<CustomerTabHidden>({ url: GetOptTabUrl });
  }
 

  // 获取商机联系人列表
  function getOpportunityContactList(data: CustomerContractTableParams) {
    return CDR.get({ url: `${GetOpportunityContactListUrl}/${data.id}` });
  }

  // 更新商机跟进计划状态
  function updateOptFollowPlanStatus(data: UpdateFollowPlanStatusParams) {
    return CDR.post({ url: UpdateOptFollowPlanStatusUrl, data });
  }

  // 导出全量商机列表
  function exportOpportunityAll(data: TableExportParams) {
    return CDR.post({ url: ExportOpportunityAllUrl, data });
  }

  // 导出选中商机列表
  function exportOpportunitySelected(data: TableExportSelectedParams) {
    return CDR.post({ url: ExportOpportunitySelectedUrl, data });
  }

  // 商机列表的金额数据
  function getOptStatistic(data: TableQueryParams) {
    return CDR.post({ url: GetOptStatisticUrl, data }, { ignoreCancelToken: true });
  }

  // 更新商机阶段配置
  function updateOpportunityStage(data: UpdateOpportunityStageParams) {
    return CDR.post({ url: UpdateOpportunityStageUrl, data });
  }

  // 商机阶段回退配置
  function updateOpportunityStageRollback(data: UpdateOpportunityStageRollbackParams) {
    return CDR.post({ url: UpdateOpportunityStageRollbackUrl, data });
  }

  // 商机阶段排序
  function sortOpportunityStage(data: string[]) {
    return CDR.post({ url: SortOpportunityStageUrl, data });
  }

  // 添加商机阶段
  function addOpportunityStage(data: AddOpportunityStageParams) {
    return CDR.post({ url: AddOpportunityStageUrl, data });
  }

  // 获取商机阶段配置
  function getOpportunityStageConfig() {
    return CDR.get<OpportunityStageConfig>({ url: GetOpportunityStageConfigUrl }, { ignoreCancelToken: true });
  }

  // 删除商机阶段
  function deleteOpportunityStage(id: string) {
    return CDR.get({ url: `${DeleteOpportunityStageUrl}/${id}` });
  }

  // 生成商机图表
  function generateOpportunityChart(data: GenerateChartParams) {
    return CDR.post<ChartResponseDataItem[]>({ url: GenerateOpportunityChartUrl, data });
  }

  // 商机视图
  function addBusinessView(data: ViewParams) {
    return CDR.post({ url: AddBusinessViewUrl, data });
  }

  function updateBusinessView(data: ViewParams) {
    return CDR.post({ url: UpdateBusinessViewUrl, data });
  }

  function getBusinessViewList() {
    return CDR.get<ViewItem[]>({ url: GetBusinessViewListUrl });
  }

  function getBusinessViewDetail(id: string) {
    return CDR.get({ url: `${GetBusinessViewDetailUrl}/${id}` });
  }

  function fixedBusinessView(id: string) {
    return CDR.get({ url: `${FixedBusinessViewUrl}/${id}` });
  }

  function enableBusinessView(id: string) {
    return CDR.get({ url: `${EnableBusinessViewUrl}/${id}` });
  }

  function deleteBusinessView(id: string) {
    return CDR.get({ url: `${DeleteBusinessViewUrl}/${id}` });
  }

  function dragBusinessView(data: TableDraggedParams) {
    return CDR.post({ url: DragBusinessViewUrl, data });
  }

  function globalSearchOptPage(data: TableQueryParams) {
    return CDR.post<CommonList<OpportunityItem>>({ url: GlobalSearchOptPageUrl, data }, { ignoreCancelToken: true });
  }

  function advancedSearchOptPage(data: TableQueryParams) {
    return CDR.post<CommonList<OpportunityItem>>({ url: AdvancedSearchOptPageUrl, data }, { ignoreCancelToken: true });
  }

  function advancedSearchOptDetail(data: TableQueryParams) {
    return CDR.post<CommonList<OpportunityItem>>({ url: AdvancedSearchOptDetailUrl, data });
  }

  function preCheckImportOpt(file: File) {
    return CDR.uploadFile<{ data: ValidateInfo }>({ url: PreCheckOptImportUrl }, { fileList: [file] }, 'file');
  }

  function downloadOptTemplate() {
    return CDR.get(
      {
        url: DownloadOptTemplateUrl,
        responseType: 'blob',
      },
      { isTransformResponse: false, isReturnNativeResponse: true }
    );
  }

  function importOpportunity(file: File) {
    return CDR.uploadFile({ url: ImportOpportunityUrl }, { fileList: [file] }, 'file');
  }

  // 批量更新商机
  function batchUpdateOpportunity(data: BatchUpdatePoolAccountParams) {
    return CDR.post({ url: BatchUpdateOpportunityUrl, data });
  }

   // 获取商机报价单tab显隐藏
  function getQuotationTab() {
    return CDR.get<CustomerTabHidden>({ url: GetQuotationTabUrl });
  }

  // 报价单视图
  function addQuotationView(data: ViewParams) {
    return CDR.post({ url: AddQuotationViewUrl, data });
  }

  function updateQuotationView(data: ViewParams) {
    return CDR.post({ url: UpdateQuotationViewUrl, data });
  }

  function getQuotationViewList() {
    return CDR.get<ViewItem[]>({ url: GetQuotationViewListUrl });
  }

  function getQuotationViewDetail(id: string) {
    return CDR.get({ url: `${GetQuotationViewDetailUrl}/${id}` });
  }

  function fixedQuotationView(id: string) {
    return CDR.get({ url: `${FixedQuotationViewUrl}/${id}` });
  }

  function enableQuotationView(id: string) {
    return CDR.get({ url: `${EnableQuotationViewUrl}/${id}` });
  }

  function deleteQuotationView(id: string) {
    return CDR.get({ url: `${DeleteQuotationViewUrl}/${id}` });
  }

  function dragQuotationView(data: TableDraggedParams) {
    return CDR.post({ url: DragQuotationViewUrl, data });
  }


  // 报价单
  // 报价列表
  function getQuotationList(data: QuotationQueryParams) {
    return CDR.post<CommonList<QuotationItem>>({ url: QuotationPageUrl, data });
  }

  // 添加报价
  function addQuotation(data: SaveQuotationParams) {
    return CDR.post({ url: AddQuotationUrl, data });
  }

  // 更新报价
  function updateQuotation(data: UpdateQuotationParams) {
    return CDR.post({ url: UpdateQuotationUrl, data });
  }

  // 报价详情
  function getQuotationDetail(id: string) {
    return CDR.get<QuotationItem>({ url: `${GetQuotationDetailUrl}/${id}` });
  }
  
  // 获取报价表单配置
  function getQuotationFormConfig() {
    return CDR.get<FormDesignConfigDetailParams>({ url: GetQuotationFormConfigUrl });
  }

  // 获取报价表单快照配置
  function getQuotationSnapshotFormConfig() {
    return CDR.get<FormDesignConfigDetailParams>({ url: GetQuotationSnapshotFormConfigUrl });
  }

  // 删除报价
  function deleteQuotation(id: string) {
    return CDR.get({ url: `${DeleteQuotationUrl}/${id}` });
  }

   // 作废报价
  function voidQuotation(id: string) {
    return CDR.get({ url: `${VoidQuotationUrl}/${id}` });
  }

    // 审批报价
  function approvalQuotation(data: ApproveQuotation) {
    return CDR.post({ url: ApprovalQuotationUrl,data });
  }

    // 撤销报价
  function revokeQuotation(id: string) {
    return CDR.get({ url: `${RevokeQuotationUrl}/${id}` });
  }

  function batchApprove(data: BatchApproveParams) {
    return CDR.post<BatchOperationResult>({ url: BatchApproveUrl,data});
  }

  function batchVoided(data: (string|number)[]) {
    return CDR.post<BatchOperationResult>({ url: BatchVoidedUrl, data });
  }

  return {
    getOpportunityList,
    addOpportunity,
    updateOpportunity,
    getOpportunityDetail,
    getOptFormConfig,
    getOptFollowRecordList,
    deleteOptFollowRecord,
    addOptFollowRecord,
    updateOptFollowRecord,
    getOptFollowRecord,
    getOptFollowPlanList,
    addOptFollowPlan,
    updateOptFollowPlan,
    deleteOptFollowPlan,
    getOptFollowPlan,
    cancelOptFollowPlan,
    transferOpt,
    batchDeleteOpt,
    deleteOpt,
    updateOptStage,
    getOptTab,
    getOpportunityContactList,
    updateOptFollowPlanStatus,
    exportOpportunityAll,
    exportOpportunitySelected,
    addBusinessView,
    deleteBusinessView,
    fixedBusinessView,
    getBusinessViewDetail,
    getBusinessViewList,
    updateBusinessView,
    enableBusinessView,
    dragBusinessView,
    advancedSearchOptPage,
    globalSearchOptPage,
    advancedSearchOptDetail,
    preCheckImportOpt,
    downloadOptTemplate,
    importOpportunity,
    getOptStatistic,
    batchUpdateOpportunity,
    sortOpportunity,
    updateOpportunityStage,
    updateOpportunityStageRollback,
    sortOpportunityStage,
    addOpportunityStage,
    getOpportunityStageConfig,
    deleteOpportunityStage,
    generateOpportunityChart,
    getQuotationTab,
    addQuotationView,
    deleteQuotationView,
    fixedQuotationView,
    getQuotationViewDetail,
    getQuotationViewList,
    updateQuotationView,
    enableQuotationView,
    dragQuotationView,
    getQuotationList,
    addQuotation,
    updateQuotation,
    getQuotationDetail,
    getQuotationFormConfig,
    getQuotationSnapshotFormConfig,
    deleteQuotation,
    approvalQuotation,
    voidQuotation,
    revokeQuotation,
    batchApprove,
    batchVoided,
  };
}
