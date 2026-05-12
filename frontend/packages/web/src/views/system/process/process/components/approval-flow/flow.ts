import { cloneDeep } from 'lodash-es';

import {
  ApprovalNodeTypeEnum,
  ApprovalTypeEnum,
  ApproverTypeEnum,
  EmptyApproverActionEnum,
  MultiApproverModeEnum,
  SameSubmitterActionEnum,
} from '@lib/shared/enums/process';
import { useI18n } from '@lib/shared/hooks/useI18n';
import type {
  ApprovalActionNode,
  ApprovalConditionBranch,
  ApprovalProcessApproverNode,
  ApprovalProcessNode,
} from '@lib/shared/models/system/process';

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

import { resolveApprovalActionNodeDefaults, resolveApprovalActionNodeDescription } from '@/config/process';

const { t } = useI18n();

// 创建审批动作节点
export function createApprovalActionNode(approvalType: ApprovalTypeEnum = ApprovalTypeEnum.MANUAL): ApprovalActionNode {
  const defaults = resolveApprovalActionNodeDefaults(approvalType);
  return createActionNode<ApprovalActionNode>({
    name: defaults.name,
    description: defaults.description,
    actionType: 'approval',
    approvalType,
    approverType: ApproverTypeEnum.SPECIFIED_MEMBER,
    approverList: [],
    approverSelectedList: [],
    multiApproverMode: MultiApproverModeEnum.ALL,
    emptyApproverAction: EmptyApproverActionEnum.AUTO_PASS,
    fallbackApprover: null,
    sameSubmitterAction: SameSubmitterActionEnum.SKIP,
    ccType: null,
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

// 在主链锚点后插入条件组：
// 1. 若后继是审批节点，直接替换并迁移到 else，避免主链重复审批
// 2. 否则按普通插入处理
function insertApprovalConditionGroupAfterNode(flowSchema: FlowSchema, anchorNodeId: string) {
  const location = findNodeLocation(flowSchema.nodes, anchorNodeId);
  if (!location) {
    return;
  }

  const nextNode = location.container[location.index + 1];
  const conditionGroupNode = createApprovalConditionGroupNode();
  bindElseBranchFallbackNode(conditionGroupNode, nextNode);

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
  const conditionGroupNode = createApprovalConditionGroupNode();
  bindElseBranchFallbackNode(conditionGroupNode, firstNode);

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

export function resolveConditionDescription(conditionConfig?: FilterForm) {
  return conditionConfig?.list?.some((item) => item.dataIndex)
    ? t('process.process.flow.conditionConfigured')
    : t('process.process.flow.conditionUnset');
}

// 后端条件节点树里，CONDITION / DEFAULT 这一组在前端对应一个条件组
function isConditionBranchNode(node: ApprovalProcessNode) {
  return node.nodeType === ApprovalNodeTypeEnum.CONDITION || node.nodeType === ApprovalNodeTypeEnum.DEFAULT;
}

// 画布上分支卡片的描述不直接依赖后端文案，而是根据节点类型和条件配置重新计算
function toConditionBranchDescription(node: ApprovalProcessNode) {
  if (node.nodeType === ApprovalNodeTypeEnum.DEFAULT) {
    return t('crmFlow.elseDescription');
  }

  return node.nodeType === ApprovalNodeTypeEnum.CONDITION ? resolveConditionDescription(node.conditionConfig) : '';
}

function createProcessNodeBase(node: FlowNode, sort: number) {
  return {
    id: node.id,
    name: node.name,
    sort,
    children: [] as ApprovalProcessNode[],
  };
}

// 前端画布节点 -> 后端审批流节点树
// 条件组在后端没有独立节点类型，所以这里会把每个分支展开成 CONDITION / DEFAULT 节点，
export function serializeFlowNodes(nodes: FlowNode[]): ApprovalProcessNode[] {
  if (!nodes.length) {
    return [];
  }
  // TODO lmy 修改结构

  const [firstNode, ...restNodes] = nodes;

  if (firstNode.type === 'condition-group') {
    return (firstNode.branches as ApprovalConditionBranch[]).map((branch, index) => {
      const branchTail = [...cloneDeep(branch.children), ...cloneDeep(restNodes)];
      return {
        id: branch.id,
        name: branch.name,
        nodeType: branch.isElse ? ApprovalNodeTypeEnum.DEFAULT : ApprovalNodeTypeEnum.CONDITION,
        sort: index,
        children: serializeFlowNodes(branchTail),
        ...(branch.isElse ? {} : { conditionConfig: branch.conditionConfig }),
      };
    });
  }

  const actionNode = firstNode.type === 'action' ? (firstNode as ApprovalActionNode) : null;
  const processNode: ApprovalProcessNode = actionNode
    ? {
        ...createProcessNodeBase(firstNode, 0),
        nodeType: ApprovalNodeTypeEnum.APPROVER,
        approvalType: actionNode.approvalType,
        approverType: actionNode.approverType,
        approverList: actionNode.approverList ?? [],
        multiApproverMode: actionNode.multiApproverMode,
        emptyApproverAction: actionNode.emptyApproverAction,
        fallbackApprover: actionNode.fallbackApprover ?? '',
        fallbackApproverName: actionNode.fallbackApproverName,
        sameSubmitterAction: actionNode.sameSubmitterAction,
        ccType: actionNode.ccType,
        ccList: actionNode.ccList ?? [],
        passPostConfig: actionNode.passPostConfig,
        rejectPostConfig: actionNode.rejectPostConfig,
        fieldPermissions: actionNode.fieldPermissions,
      }
    : {
        ...createProcessNodeBase(firstNode, 0),
        nodeType: firstNode.type === 'start' ? ApprovalNodeTypeEnum.START : ApprovalNodeTypeEnum.END,
      };

  processNode.children = serializeFlowNodes(restNodes);
  return [processNode];
}

// 后端节点树 -> 前端审批节点
// 仅前端使用的 selectedList 等展示态补齐
function deserializeApproverNode(node: ApprovalProcessApproverNode): ApprovalActionNode {
  const approverList = node.approverList ?? [];
  const ccList = node.ccList ?? [];
  const fallbackApprover = node.fallbackApprover ?? null;

  return createActionNode<ApprovalActionNode>({
    id: node.id,
    type: 'action',
    actionType: 'approval',
    name: node.name,
    description: resolveApprovalActionNodeDescription(node.approvalType, node.approverType ?? undefined),
    approvalType: node.approvalType,
    approverType: node.approverType ?? ApproverTypeEnum.SPECIFIED_MEMBER,
    approverList,
    approverSelectedList: node.approverSelectOptions?.map((item) => ({ id: item.id, name: item.name })) ?? [],
    multiApproverMode: node.multiApproverMode,
    emptyApproverAction: node.emptyApproverAction,
    fallbackApprover,
    sameSubmitterAction: node.sameSubmitterAction,
    emptyApproverSelectedList:
      fallbackApprover && node.fallbackApproverName ? [{ id: fallbackApprover, name: node.fallbackApproverName }] : [],
    ccType: node.ccType ?? null,
    ccList,
    ccSelectedList: node.ccSelectOptions?.map((item) => ({ id: item.id, name: item.name })) ?? [],
    passPostConfig: node.passPostConfig,
    rejectPostConfig: node.rejectPostConfig,
    fieldPermissions: node.fieldPermissions,
  });
}

// 条件组序列化时会把主链尾部复制到每个分支里，反序列化时要把这段公共尾链再还原出来
function getCommonTailLength(chains: FlowNode[][]) {
  if (!chains.length || chains.some((chain) => !chain.length)) {
    return 0;
  }

  let tailLength = 0;
  // 逐个比较每条分支链的尾部，找出为了适配后端树结构而重复拼接的公共尾链长度
  while (true) {
    let baseSignature = '';

    for (let index = 0; index < chains.length; index += 1) {
      const node = chains[index][chains[index].length - tailLength - 1];
      if (!node) {
        return tailLength;
      }

      const signature = `${node.type}:${node.id}`;
      if (index === 0) {
        baseSignature = signature;
      } else if (signature !== baseSignature) {
        return tailLength;
      }
    }

    tailLength += 1;
  }
}

// 后端节点树 -> 前端画布节点列表
// 其中最特殊的是条件组：后端是多个兄弟 CONDITION / DEFAULT 节点，前端需要重新拼成一个 group。
function deserializeProcessNodeList(nodes: ApprovalProcessNode[]): FlowNode[] {
  if (!nodes.length) {
    return [];
  }

  const sortedNodes = [...nodes].sort((a, b) => a.sort - b.sort);

  if (sortedNodes.every(isConditionBranchNode)) {
    const branchChains = sortedNodes.map((node) => deserializeProcessNodeList(node.children ?? []));
    const commonTailLength = getCommonTailLength(branchChains);
    const commonTail = commonTailLength ? cloneDeep(branchChains[0].slice(-commonTailLength)) : [];

    const branches = sortedNodes.map((node, index) => {
      const branchNodes = branchChains[index];
      const children =
        commonTailLength > 0 ? branchNodes.slice(0, Math.max(branchNodes.length - commonTailLength, 0)) : branchNodes;

      if (node.nodeType === ApprovalNodeTypeEnum.DEFAULT) {
        return createElseBranch<ApprovalConditionBranch>({
          id: node.id,
          name: node.name,
          description: toConditionBranchDescription(node),
          children,
        });
      }

      return createApprovalConditionBranch({
        id: node.id,
        name: node.name,
        description: toConditionBranchDescription(node),
        conditionConfig: node.nodeType === ApprovalNodeTypeEnum.CONDITION ? node.conditionConfig : undefined,
        children,
      });
    });

    return [createConditionGroupNode({ branches }), ...commonTail];
  }

  return sortedNodes.flatMap((node) => {
    let currentNode: FlowNode;
    if (node.nodeType === ApprovalNodeTypeEnum.START) {
      currentNode = createStartNode({
        id: node.id,
        name: node.name,
      });
    } else if (node.nodeType === ApprovalNodeTypeEnum.END) {
      currentNode = createEndNode({
        id: node.id,
        name: node.name,
      });
    } else {
      currentNode = deserializeApproverNode(node as ApprovalProcessApproverNode);
    }

    return [currentNode, ...deserializeProcessNodeList(node.children ?? [])];
  });
}

// 对外暴露的总入口：把后端详情里的 nodes 恢复成流程设计器使用的 FlowSchema
export function deserializeProcessNodes(nodes: ApprovalProcessNode[], startDescription: string): FlowSchema {
  const flowNodes = deserializeProcessNodeList(nodes);
  if (!flowNodes.length) {
    return createDefaultFlow(startDescription);
  }

  const startNode = flowNodes.find((node) => node.type === 'start');
  if (startNode?.type === 'start') {
    startNode.description = startDescription;
  }

  return { nodes: flowNodes };
}
