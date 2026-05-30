/** DSL 守卫：定义节点与分支的操作约束规则 */
import type { ConditionBranch, ConditionGroupNode, FlowNode } from '../types';

export function canDeleteNode(node: FlowNode): boolean {
  return node.type !== 'start' && node.type !== 'end';
}

export function canEditNode(node: FlowNode): boolean {
  return node.type !== 'start' && node.type !== 'end';
}

export function canAddAfterNode(node: FlowNode): boolean {
  return node.type !== 'end';
}

export function canDeleteBranch(group: ConditionGroupNode, branch: ConditionBranch): boolean {
  if (branch.isElse) {
    return false;
  }

  return group.branches.length > 1;
}

export function shouldRemoveWholeGroupWhenDeleteIf(group: ConditionGroupNode): boolean {
  const ifBranchCount = group.branches.filter((item) => !item.isElse).length;
  return ifBranchCount <= 1;
}
