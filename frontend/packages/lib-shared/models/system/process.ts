import { ProcessStatusEnum } from "@lib/shared/enums/process";


export type ProcessStatusType = Exclude<ProcessStatusEnum, ProcessStatusEnum.VOIDED>;

export interface BaseItem {
  id: string;
  name: string;
}

// 权限项
export interface PermissionItem {
  id: string;
  name: string;
  enabled: boolean;
}

// 审批流程基本信息接口
export interface ApprovalProcessItem extends BaseItem {
  number: string;
  formType: string;
  executeTiming: string;
  enable: boolean;
  submitterCanRevoke: boolean;
  allowBatchProcess: boolean;
  allowWithdraw: boolean;
  allowAddSign: boolean;
  duplicateApproverRule: string;
  requireComment: boolean;
  organizationId: string;
  createUserName: string;
  createUser: string;
  updateUser: string;
  updateUserName: string;
  createTime: number;
  updateTime: number;
}

// 审批权限详情
export interface ApprovalPermissionsDetail {
  permissions: PermissionItem[];
  statusPermissions: StatusPermissions[];
}

// 审批流程节点
export interface ApprovalProcessNode extends BaseItem {
  nodeType: string;
  sort: number;
  children: ApprovalProcessNode[];
}
// 状态权限
export interface StatusPermissions {
  approvalStatus: ProcessStatusType;
  permission: string;
  enabled: boolean;
}

// 基本表单参数
export interface BasicFormParams {
  name: string;
  formType: string;
  description: string;
  executeTiming: string[];
}

// 更多设置参数
export interface MoreSettingsParams {
  submitterCanRevoke: boolean;
  allowBatchProcess: boolean;
  allowWithdraw: boolean;
  allowAddSign: boolean;
  duplicateApproverRule: string;
  requireComment: boolean;
  permissions: BaseItem[];
  statusPermissions: StatusPermissions[];
}

export interface AddApprovalProcessParams extends BasicFormParams, MoreSettingsParams {
  enable: boolean;
  nodes: ApprovalProcessNode[];
}

export interface UpdateApprovalProcessParams extends AddApprovalProcessParams {
  id: string;
}

export interface ApprovalProcessDetail extends UpdateApprovalProcessParams {
  permissions: PermissionItem[];
  id: string;
  number: string;
}

export interface ApprovalProcessForm {
  id: string;
  enable: boolean;
  nodes: ApprovalProcessNode[];
  basicConfig: BasicFormParams;
  moreConfig: MoreSettingsParams;
}

