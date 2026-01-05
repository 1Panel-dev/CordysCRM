import type { CordysAxios } from '@lib/shared/api/http/Axios';
import type { FormDesignConfigDetailParams } from '@lib/shared/models/system/module';
import type { TableQueryParams } from '@lib/shared/models/common';
import { ValidateInfo } from '@lib/shared/models/system/org';

import {
  ContractPageUrl,
  ContractAddUrl,
  ContractUpdateUrl,
  ContractDeleteUrl,
  GetContractDetailUrl,
  GetContractFormConfigUrl,
  GetContractTabUrl,
  ChangeContractStatusUrl,
  GetContractFormSnapshotConfigUrl,
  ExportContractAllUrl,
  ExportContractSelectedUrl,
  GenerateContractChartUrl,
  AddContractViewUrl,
  UpdateContractViewUrl,
  GetContractViewListUrl,
  GetContractViewDetailUrl,
  FixedContractViewUrl,
  EnableContractViewUrl,
  DeleteContractViewUrl,
  DragContractViewUrl,
  PaymentPlanPageUrl,
  PaymentPlanAddUrl,
  ContractPaymentPlanPageUrl,
  PaymentPlanUpdateUrl,
  PaymentPlanDeleteUrl,
  GetPaymentPlanDetailUrl,
  GetPaymentPlanFormConfigUrl,
  GetPaymentPlanTabUrl,
  ExportPaymentPlanAllUrl,
  ExportPaymentPlanSelectedUrl,
  GeneratePaymentPlanChartUrl,
  AddPaymentPlanViewUrl,
  UpdatePaymentPlanViewUrl,
  GetPaymentPlanViewListUrl,
  GetPaymentPlanViewDetailUrl,
  FixedPaymentPlanViewUrl,
  EnablePaymentPlanViewUrl,
  DeletePaymentPlanViewUrl,
  DragPaymentPlanViewUrl,
  BatchApproveContractUrl,
  ApproveContractUrl,
  RevokeContractUrl,
  PaymentRecordPageUrl,
  PaymentRecordAddUrl,
  PaymentRecordUpdateUrl,
  PaymentRecordDeleteUrl,
  GetPaymentRecordDetailUrl,
  GetPaymentRecordFormConfigUrl,
  GetPaymentRecordTabUrl,
  ExportPaymentRecordAllUrl,
  ExportPaymentRecordSelectedUrl,
  AddPaymentRecordViewUrl,
  UpdatePaymentRecordViewUrl,
  GetPaymentRecordViewListUrl,
  GetPaymentRecordViewDetailUrl,
  FixedPaymentRecordViewUrl,
  EnablePaymentRecordViewUrl,
  DeletePaymentRecordViewUrl,
  DragPaymentRecordViewUrl,
  PreCheckPaymentRecordImportUrl,
  DownloadPaymentRecordTemplateUrl,
  ImportPaymentRecordUrl,
  DownloadBusinessNameTemplateUrl,
  ImportBusinessNameUrl,
  PreCheckBusinessNameImportUrl,
  BusinessNamePageUrl,
  BusinessNameAddUrl,
  BusinessNameUpdateUrl,
  BusinessNameDeleteUrl,
  GetBusinessNameDetailUrl,
  BusinessNameRevokeUrl,
} from '@lib/shared/api/requrls/contract';
import type { CustomerTabHidden } from '@lib/shared/models/customer';
import type {
  ChartResponseDataItem,
  CommonList,
  GenerateChartParams,
  TableDraggedParams,
  TableExportParams,
  TableExportSelectedParams,
} from '@lib/shared/models/common';
import type { ViewItem, ViewParams } from '@lib/shared/models/view';
import type {
  ContractDetail,
  ContractItem,
  SaveContractParams,
  UpdateContractParams,
  PaymentPlanItem,
  PaymentPlanDetail,
  SavePaymentPlanParams,
  UpdatePaymentPlanParams,
  ApprovalContractParams,
  PaymentRecordItem,
  PaymentRecordDetail,
  SavePaymentRecordParams,
  UpdatePaymentRecordParams,
  BusinessNameItem,
  SaveBusinessNameParams,
  UpdateBusinessNameParams,
} from '@lib/shared/models/contract';
import type { BatchOperationResult, BatchUpdateQuotationStatusParams } from '@lib/shared/models/opportunity';
export default function useContractApi(CDR: CordysAxios) {
  // 合同列表
  function getContractList(data: TableQueryParams) {
    return CDR.post<CommonList<ContractItem>>({ url: ContractPageUrl, data }, { ignoreCancelToken: true });
  }

  // 添加合同
  function addContract(data: SaveContractParams) {
    return CDR.post({ url: ContractAddUrl, data });
  }

  // 更新合同
  function updateContract(data: UpdateContractParams) {
    return CDR.post({ url: ContractUpdateUrl, data });
  }

  // 删除合同
  function deleteContract(id: string) {
    return CDR.get({ url: `${ContractDeleteUrl}/${id}` });
  }

  // 合同详情
  function getContractDetail(id: string) {
    return CDR.get<ContractDetail>({ url: `${GetContractDetailUrl}/${id}` });
  }

  // 获取合同表单配置
  function getContractFormConfig() {
    return CDR.get<FormDesignConfigDetailParams>({
      url: GetContractFormConfigUrl,
    });
  }

  function getContractFormSnapshotConfig(id?: string) {
    return CDR.get<FormDesignConfigDetailParams>({
      url: `${GetContractFormSnapshotConfigUrl}/${id}`,
    });
  }

  function changeContractStatus(id: string, stage: string, voidReason?: string) {
    return CDR.post({ url: `${ChangeContractStatusUrl}`, data: { stage, id, voidReason } });
  }

  // 获取合同tab显隐藏
  function getContractTab() {
    return CDR.get<CustomerTabHidden>({ url: GetContractTabUrl });
  }

  // 导出全量合同列表
  function exportContractAll(data: TableExportParams) {
    return CDR.post({ url: ExportContractAllUrl, data });
  }

  // 导出选中合同列表
  function exportContractSelected(data: TableExportSelectedParams) {
    return CDR.post({ url: ExportContractSelectedUrl, data });
  }

  // 生成合同图表
  function generateContractChart(data: GenerateChartParams) {
    return CDR.post<ChartResponseDataItem[]>({
      url: GenerateContractChartUrl,
      data,
    });
  }

  function batchApproveContract(data: BatchUpdateQuotationStatusParams) {
    return CDR.post<BatchOperationResult>({ url: BatchApproveContractUrl, data });
  }

  function approvalContract(data: ApprovalContractParams) {
    return CDR.post({ url: ApproveContractUrl, data });
  }

  function revokeContract(id: string) {
    return CDR.get({ url: `${RevokeContractUrl}/${id}` });
  }

  // 视图
  function addContractView(data: ViewParams) {
    return CDR.post({ url: AddContractViewUrl, data });
  }

  function updateContractView(data: ViewParams) {
    return CDR.post({ url: UpdateContractViewUrl, data });
  }

  function getContractViewList() {
    return CDR.get<ViewItem[]>({ url: GetContractViewListUrl });
  }

  function getContractViewDetail(id: string) {
    return CDR.get({ url: `${GetContractViewDetailUrl}/${id}` });
  }

  function fixedContractView(id: string) {
    return CDR.get({ url: `${FixedContractViewUrl}/${id}` });
  }

  function enableContractView(id: string) {
    return CDR.get({ url: `${EnableContractViewUrl}/${id}` });
  }

  function deleteContractView(id: string) {
    return CDR.get({ url: `${DeleteContractViewUrl}/${id}` });
  }

  function dragContractView(data: TableDraggedParams) {
    return CDR.post({ url: DragContractViewUrl, data });
  }

  // 回款计划列表
  function getPaymentPlanList(data: TableQueryParams) {
    return CDR.post<CommonList<PaymentPlanItem>>({ url: PaymentPlanPageUrl, data });
  }

  function getContractPaymentPlanList(data: TableQueryParams) {
    return CDR.post<CommonList<PaymentPlanItem>>({ url: ContractPaymentPlanPageUrl, data });
  }

  // 添加回款计划
  function addPaymentPlan(data: SavePaymentPlanParams) {
    return CDR.post({ url: PaymentPlanAddUrl, data });
  }

  // 更新回款计划
  function updatePaymentPlan(data: UpdatePaymentPlanParams) {
    return CDR.post({ url: PaymentPlanUpdateUrl, data });
  }

  // 删除回款计划
  function deletePaymentPlan(id: string) {
    return CDR.get({ url: `${PaymentPlanDeleteUrl}/${id}` });
  }

  // 回款计划详情
  function getPaymentPlanDetail(id: string) {
    return CDR.get<PaymentPlanDetail>({ url: `${GetPaymentPlanDetailUrl}/${id}` });
  }

  // 获取回款计划表单配置
  function getPaymentPlanFormConfig() {
    return CDR.get<FormDesignConfigDetailParams>({
      url: GetPaymentPlanFormConfigUrl,
    });
  }

  // 获取回款计划 tab 显隐
  function getPaymentPlanTab() {
    return CDR.get<CustomerTabHidden>({ url: GetPaymentPlanTabUrl });
  }

  // 导出全量回款计划
  function exportPaymentPlanAll(data: TableExportParams) {
    return CDR.post({ url: ExportPaymentPlanAllUrl, data });
  }

  // 导出选中回款计划
  function exportPaymentPlanSelected(data: TableExportSelectedParams) {
    return CDR.post({ url: ExportPaymentPlanSelectedUrl, data });
  }

  // 生成回款计划图表
  function generatePaymentPlanChart(data: GenerateChartParams) {
    return CDR.post<ChartResponseDataItem[]>({
      url: GeneratePaymentPlanChartUrl,
      data,
    });
  }

  // 添加视图
  function addPaymentPlanView(data: ViewParams) {
    return CDR.post({ url: AddPaymentPlanViewUrl, data });
  }

  // 更新视图
  function updatePaymentPlanView(data: ViewParams) {
    return CDR.post({ url: UpdatePaymentPlanViewUrl, data });
  }

  // 获取视图列表
  function getPaymentPlanViewList() {
    return CDR.get<ViewItem[]>({ url: GetPaymentPlanViewListUrl });
  }

  // 获取视图详情
  function getPaymentPlanViewDetail(id: string) {
    return CDR.get({ url: `${GetPaymentPlanViewDetailUrl}/${id}` });
  }

  // 固定视图
  function fixedPaymentPlanView(id: string) {
    return CDR.get({ url: `${FixedPaymentPlanViewUrl}/${id}` });
  }

  // 启用视图
  function enablePaymentPlanView(id: string) {
    return CDR.get({ url: `${EnablePaymentPlanViewUrl}/${id}` });
  }

  // 删除视图
  function deletePaymentPlanView(id: string) {
    return CDR.get({ url: `${DeletePaymentPlanViewUrl}/${id}` });
  }

  // 拖拽排序视图
  function dragPaymentPlanView(data: TableDraggedParams) {
    return CDR.post({ url: DragPaymentPlanViewUrl, data });
  }

  // 回款记录列表
  function getPaymentRecordList(data: TableQueryParams) {
    return CDR.post<CommonList<PaymentRecordItem>>({ url: PaymentRecordPageUrl, data }, { ignoreCancelToken: true });
  }

  // 添加回款记录
  function addPaymentRecord(data: SavePaymentRecordParams) {
    return CDR.post({ url: PaymentRecordAddUrl, data });
  }

  // 更新回款记录
  function updatePaymentRecord(data: UpdatePaymentRecordParams) {
    return CDR.post({ url: PaymentRecordUpdateUrl, data });
  }

  // 删除回款记录
  function deletePaymentRecord(id: string) {
    return CDR.get({ url: `${PaymentRecordDeleteUrl}/${id}` });
  }

  // 回款记录详情
  function getPaymentRecordDetail(id: string) {
    return CDR.get<PaymentRecordDetail>({ url: `${GetPaymentRecordDetailUrl}/${id}` });
  }

  // 获取回款记录表单配置
  function getPaymentRecordFormConfig() {
    return CDR.get<FormDesignConfigDetailParams>({
      url: GetPaymentRecordFormConfigUrl,
    });
  }

  // 获取回款记录 tab 显隐
  function getPaymentRecordTab() {
    return CDR.get<CustomerTabHidden>({ url: GetPaymentRecordTabUrl });
  }

  // 导出全量回款记录
  function exportPaymentRecordAll(data: TableExportParams) {
    return CDR.post({ url: ExportPaymentRecordAllUrl, data });
  }

  // 导出选中回款记录
  function exportPaymentRecordSelected(data: TableExportSelectedParams) {
    return CDR.post({ url: ExportPaymentRecordSelectedUrl, data });
  }

  // 添加视图
  function addPaymentRecordView(data: ViewParams) {
    return CDR.post({ url: AddPaymentRecordViewUrl, data });
  }

  // 更新视图
  function updatePaymentRecordView(data: ViewParams) {
    return CDR.post({ url: UpdatePaymentRecordViewUrl, data });
  }

  // 获取视图列表
  function getPaymentRecordViewList() {
    return CDR.get<ViewItem[]>({ url: GetPaymentRecordViewListUrl });
  }

  // 获取视图详情
  function getPaymentRecordViewDetail(id: string) {
    return CDR.get({ url: `${GetPaymentRecordViewDetailUrl}/${id}` });
  }

  // 固定视图
  function fixedPaymentRecordView(id: string) {
    return CDR.get({ url: `${FixedPaymentRecordViewUrl}/${id}` });
  }

  // 启用视图
  function enablePaymentRecordView(id: string) {
    return CDR.get({ url: `${EnablePaymentRecordViewUrl}/${id}` });
  }

  // 删除视图
  function deletePaymentRecordView(id: string) {
    return CDR.get({ url: `${DeletePaymentRecordViewUrl}/${id}` });
  }

  // 拖拽排序视图
  function dragPaymentRecordView(data: TableDraggedParams) {
    return CDR.post({ url: DragPaymentRecordViewUrl, data });
  }

  function preCheckImportContractPaymentRecord(file: File) {
    return CDR.uploadFile<{ data: ValidateInfo }>(
      { url: PreCheckPaymentRecordImportUrl },
      { fileList: [file] },
      'file'
    );
  }

  function downloadContractPaymentRecordTemplate() {
    return CDR.get(
      {
        url: DownloadPaymentRecordTemplateUrl,
        responseType: 'blob',
      },
      { isTransformResponse: false, isReturnNativeResponse: true }
    );
  }

  function importContractPaymentRecord(file: File) {
    return CDR.uploadFile({ url: ImportPaymentRecordUrl }, { fileList: [file] }, 'file');
  }

  // 合同-工商抬头导入
  function preCheckImportBusinessName(file: File) {
    return CDR.uploadFile<{ data: ValidateInfo }>({ url: PreCheckBusinessNameImportUrl }, { fileList: [file] }, 'file');
  }

  function downloadBusinessNameTemplate() {
    return CDR.get(
      {
        url: DownloadBusinessNameTemplateUrl,
        responseType: 'blob',
      },
      { isTransformResponse: false, isReturnNativeResponse: true }
    );
  }

  function importBusinessName(file: File) {
    return CDR.uploadFile({ url: ImportBusinessNameUrl }, { fileList: [file] }, 'file');
  }

  // 工商抬头列表
  function getBusinessNameList(data: TableQueryParams) {
    return CDR.post<CommonList<BusinessNameItem>>({ url: BusinessNamePageUrl, data }, { ignoreCancelToken: true });
  }

  // 添加工商抬头
  function addBusinessName(data: SaveBusinessNameParams) {
    return CDR.post({ url: BusinessNameAddUrl, data });
  }

  // 更新工商抬头
  function updateBusinessName(data: UpdateBusinessNameParams) {
    return CDR.post({ url: BusinessNameUpdateUrl, data });
  }

  // 删除工商抬头
  function deleteBusinessName(id: string) {
    return CDR.get({ url: `${BusinessNameDeleteUrl}/${id}` });
  }

  //撤销工商抬头
  function revokeBusinessName(id: string) {
    return CDR.get({ url: `${BusinessNameRevokeUrl}/${id}` });
  }

  // 工商抬头详情 todo xinxinwu
  function getBusinessNameDetail(id: string) {
    return CDR.get<BusinessNameItem>({ url: `${GetBusinessNameDetailUrl}/${id}` });
  }

  return {
    exportContractAll,
    exportContractSelected,
    generateContractChart,
    getContractDetail,
    getContractList,
    getContractTab,
    getContractViewDetail,
    getContractViewList,
    addContractView,
    updateContractView,
    fixedContractView,
    enableContractView,
    deleteContractView,
    dragContractView,
    addContract,
    updateContract,
    deleteContract,
    changeContractStatus,
    getContractFormConfig,
    getContractFormSnapshotConfig,
    batchApproveContract,
    approvalContract,
    revokeContract,
    // 回款计划
    getPaymentPlanList,
    getContractPaymentPlanList,
    addPaymentPlan,
    updatePaymentPlan,
    deletePaymentPlan,
    getPaymentPlanDetail,
    getPaymentPlanFormConfig,
    getPaymentPlanTab,
    exportPaymentPlanAll,
    exportPaymentPlanSelected,
    generatePaymentPlanChart,
    addPaymentPlanView,
    updatePaymentPlanView,
    getPaymentPlanViewList,
    getPaymentPlanViewDetail,
    fixedPaymentPlanView,
    enablePaymentPlanView,
    deletePaymentPlanView,
    dragPaymentPlanView,
    // 回款记录
    getPaymentRecordFormConfig,
    addPaymentRecord,
    updatePaymentRecord,
    getPaymentRecordDetail,
    getPaymentRecordList,
    deletePaymentRecord,
    getPaymentRecordTab,
    exportPaymentRecordAll,
    exportPaymentRecordSelected,
    addPaymentRecordView,
    updatePaymentRecordView,
    getPaymentRecordViewList,
    getPaymentRecordViewDetail,
    fixedPaymentRecordView,
    enablePaymentRecordView,
    deletePaymentRecordView,
    dragPaymentRecordView,
    preCheckImportContractPaymentRecord,
    importContractPaymentRecord,
    downloadContractPaymentRecordTemplate,
    // 合同工商抬头
    preCheckImportBusinessName,
    downloadBusinessNameTemplate,
    importBusinessName,
    getBusinessNameList,
    addBusinessName,
    updateBusinessName,
    deleteBusinessName,
    revokeBusinessName,
    getBusinessNameDetail,
  };
}
