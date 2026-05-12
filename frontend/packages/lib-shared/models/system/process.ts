import {
  ApprovalOperationEnum,
  ApprovalTypeEnum,
  ApproverTypeEnum,
  ProcessStatusEnum,
  type ApprovalResourceTypeEnum,
} from '@lib/shared/enums/process';
import type { SelectedUsersItem } from './module';
import type { FilterForm } from '@cordys/web/src/components/pure/crm-advance-filter/type';
import type { UserInfo } from '@lib/shared/models/user';
import {
  ApprovalFieldPermissionModeEnum,
  ApprovalNodeTypeEnum,
  EmptyApproverActionEnum,
  MultiApproverModeEnum,
  SameSubmitterActionEnum,
} from '@lib/shared/enums/process';
import type { ActionNode, ConditionBranch } from '@cordys/web/src/components/business/crm-flow/types';
import type { TableQueryParams } from '../common';

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
  createExecute: boolean;
  updateExecute: boolean;
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

export interface ApprovalFieldPermission {
  fieldId: string;
  permissionType: ApprovalFieldPermissionModeEnum;
}

export interface ApprovalFieldUpdateConfig {
  fieldId: string;
  fieldValue: any;
  enable: boolean;
}

export interface ApprovalPostConfig {
  fieldUpdateConfigs: ApprovalFieldUpdateConfig[];
}

// 后端审批流节点的基础结构
export interface ApprovalProcessNodeBase<TNodeType extends ApprovalNodeTypeEnum> extends BaseItem {
  nodeType: TNodeType;
  sort: number;
  children: ApprovalProcessNode[];
}

// 审批节点里前后端共用的业务配置
export interface ApprovalNodeParticipantConfig {
  approvalType: ApprovalTypeEnum;
  approverType: ApproverTypeEnum;
  approverList: string[];
  multiApproverMode: MultiApproverModeEnum;
  emptyApproverAction: EmptyApproverActionEnum;
  fallbackApprover: string | null;
  fallbackApproverName?: string;
  sameSubmitterAction: SameSubmitterActionEnum;
  ccType: ApproverTypeEnum | null;
  ccList: string[];
  passPostConfig?: ApprovalPostConfig;
  rejectPostConfig?: ApprovalPostConfig;
  fieldPermissions?: ApprovalFieldPermission[];
}

export type ApprovalProcessStartNode = ApprovalProcessNodeBase<ApprovalNodeTypeEnum.START>;

export type ApprovalProcessEndNode = ApprovalProcessNodeBase<ApprovalNodeTypeEnum.END>;

export interface ApprovalProcessApproverNode
  extends ApprovalProcessNodeBase<ApprovalNodeTypeEnum.APPROVER>,
    ApprovalNodeParticipantConfig {}

export interface ApprovalProcessConditionNode extends ApprovalProcessNodeBase<ApprovalNodeTypeEnum.CONDITION> {
  nodeType: ApprovalNodeTypeEnum.CONDITION;
  conditionConfig?: FilterForm;
}

export type ApprovalProcessDefaultNode = ApprovalProcessNodeBase<ApprovalNodeTypeEnum.DEFAULT>;

export type ApprovalProcessNode =
  | ApprovalProcessStartNode
  | ApprovalProcessEndNode
  | ApprovalProcessApproverNode
  | ApprovalProcessConditionNode
  | ApprovalProcessDefaultNode;

// 审批动作节点（前端审批流图使用）
export interface ApprovalActionNode extends ActionNode, ApprovalNodeParticipantConfig {
  approverSelectedList?: SelectedUsersItem[];
  emptyApproverSelectedList?: SelectedUsersItem[];
  ccSelectedList?: SelectedUsersItem[];
}

// 审批条件分支（前端审批流图使用）
export interface ApprovalConditionBranch extends ConditionBranch {
  conditionConfig?: FilterForm;
}

// 状态权限
export interface StatusPermissions {
  approvalStatus: ProcessStatusEnum;
  permission: string;
  enabled: boolean;
}

// 基本表单参数
export interface BasicFormParams {
  name: string;
  formType: string;
  description: string;
  createExecute: boolean; // 创建执行
  updateExecute: boolean; // 更新执行
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

export interface ApproverItem extends Pick<UserInfo, 'id' | 'name' | 'avatar'> {
  approveResult: ApprovalOperationEnum;
  approveReason: string;
}

export interface ApprovalPopoverDetail {
  resourceId: string;
  approveStatus: ProcessStatusEnum;
  approveUserList: ApproverItem[];
}

export interface CommonApprovalActionParams {
  resourceId: string;
  formKey: string;
}

export interface ApprovalTodoTableParams extends TableQueryParams {
  resourceName?: string;
  resourceType: ApprovalResourceTypeEnum;
}

export interface ApprovalTodoItem {
  approvalTaskId: string;
  approvalNodeId: string;
  approvalInstanceId: string;
  approvalId: string;
  resourceId: string;
  resourceName: string;
  resourceType: string;
  applicant: string;
  submitTime: number;
  approvalOperation: ApprovalOperationEnum;
  dataResult: ProcessStatusEnum;
}

export interface ApprovalOperationParams {
  id: string;
  nodeId: string;
  instanceId: string;
  attachmentIds: string[];
}

export interface ApprovalBackParams extends ApprovalOperationParams {
  returnToTaskId: string;
  returnReason: string;
}

export interface ApprovalAddSignParams extends ApprovalOperationParams {
  type: string;
  approverId: string;
  comment: string;
}
