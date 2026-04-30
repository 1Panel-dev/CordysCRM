import type { CordysAxios } from '@lib/shared/api/http/Axios';
import {
  ApprovalPermissionsUrl,
  AddApprovalProcessUrl,
  UpdateApprovalProcessUrl,
  DeleteApprovalProcessUrl,
  ApprovalProcessDetailUrl,
  ToggleApprovalProcessUrl,
  ApprovalProcessPageUrl,
  GetApprovalConfigDetailUrl,
} from '@lib/shared/api/requrls/system/process';
import {
  AddApprovalProcessParams,
  ApprovalPermissionsDetail,
  ApprovalProcessDetail,
  ApprovalProcessItem,
  UpdateApprovalProcessParams,
} from '@lib/shared/models/system/process';
import type { CommonList } from '@lib/shared/models/common';
import type { TableQueryParams } from '@lib/shared/models/common';

export default function useProcessApi(CDR: CordysAxios) {

  // 审批流数据权限
  function getApprovalPermissions(type: string) {
    return CDR.get<ApprovalPermissionsDetail>({ url:`${ApprovalPermissionsUrl}/${type}` });
  }
  // 审批流配置详情 用于列表里边查询对应状态审批流详情
  function getApprovalConfigDetail(type: string) {
    return CDR.get<ApprovalProcessDetail>({ url:`${GetApprovalConfigDetailUrl}/${type}` });
  }
  // 审批流数据权限
  function getApprovalProcessList(data: TableQueryParams) {
    return CDR.post<CommonList<ApprovalProcessItem>>({ url:ApprovalProcessPageUrl, data});
  }
  // 添加审批流
  function addApprovalProcess(data: AddApprovalProcessParams) {
    return CDR.post({ url:AddApprovalProcessUrl, data });
  }
  // 更新审批流
  function updateApprovalProcess(data: UpdateApprovalProcessParams) {
    return CDR.post({ url:UpdateApprovalProcessUrl,data });
  }
  // 审批流详情
  function approvalProcessDetail(id: string) {
    return CDR.get<ApprovalProcessDetail>({ url:`${ApprovalProcessDetailUrl}/${id}` });
  }
  // 删除审批流
  function deleteApprovalProcess(id: string) {
    return CDR.get({ url:`${DeleteApprovalProcessUrl}/${id}` });
  }
  // 切换审批流
  function toggleApprovalProcess(id: string, enable: boolean) {
    return CDR.get({ url:`${ToggleApprovalProcessUrl}/${id}`,params: { enable }  });
  }

  return {
    getApprovalProcessList,
    getApprovalPermissions,
    addApprovalProcess,
    updateApprovalProcess,
    approvalProcessDetail,
    deleteApprovalProcess,
    toggleApprovalProcess,
    getApprovalConfigDetail,
  };
}
