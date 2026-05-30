import {
  ApprovalLevelDirectionEnum,
  ApprovalTypeEnum,
  EmptyApproverActionEnum,
  MultiApproverModeEnum,
  SameSubmitterActionEnum,
} from '@lib/shared/enums/process';
import { useI18n } from '@lib/shared/hooks/useI18n';
import type { ApprovalActionNode, ApprovalConditionBranch } from '@lib/shared/models/system/process';

import type { FilterForm } from '@/components/pure/crm-advance-filter/type';
import {
  addConditionBranch,
  insertNodeAfterNode,
  insertNodeToConditionBranch,
} from '@/components/business/crm-flow/dsl/actions';
import {
  createActionNode,
  createConditionBranch,
  createConditionGroupNode,
  createElseBranch,
  createEndNode,
  createStartNode,
} from '@/components/business/crm-flow/dsl/factory';
import { findBranchLocation, findNodeLocation } from '@/components/business/crm-flow/dsl/queries';
import type { FlowNode, FlowSchema } from '@/components/business/crm-flow/types';

import { resolveApprovalActionNodeDefaults } from '@/config/process';

const { t } = useI18n();

// 创建审批动作节点
export function createApprovalActionNode(approvalType: ApprovalTypeEnum = ApprovalTypeEnum.MANUAL): ApprovalActionNode {
  const defaults = resolveApprovalActionNodeDefaults(approvalType);
  return createActionNode<ApprovalActionNode>({
    name: defaults.name,
    description: defaults.description,
    actionType: 'approval',
    approvalType,
    approverType: null,
    approverDirection: ApprovalLevelDirectionEnum.BOTTOM_UP,
    approverList: [],
    approverSelectedList: [],
    multiApproverMode: MultiApproverModeEnum.ALL,
    emptyApproverAction: EmptyApproverActionEnum.AUTO_PASS,
    fallbackApprover: null,
    sameSubmitterAction: SameSubmitterActionEnum.SKIP,
    ccType: null,
    ccDirection: ApprovalLevelDirectionEnum.BOTTOM_UP,
    ccList: [],
    ccSelectedList: [],
  });
}

// 新建流程的默认骨架：开始 -> 审批 -> 结束
export function createDefaultFlow(startDescription: string): FlowSchema {
  return {
    nodes: [createStartNode({ description: startDescription }), createApprovalActionNode(), createEndNode()],
  };
}

// 创建 if 分支：业务上要求每个条件分支默认带一个审批节点
export function createApprovalConditionBranch(partial: Partial<ApprovalConditionBranch> = {}): ApprovalConditionBranch {
  return createConditionBranch<ApprovalConditionBranch>({
    ...partial,
    description: partial.description ?? t('process.process.flow.conditionUnset'),
    children: partial.children ?? [createApprovalActionNode()],
  });
}

// 创建条件组：默认包含 if 分支和 else 分支，且都挂审批节点
function createApprovalConditionGroupNode() {
  return createConditionGroupNode({
    branches: [
      createApprovalConditionBranch(),
      createElseBranch<ApprovalConditionBranch>({
        children: [createApprovalActionNode()],
      }),
    ],
  });
}

// 如果插入条件组前方已有审批节点，把该审批节点迁移到 else 分支作为兜底分支
function bindElseBranchFallbackNode(
  groupNode: ReturnType<typeof createApprovalConditionGroupNode>,
  fallbackNode?: FlowNode
) {
  if (fallbackNode?.type !== 'action') {
    return;
  }

  const elseBranch = groupNode.branches.find((branch) => branch.isElse);
  if (!elseBranch) {
    return;
  }

  elseBranch.children = [fallbackNode];
}

// 在创建条件组时顺手把兜底审批节点挂到 else 分支，减少主链/分支侧的重复逻辑
function createApprovalConditionGroupWithFallback(fallbackNode?: FlowNode) {
  const conditionGroupNode = createApprovalConditionGroupNode();
  bindElseBranchFallbackNode(conditionGroupNode, fallbackNode);
  return conditionGroupNode;
}

// 在主链锚点后插入条件组：
// 1. 若后继是审批节点，直接替换并迁移到 else，避免主链重复审批
// 2. 否则按普通插入处理
function insertApprovalConditionGroupAfterNode(flowSchema: FlowSchema, anchorNodeId: string) {
  const location = findNodeLocation(flowSchema.nodes, anchorNodeId);
  if (!location) {
    return;
  }

  const nextNode = location.container[location.index + 1];
  const conditionGroupNode = createApprovalConditionGroupWithFallback(nextNode);

  if (nextNode?.type === 'action') {
    location.container.splice(location.index + 1, 1, conditionGroupNode);
    return;
  }

  insertNodeAfterNode(flowSchema, anchorNodeId, conditionGroupNode);
}

// 在分支内插入条件组
function insertApprovalConditionGroupToBranch(flowSchema: FlowSchema, groupId: string, branchId: string) {
  const location = findBranchLocation(flowSchema.nodes, branchId);
  if (!location || location.group.id !== groupId) {
    return;
  }

  const firstNode = location.branch.children[0];
  const conditionGroupNode = createApprovalConditionGroupWithFallback(firstNode);

  if (firstNode?.type === 'action') {
    location.branch.children.splice(0, 1, conditionGroupNode);
    return;
  }

  location.branch.children.unshift(conditionGroupNode);
}

// “+” 弹窗的新增操作：
// - 新增条件组：按主链/分支分别走条件组插入逻辑
// - 新增审批节点：按主链/分支分别插入审批节点
export function insertFromAnchor(payload: {
  flowSchema: FlowSchema;
  type: 'action' | 'condition-group';
  anchorNodeId: string | null;
  anchorBranch: { groupId: string; branchId: string } | null;
  actionApprovalType?: ApprovalTypeEnum;
}) {
  if (payload.type === 'condition-group') {
    if (payload.anchorBranch) {
      insertApprovalConditionGroupToBranch(
        payload.flowSchema,
        payload.anchorBranch.groupId,
        payload.anchorBranch.branchId
      );
      return;
    }

    if (!payload.anchorNodeId) {
      return;
    }

    insertApprovalConditionGroupAfterNode(payload.flowSchema, payload.anchorNodeId);
    return;
  }

  const actionNode = createApprovalActionNode(payload.actionApprovalType ?? ApprovalTypeEnum.MANUAL);
  if (payload.anchorBranch) {
    insertNodeToConditionBranch(
      payload.flowSchema,
      payload.anchorBranch.groupId,
      payload.anchorBranch.branchId,
      actionNode
    );
    return;
  }

  if (!payload.anchorNodeId) {
    return;
  }

  insertNodeAfterNode(payload.flowSchema, payload.anchorNodeId, actionNode);
}

// 条件组新增 if 分支时，默认仍然补一个审批节点，保持分支规则一致
export function addApprovalConditionBranch(flowSchema: FlowSchema, groupId: string) {
  addConditionBranch(flowSchema, groupId, createApprovalConditionBranch());
}

// 是否已设置条件
export function hasConfiguredCondition(conditionConfig?: FilterForm) {
  return conditionConfig?.conditions?.some((item) => item.name) ?? false;
}

export function resolveConditionDescription(conditionConfig?: FilterForm) {
  return hasConfiguredCondition(conditionConfig)
    ? t('process.process.flow.conditionConfigured')
    : t('process.process.flow.conditionUnset');
}
