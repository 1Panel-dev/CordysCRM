/** DSL actions：实现节点插入、删除、更新和分支调整（写操作） */
import type { ConditionBranch, FlowNode, FlowSchema } from '../types';
import { createConditionBranch } from './factory';
import { canDeleteBranch, canDeleteNode, shouldRemoveWholeGroupWhenDeleteIf } from './guards';
import {
  findBranchLocation,
  findConditionGroupById,
  findNodeById,
  findNodeLocation as findLocation,
  findNodeLocation,
} from './queries';

// 在锚点节点后插入节点（活动节点或条件组节点）
export function insertNodeAfterNode(schema: FlowSchema, anchorNodeId: string, node: FlowNode): boolean {
  const location = findLocation(schema.nodes, anchorNodeId);
  if (!location) {
    return false;
  }

  location.container.splice(location.index + 1, 0, node);
  return true;
}

// 在条件组里新增一个 if 分支，插到 else 之前
export function addConditionBranch(schema: FlowSchema, groupId: string, partial?: Partial<ConditionBranch>): boolean {
  const groupNode = findConditionGroupById(schema.nodes, groupId);
  if (!groupNode) {
    return false;
  }

  const elseIndex = groupNode.branches.findIndex((item) => item.isElse);

  const nextIfBranch = createConditionBranch(partial);

  if (elseIndex >= 0) {
    groupNode.branches.splice(elseIndex, 0, nextIfBranch);
  } else {
    groupNode.branches.push(nextIfBranch);
  }

  return true;
}

// 向指定条件分支 children 头部插入节点（用于分支内新增动作/条件组）
export function insertNodeToConditionBranch(
  schema: FlowSchema,
  groupId: string,
  branchId: string,
  node: FlowNode
): boolean {
  const location = findBranchLocation(schema.nodes, branchId);
  if (!location || location.group.id !== groupId) {
    return false;
  }

  location.branch.children.unshift(node);
  return true;
}

// 删除节点（受 guards 限制，例如开始/结束节点不可删）
export function deleteNodeById(schema: FlowSchema, nodeId: string): boolean {
  const location = findNodeLocation(schema.nodes, nodeId);
  if (!location || !canDeleteNode(location.node)) {
    return false;
  }

  location.container.splice(location.index, 1);
  return true;
}

// 删除条件分支：先走 guard，命中规则时会删除整个条件组
export function deleteConditionBranch(schema: FlowSchema, groupId: string, branchId: string): boolean {
  const groupNode = findConditionGroupById(schema.nodes, groupId);
  if (!groupNode) {
    return false;
  }

  const branchIndex = groupNode.branches.findIndex((item) => item.id === branchId);
  if (branchIndex < 0) {
    return false;
  }

  const targetBranch = groupNode.branches[branchIndex];
  if (!canDeleteBranch(groupNode, targetBranch)) {
    return false;
  }

  if (!targetBranch.isElse && shouldRemoveWholeGroupWhenDeleteIf(groupNode)) {
    return deleteNodeById(schema, groupId);
  }

  groupNode.branches.splice(branchIndex, 1);
  return true;
}

// 按 id 局部更新节点
export function updateNodeById(schema: FlowSchema, nodeId: string, patch: Partial<FlowNode>): FlowNode | null {
  const node = findNodeById(schema.nodes, nodeId);
  if (!node) {
    return null;
  }

  Object.assign(node, patch);
  return node;
}
