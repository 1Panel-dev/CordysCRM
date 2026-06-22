import {
  ApprovalLevelDirectionEnum,
  ApprovalOperationEnum,
  ApprovalTypeEnum,
  ApproverTypeEnum,
  ProcessStatusEnum,
  type ApprovalResourceTypeEnum,
} from '@lib/shared/enums/process';
import { RequestEnum } from '@lib/shared/enums/httpEnum';
import type { SelectedUsersItem } from './module';
import type { OptionDTO } from './business';
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
import type { AttachmentInfo } from '@cordys/web/src/components/business/crm-form-create/types';

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
  deleteExecute: boolean;
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
  fieldId: string | null;
  fieldValue: any;
  enable: boolean;
}

export interface ApprovalWebhookConfig {
  webHookDescribe: string;
  webHookEnable: boolean;
  webHookUrl: string;
  webHookMethod: RequestEnum;
  webHookHeader: string;
  webHookBody: string;
}

export interface ApprovalPostConfig {
  fieldUpdateConfigs: ApprovalFieldUpdateConfig[];
  webHookConfig?: ApprovalWebhookConfig;
}

// 后端审批流节点的基础结构
export interface ApprovalProcessNodeBase<TNodeType extends ApprovalNodeTypeEnum> extends BaseItem {
  nodeType: TNodeType;
  number?: string;
  sort: number;
}

// 审批节点里前后端共用的业务配置
export interface ApprovalNodeParticipantConfig {
  approvalType: ApprovalTypeEnum; // 审批类型：人工审批/自动通过/自动拒绝
  approverType: ApproverTypeEnum | null;
  approverDirection?: ApprovalLevelDirectionEnum;
  approverList: string[]; // 审批人配置列表：成员 ID / 角色 ID / 层级等
  approverSelectOptions?: OptionDTO[]; // 审批人选择项（用于前端回显）
  multiApproverMode: MultiApproverModeEnum; // 多人审批方式
  emptyApproverAction: EmptyApproverActionEnum; // 异常处理-审批人为空时动作
  fallbackApprover: string | null; // 异常处理-兜底审批人
  fallbackApproverName?: string;
  sameSubmitterAction: SameSubmitterActionEnum; // 异常处理-审批人与提交人相同时动作
  ccType: ApproverTypeEnum | null; // 抄送人
  ccDirection?: ApprovalLevelDirectionEnum;
  ccList: string[]; // 抄送人配置列表：成员 ID / 角色 ID / 层级等
  ccSelectOptions?: OptionDTO[]; // 抄送人选择项（用于前端回显）
  passPostConfig?: ApprovalPostConfig; // 审批通过后配置
  rejectPostConfig?: ApprovalPostConfig; // 审批驳回后配置
  fieldPermissions?: ApprovalFieldPermission[]; // 字段权限配置列表
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
  approverSelectedList?: SelectedUsersItem[]; // 前端展示用的审批人选中列表
  emptyApproverSelectedList?: SelectedUsersItem[]; // 异常处理-前端展示用的
  ccSelectedList?: SelectedUsersItem[]; // 前端展示用的抄送选中列表
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
  deleteExecute: boolean; // 删除执行
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

export interface ApprovalNodeLinkResponse {
  id: string;
  fromNodeId: string;
  toNodeId: string;
}

export interface ApprovalFlowNodeConfig {
  nodes: ApprovalProcessNode[];
  links: ApprovalNodeLinkResponse[];
}

export interface AddApprovalProcessParams extends BasicFormParams, MoreSettingsParams {
  enable: boolean;
  createNodeConfig?: ApprovalFlowNodeConfig;
  updateNodeConfig?: ApprovalFlowNodeConfig;
  deleteNodeConfig?: ApprovalFlowNodeConfig;
}

export interface UpdateApprovalProcessParams extends AddApprovalProcessParams {
  id: string;
}

export interface ApprovalProcessDetail extends UpdateApprovalProcessParams {
  permissions: PermissionItem[];
  id: string;
  number: string;
  optionMap?: Record<string, any[]>;
}

export interface ApprovalProcessForm {
  id: string;
  enable: boolean;
  createNodeConfig?: ApprovalFlowNodeConfig;
  updateNodeConfig?: ApprovalFlowNodeConfig;
  deleteNodeConfig?: ApprovalFlowNodeConfig;
  basicConfig: BasicFormParams;
  moreConfig: MoreSettingsParams;
}

export interface ApproverItem extends Pick<UserInfo, 'id' | 'name' | 'avatar'> {
  approveResult: ProcessStatusEnum;
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
  approvalFlowId: string;
  approvalFlowVersionId: string;
  resourceId: string;
  resourceName: string;
  resourceType: ApprovalResourceTypeEnum;
  applicant: string;
  submitTime: number;
  approvalOperation: ApprovalOperationEnum;
  dataResult: ProcessStatusEnum;
}

export interface ApprovalOperationParams {
  id: string;
  nodeId: string;
  instanceId: string;
  approverId: string;
  comment?: string;
  attachmentIds: string[];
  module: string;
}

export interface ApprovalBackParams extends ApprovalOperationParams {
  returnToNodeId: string;
}

export interface ApprovalAddSignParams extends ApprovalOperationParams {
  type: string;
  signApprover: string;
}

export interface BatchRejectApprovalParams {
  ids: string[];
  comment: string;
  attachmentIds: string[];
}

export interface BatchApprovalParams {
  ids: string[];
  comment?: string;
  attachmentIds: string[];
}

export interface TodoStatistic {
  total: number;
  quotation: number;
  contract: number;
  order: number;
  invoice: number;
}

export interface ApprovalDetail {
  id: string;
  submitterId: string;
  submitAvatar: string;
  submitter: string;
  submitTime: number;
  comment: string;
  result: string;
  approvalStatus: ProcessStatusEnum;
  currentNodeId: string;
  currentNodeFieldPermissions: string;
  nodes: ApprovalNode[];
}

export interface ApprovalNode {
  taskId: string;
  recordId: string;
  nodeId: string;
  nodeName: string;
  endNode: boolean;
  approverId: string;
  approver: string;
  approvalStatus: ProcessStatusEnum;
  approvalTime: number;
  comment: string;
  attachments: AttachmentInfo[];
  ccNodes: CcNode[];
  nodeRound: number; // 节点轮次
  taskNodes: ApprovalNodeTaskNode[];
  multiApproverMode: MultiApproverModeEnum; // 多人审批方式
  backNode: boolean; // 是否回退节点
  backReason: string; // 回退原因
  backAttachments: AttachmentInfo[]; // 回退附件
}

export interface ApprovalNodeTaskNode {
  taskId: string;
  approverId: string;
  approver: string;
  approverAvatar: string;
  approvalStatus: ProcessStatusEnum;
  recordId: string;
  comment: string;
  attachments: AttachmentInfo[];
  approvalTime: number;
  sign: boolean; // 是否加签任务
  signAction: boolean; // 是否操作加签
  signComment: string; // 加签备注
  signAttachments: AttachmentInfo[]; // 加签附件
}

export interface CcNode {
  ccUserId: string;
  ccUserName: string;
  ccUserAvatar: string;
}
