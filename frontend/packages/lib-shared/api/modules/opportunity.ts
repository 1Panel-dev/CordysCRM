import type { CordysAxios } from '@lib/shared/api/http/Axios';
import {
  AddOptFollowPlanUrl,
  AddOptFollowRecordUrl,
  CancelOptFollowPlanUrl,
  DeleteOptFollowPlanUrl,
  DeleteOptFollowRecordUrl,
  GetOptDetailUrl,
  GetOptFollowPlanUrl,
  GetOptFollowRecordUrl,
  GetOptFormConfigUrl,
  GetOptTabUrl,
  OptAddUrl,
  OptBatchDeleteUrl,
  OptBatchTransferUrl,
  OptDeleteUrl,
  OptFollowPlanPageUrl,
  OptFollowRecordListUrl,
  OptPageUrl,
  OptUpdateStageUrl,
  OptUpdateUrl,
  UpdateOptFollowPlanUrl,
  UpdateOptFollowRecordUrl,
} from '@lib/shared/api/requrls/opportunity';
import type { CommonList, TableQueryParams } from '@lib/shared/models/common';
import type {
  CustomerFollowPlanTableParams,
  CustomerFollowRecordTableParams,
  CustomerTabHidden,
  FollowDetailItem,
  SaveCustomerFollowPlanParams,
  SaveCustomerFollowRecordParams,
  TransferParams,
  UpdateCustomerFollowPlanParams,
  UpdateCustomerFollowRecordParams,
} from '@lib/shared/models/customer';
import type {
  OpportunityDetail,
  OpportunityItem,
  SaveOpportunityParams,
  UpdateOpportunityParams,
} from '@lib/shared/models/opportunity';
import type { FormDesignConfigDetailParams } from '@lib/shared/models/system/module';

export default function useProductApi(CDR: CordysAxios) {
  // 商机列表
  function getOpportunityList(data: TableQueryParams) {
    return CDR.post<CommonList<OpportunityItem>>({ url: OptPageUrl, data });
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
  function updateOptStage(data: { id: string; stage: string }) {
    return CDR.post({ url: OptUpdateStageUrl, data });
  }

  // 获取商机tab显隐藏
  function getOptTab() {
    return CDR.get<CustomerTabHidden>({ url: GetOptTabUrl });
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
  };
}
