import { ApprovalLevelDirectionEnum, ApprovalNodeTypeEnum } from '@lib/shared/enums/process';
import { useI18n } from '@lib/shared/hooks/useI18n';
import type {
  ApprovalActionNode,
  ApprovalConditionBranch,
  ApprovalNodeLinkResponse,
  ApprovalProcessApproverNode,
  ApprovalProcessNode,
} from '@lib/shared/models/system/process';

import {
  createActionNode,
  createConditionGroupNode,
  createElseBranch,
  createEndNode,
  createStartNode,
} from '@/components/business/crm-flow/dsl/factory';
import type { FlowNode, FlowSchema } from '@/components/business/crm-flow/types';

import { resolveApprovalActionNodeDescription } from '@/config/process';

import { createApprovalConditionBranch, createDefaultFlow, resolveConditionDescription } from './index';

const { t } = useI18n();

// 后端条件节点里，CONDITION / DEFAULT 这一组在前端对应一个条件组
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

// 后端平铺节点的公共字段
function createProcessNodeBase(node: FlowNode, sort: number) {
  return {
    id: node.id,
    name: node.name,
    number: node.number,
    sort,
  };
}

// 前端审批节点 -> 后端审批节点
// 这里只保留真正的业务字段，不带 invalid / selectedList 这类前端展示态
function createProcessActionNode(node: ApprovalActionNode): ApprovalProcessNode {
  return {
    ...createProcessNodeBase(node, 0),
    nodeType: ApprovalNodeTypeEnum.APPROVER,
    approvalType: node.approvalType,
    approverType: node.approverType,
    approverDirection: node.approverDirection,
    approverList: node.approverList ?? [],
    multiApproverMode: node.multiApproverMode,
    emptyApproverAction: node.emptyApproverAction,
    fallbackApprover: node.fallbackApprover ?? '',
    fallbackApproverName: node.fallbackApproverName,
    sameSubmitterAction: node.sameSubmitterAction,
    ccType: node.ccType,
    ccDirection: node.ccDirection,
    ccList: node.ccList ?? [],
    passPostConfig: node.passPostConfig,
    rejectPostConfig: node.rejectPostConfig,
    fieldPermissions: node.fieldPermissions,
  };
}

// 前端分支卡片 -> 后端条件分支节点
// if 分支是 CONDITION，else 分支是 DEFAULT
function createProcessBranchNode(branch: ApprovalConditionBranch, sort: number): ApprovalProcessNode {
  return {
    id: branch.id,
    name: branch.name,
    number: branch.number,
    nodeType: branch.isElse ? ApprovalNodeTypeEnum.DEFAULT : ApprovalNodeTypeEnum.CONDITION,
    sort,
    ...(branch.isElse ? {} : { conditionConfig: branch.conditionConfig }),
  };
}

// 开始/结束节点字段很少，这里统一转成后端基础节点
function createProcessNode(node: FlowNode): ApprovalProcessNode {
  if (node.type === 'action') {
    return createProcessActionNode(node as ApprovalActionNode);
  }

  return {
    ...createProcessNodeBase(node, 0),
    nodeType: node.type === 'start' ? ApprovalNodeTypeEnum.START : ApprovalNodeTypeEnum.END,
  };
}

// 连接线 id 直接用 from_to 拼出来，方便调试时一眼看出边的方向
function pushLink(links: ApprovalNodeLinkResponse[], fromNodeId: string, toNodeId: string) {
  links.push({
    id: `${fromNodeId}_${toNodeId}`,
    fromNodeId,
    toNodeId,
  });
}

// 这里用笛卡尔积补边，主要是为了处理：
// - 多个条件分支统一流向同一段公共尾链
// - 某个节点需要同时连向多个入口节点
function linkNodes(links: ApprovalNodeLinkResponse[], fromNodeIds: string[], toNodeIds: string[]) {
  fromNodeIds.forEach((fromNodeId) => {
    toNodeIds.forEach((toNodeId) => {
      pushLink(links, fromNodeId, toNodeId);
    });
  });
}

// 从尾到头序列化，能自然处理“条件组后面的公共尾链”链接关系。
// 返回值表示：这一段序列最前面的入口节点 id 列表。
// 调用方拿到这些入口 id 后，就知道上一段节点应该连向谁。
function serializeFlowSequence(
  nodes: FlowNode[],
  nextEntryIds: string[],
  serializedNodes: ApprovalProcessNode[],
  serializedLinks: ApprovalNodeLinkResponse[]
): string[] {
  // currentEntryIds 表示“当前这段流程之后，下一跳应该连到哪些节点”。
  // 一开始由外层传入；随着倒序遍历，它会不断被前一个节点改写。
  let currentEntryIds = nextEntryIds;

  for (let index = nodes.length - 1; index >= 0; index -= 1) {
    const currentNode = nodes[index];

    if (currentNode.type === 'condition-group') {
      // 条件组自己不会下发成一个独立后端节点，
      // 后端只保留各个 CONDITION / DEFAULT 分支节点。
      const branchEntryIds: string[] = [];
      // continuationEntryIds 是“整个条件组执行完后应该继续流向哪里”。
      const continuationEntryIds = currentEntryIds;

      (currentNode.branches as ApprovalConditionBranch[]).forEach((branch, branchIndex) => {
        const branchNode = createProcessBranchNode(branch, branchIndex);
        serializedNodes.push(branchNode);

        // 先递归序列化分支里的子链，拿到该分支真正的入口节点。
        const childEntryIds = serializeFlowSequence(
          branch.children,
          continuationEntryIds,
          serializedNodes,
          serializedLinks
        );
        if (childEntryIds.length) {
          // 分支节点先连到自己分支里的第一个节点。
          linkNodes(serializedLinks, [branchNode.id], childEntryIds);
        } else if (continuationEntryIds.length) {
          // 空分支没有子节点时，分支节点直接连到公共后继。
          linkNodes(serializedLinks, [branchNode.id], continuationEntryIds);
        }

        // 对上一层来说，条件组的“入口”就是每个分支节点本身。
        branchEntryIds.push(branchNode.id);
      });

      currentEntryIds = branchEntryIds;
    } else {
      const processNode = createProcessNode(currentNode);
      serializedNodes.push(processNode);
      if (currentEntryIds.length) {
        // 普通节点总是直接连向它后面的入口节点。
        linkNodes(serializedLinks, [processNode.id], currentEntryIds);
      }
      currentEntryIds = [processNode.id];
    }
  }

  return currentEntryIds;
}

// serializedNodes 是倒序 push 进去的，真正返回给后端前，
// 还是按前端 DSL 的自然阅读顺序重新排一次，避免回显时左右分支漂移。
function collectSerializedNodeOrder(nodes: FlowNode[]): string[] {
  return nodes.flatMap((node) => {
    if (node.type !== 'condition-group') {
      return [node.id];
    }

    return (node.branches as ApprovalConditionBranch[]).flatMap((branch) => [
      branch.id,
      ...collectSerializedNodeOrder(branch.children),
    ]);
  });
}

// 前端画布节点 -> 后端平铺节点 + 连接线配置。
export function serializeFlowNodes(nodes: FlowNode[]): {
  nodes: ApprovalProcessNode[];
  links: ApprovalNodeLinkResponse[];
} {
  // 先收集“完整的节点和边”，顺序暂时不重要。
  const serializedNodes: ApprovalProcessNode[] = [];
  const serializedLinks: ApprovalNodeLinkResponse[] = [];

  serializeFlowSequence(nodes, [], serializedNodes, serializedLinks);
  // 再按前端可视顺序重排，保证后端再回给前端时左右分支顺序稳定。
  const serializedNodeOrder = collectSerializedNodeOrder(nodes);
  const serializedNodeMap = new Map(serializedNodes.map((node) => [node.id, node]));
  const serializedNodeOrderIndexMap = new Map(serializedNodeOrder.map((nodeId, index) => [nodeId, index]));

  return {
    nodes: serializedNodeOrder
      .map((nodeId) => serializedNodeMap.get(nodeId))
      .filter((node): node is ApprovalProcessNode => Boolean(node)),
    // links 本身没有“前后顺序”语义，但按节点顺序排一下更方便接口排查，
    // 同时也能减少后端存储/返回时顺序抖动。
    links: [...serializedLinks].sort((leftLink, rightLink) => {
      const leftFromIndex = serializedNodeOrderIndexMap.get(leftLink.fromNodeId) ?? Number.MAX_SAFE_INTEGER;
      const rightFromIndex = serializedNodeOrderIndexMap.get(rightLink.fromNodeId) ?? Number.MAX_SAFE_INTEGER;
      if (leftFromIndex !== rightFromIndex) {
        return leftFromIndex - rightFromIndex;
      }

      const leftToIndex = serializedNodeOrderIndexMap.get(leftLink.toNodeId) ?? Number.MAX_SAFE_INTEGER;
      const rightToIndex = serializedNodeOrderIndexMap.get(rightLink.toNodeId) ?? Number.MAX_SAFE_INTEGER;
      return leftToIndex - rightToIndex;
    }),
  };
}

function mapSelectedOptions(options?: { id: string; name: string }[]) {
  return options?.map((item) => ({ id: item.id, name: item.name })) ?? [];
}

// 后端审批节点 -> 前端审批节点。
// 这里会把前端展示所需的 selectedList 一并补齐，方便右侧表单直接回显。
function deserializeApproverNode(node: ApprovalProcessApproverNode): ApprovalActionNode {
  const approverList = node.approverList ?? [];
  const ccList = node.ccList ?? [];
  const fallbackApprover = node.fallbackApprover ?? null;

  return createActionNode<ApprovalActionNode>({
    id: node.id,
    type: 'action',
    actionType: 'approval',
    name: node.name,
    number: node.number,
    description: resolveApprovalActionNodeDescription(node.approvalType, node.approverType ?? null),
    approvalType: node.approvalType,
    approverType: node.approverType ?? null,
    approverDirection: node.approverDirection ?? ApprovalLevelDirectionEnum.BOTTOM_UP,
    approverList,
    approverSelectedList: mapSelectedOptions(node.approverSelectOptions),
    multiApproverMode: node.multiApproverMode,
    emptyApproverAction: node.emptyApproverAction,
    fallbackApprover,
    sameSubmitterAction: node.sameSubmitterAction,
    emptyApproverSelectedList:
      fallbackApprover && node.fallbackApproverName ? [{ id: fallbackApprover, name: node.fallbackApproverName }] : [],
    ccType: node.ccType ?? null,
    ccDirection: node.ccDirection ?? ApprovalLevelDirectionEnum.BOTTOM_UP,
    ccList,
    ccSelectedList: mapSelectedOptions(node.ccSelectOptions),
    passPostConfig: node.passPostConfig,
    rejectPostConfig: node.rejectPostConfig,
    fieldPermissions: node.fieldPermissions,
  });
}

// 同层节点的稳定顺序全部交给 sort 字段处理。
function sortNodeIds(nodeIds: string[], nodeMap: Map<string, ApprovalProcessNode>) {
  return [...nodeIds].sort((leftId, rightId) => {
    const leftNode = nodeMap.get(leftId);
    const rightNode = nodeMap.get(rightId);

    return (leftNode?.sort ?? 0) - (rightNode?.sort ?? 0);
  });
}

// from -> to[]，给“往后走”时使用。
function buildOutgoingLinkMap(links: ApprovalNodeLinkResponse[]) {
  return links.reduce((map, link) => {
    const currentTargets = map.get(link.fromNodeId) ?? [];
    currentTargets.push(link.toNodeId);
    map.set(link.fromNodeId, currentTargets);
    return map;
  }, new Map<string, string[]>());
}

// to -> from[]，给“找公共入口/前驱”时使用。
function buildIncomingLinkMap(links: ApprovalNodeLinkResponse[]) {
  return links.reduce((map, link) => {
    const currentSources = map.get(link.toNodeId) ?? [];
    currentSources.push(link.fromNodeId);
    map.set(link.toNodeId, currentSources);
    return map;
  }, new Map<string, string[]>());
}

function collectReachableNodeIds(
  startNodeId: string,
  outgoingLinkMap: Map<string, string[]>,
  visited = new Set<string>()
) {
  // 从某个分支节点出发，把后面所有能走到的节点都收出来。
  // 后面找“多个分支的公共尾链”时会用到。
  const nextNodeIds = outgoingLinkMap.get(startNodeId) ?? [];

  nextNodeIds.forEach((nextNodeId) => {
    if (visited.has(nextNodeId)) {
      return;
    }

    visited.add(nextNodeId);
    collectReachableNodeIds(nextNodeId, outgoingLinkMap, visited);
  });

  return visited;
}

function findSharedEntryNodeIds(
  branchNodeIds: string[],
  outgoingLinkMap: Map<string, string[]>,
  incomingLinkMap: Map<string, string[]>,
  nodeMap: Map<string, ApprovalProcessNode>
) {
  // 先找“每个分支都能到达的节点”，也就是多个分支共享的后续链路。
  const reachableNodeSets = branchNodeIds.map((branchNodeId) => collectReachableNodeIds(branchNodeId, outgoingLinkMap));
  if (!reachableNodeSets.length) {
    return [];
  }

  const sharedNodeIds = [...reachableNodeSets[0]].filter((nodeId) =>
    reachableNodeSets.every((reachableNodeSet) => reachableNodeSet.has(nodeId))
  );
  const sharedNodeIdSet = new Set(sharedNodeIds);

  return sortNodeIds(
    sharedNodeIds.filter((nodeId) => {
      const predecessorIds = incomingLinkMap.get(nodeId) ?? [];
      // 这里要的是“共享链的入口节点”，所以要求它的前驱不能也在共享链内部。
      return predecessorIds.every((predecessorId) => !sharedNodeIdSet.has(predecessorId));
    }),
    nodeMap
  );
}

// 后端单个节点 -> 前端单个节点。
// 条件分支节点不在这里处理，而是在 deserializeProcessNodeList 中整组恢复为 condition-group。
function deserializeProcessNode(node: ApprovalProcessNode): FlowNode {
  if (node.nodeType === ApprovalNodeTypeEnum.START) {
    return createStartNode({
      id: node.id,
      name: node.name,
      number: node.number,
    });
  }

  if (node.nodeType === ApprovalNodeTypeEnum.END) {
    return createEndNode({
      id: node.id,
      name: node.name,
      number: node.number,
    });
  }

  return deserializeApproverNode(node as ApprovalProcessApproverNode);
}

function deserializeProcessNodeList(
  entryNodeIds: string[],
  nodeMap: Map<string, ApprovalProcessNode>,
  outgoingLinkMap: Map<string, string[]>,
  incomingLinkMap: Map<string, string[]>,
  stopNodeIds = new Set<string>()
): FlowNode[] {
  // stopNodeIds 表示“这批节点属于条件组后面的公共尾链”，
  // 当前分支递归到这里就该停，不能把公共尾链重复塞进每个分支里。
  const activeEntryNodeIds = sortNodeIds(
    entryNodeIds.filter((nodeId) => !stopNodeIds.has(nodeId)),
    nodeMap
  );
  if (!activeEntryNodeIds.length) {
    return [];
  }

  if (activeEntryNodeIds.every((nodeId) => isConditionBranchNode(nodeMap.get(nodeId)!))) {
    // 入口如果全是 CONDITION / DEFAULT，说明这里在前端应该恢复成一个条件组。
    const sharedEntryNodeIds = findSharedEntryNodeIds(activeEntryNodeIds, outgoingLinkMap, incomingLinkMap, nodeMap);
    const sharedEntryNodeIdSet = new Set(sharedEntryNodeIds);

    const branches = activeEntryNodeIds.map((branchNodeId) => {
      const branchNode = nodeMap.get(branchNodeId)!;
      const branchChildEntryNodeIds = sortNodeIds(outgoingLinkMap.get(branchNodeId) ?? [], nodeMap);
      // 分支内部继续递归，但碰到公共尾链入口就停止，避免把公共节点吞进分支 children。
      const children = deserializeProcessNodeList(
        branchChildEntryNodeIds,
        nodeMap,
        outgoingLinkMap,
        incomingLinkMap,
        sharedEntryNodeIdSet
      );

      if (branchNode.nodeType === ApprovalNodeTypeEnum.DEFAULT) {
        return createElseBranch<ApprovalConditionBranch>({
          id: branchNode.id,
          name: branchNode.name,
          number: branchNode.number,
          description: toConditionBranchDescription(branchNode),
          children,
        });
      }

      return createApprovalConditionBranch({
        id: branchNode.id,
        name: branchNode.name,
        number: branchNode.number,
        description: toConditionBranchDescription(branchNode),
        conditionConfig:
          branchNode.nodeType === ApprovalNodeTypeEnum.CONDITION ? branchNode.conditionConfig : undefined,
        children,
      });
    });

    return [
      // 先恢复整个条件组，再把条件组后面的公共尾链继续拼回主链。
      createConditionGroupNode({ branches }),
      ...deserializeProcessNodeList(sharedEntryNodeIds, nodeMap, outgoingLinkMap, incomingLinkMap, stopNodeIds),
    ];
  }

  // 普通主链场景：每次取当前入口的第一个节点，顺着后继继续往下还原。
  const [currentNodeId] = activeEntryNodeIds;
  const currentProcessNode = nodeMap.get(currentNodeId);
  if (!currentProcessNode) {
    return [];
  }

  const nextEntryNodeIds = sortNodeIds(outgoingLinkMap.get(currentNodeId) ?? [], nodeMap);
  return [
    deserializeProcessNode(currentProcessNode),
    ...deserializeProcessNodeList(nextEntryNodeIds, nodeMap, outgoingLinkMap, incomingLinkMap, stopNodeIds),
  ];
}

// 对外暴露的总入口：把后端详情里的平铺 nodes + links 恢复成流程设计器使用的 FlowSchema
export function deserializeProcessNodes(
  nodes: ApprovalProcessNode[],
  links: ApprovalNodeLinkResponse[],
  startDescription: string
): FlowSchema {
  // 先把后端平铺结构整理成更适合查找的索引结构。
  const nodeMap = new Map(nodes.map((node) => [node.id, node]));
  const outgoingLinkMap = buildOutgoingLinkMap(links);
  const incomingLinkMap = buildIncomingLinkMap(links);
  const startProcessNode = nodes.find((node) => node.nodeType === ApprovalNodeTypeEnum.START);
  if (!startProcessNode) {
    // 后端异常或空数据时，前端仍然回到一个可编辑的默认流程。
    return createDefaultFlow(startDescription);
  }

  const flowNodes = deserializeProcessNodeList([startProcessNode.id], nodeMap, outgoingLinkMap, incomingLinkMap);
  if (!flowNodes.length) {
    return createDefaultFlow(startDescription);
  }

  // 开始节点描述以前端当前文案为准，不直接吃后端存量描述。
  const startFlowNode = flowNodes.find((node) => node.type === 'start');
  if (startFlowNode?.type === 'start') {
    startFlowNode.description = startDescription;
  }

  return { nodes: flowNodes };
}
