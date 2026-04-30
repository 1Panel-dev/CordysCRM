import { ApprovalTypeEnum, ApproverTypeEnum, ProcessStatusEnum } from '@lib/shared/enums/process';
import type { SelectedUsersItem } from './module';
import type { FilterForm } from '@cordys/web/src/components/pure/crm-advance-filter/type';
import type { ActionNode, ConditionBranch } from '@cordys/web/src/components/business/crm-flow/types';


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

// 状态权限
export interface ApprovalPermission {
  approvalStatus: ProcessStatusType;
  permission: string[];
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
  statusPermissions: ApprovalPermission[];
}

// 审批流程节点
export interface ApprovalProcessNode extends BaseItem {
  nodeType: string;
  sort: number;
  children: ApprovalProcessNode[];
}

// 节点类型：开始/审批人/条件/默认分支/结束
export type ApprovalNodeType = 'START' | 'APPROVER' | 'CONDITION' | 'DEFAULT' | 'END';
// 多人审批方式：会签/或签/依次审批
export type MultiApproverMode = 'ALL' | 'ANY' | 'SEQUENTIAL';
// 审批人为空时动作：自动通过/自动拒绝/转交管理员
export type EmptyApproverAction = 'AUTO_PASS' | 'AUTO_REJECT' | 'TRANSFER_ADMIN';
// 审批人与提交人相同时动作：跳过/自动通过/自动拒绝/转交管理员
export type SameSubmitterAction = 'SKIP' | 'AUTO_PASS' | 'AUTO_REJECT' | 'TRANSFER_ADMIN';

// 审批动作节点（前端审批流图使用）
export interface ApprovalActionNode extends ActionNode {
  approvalType: ApprovalTypeEnum; // 审批类型：人工审批/自动通过/自动拒绝
  approverType: ApproverTypeEnum; // 审批人来源
  approverList: string[]; // 审批人配置列表：成员 ID / 角色 ID / 层级等
  approverSelectedList?: SelectedUsersItem[]; // 前端展示用的审批人选中列表
  multiApproverMode: MultiApproverMode; // 多人审批方式
  emptyApproverAction: EmptyApproverAction; // 审批人为空时动作
  sameSubmitterAction: SameSubmitterAction; // 审批人与提交人相同时动作
  cc: SelectedUsersItem[]; // 抄送人列表
  passPostConfig?: Record<string, any>; // 审批通过后配置
  rejectPostConfig?: Record<string, any>; // 审批驳回后配置
  fieldPermissions?: Record<string, any>[]; // 字段权限配置列表
}

// 审批条件分支（前端审批流图使用）
export interface ApprovalConditionBranch extends ConditionBranch {
  conditionConfig?: FilterForm;
}

// 状态权限配置
export interface ApprovalPermission {
  status: ProcessStatusEnum;
  statusLabel: string;
  permissions: PermissionItem[];
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
