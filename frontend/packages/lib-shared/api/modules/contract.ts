import type { CordysAxios } from '@lib/shared/api/http/Axios';
import type { FormDesignConfigDetailParams } from '@lib/shared/models/system/module';
import type { TableQueryParams } from '@lib/shared/models/common';

import {
  ContractPageUrl,
  ContractAddUrl,
  ContractUpdateUrl,
  ContractDeleteUrl,
  GetContractDetailUrl,
  GetContractFormConfigUrl,
  GetContractTabUrl,
  ContractVoidedUrl,
  ContractArchivedUrl,
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
} from '@lib/shared/models/contract';
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

  // 作废合同
  function voidedContract(id: string, reason: string) {
    return CDR.post({ url: `${ContractVoidedUrl}`, data: { reason, id } });
  }

  // 归档合同
  function archivedContract(id: string, archivedStatus: string) {
    return CDR.post({ url: `${ContractArchivedUrl}`, data: { archivedStatus, id } });
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
    return CDR.post<CommonList<PaymentPlanItem>>({ url: PaymentPlanPageUrl, data }, { ignoreCancelToken: true });
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
    getContractFormConfig,
    voidedContract,
    archivedContract,
    getContractFormSnapshotConfig,
    // 回款计划
    getPaymentPlanList,
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
  };
}
