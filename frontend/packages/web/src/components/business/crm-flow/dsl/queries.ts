/** DSL queries：查找节点、定位容器等只读查询能力 */
import type { ConditionBranch, ConditionGroupNode, FlowNode } from '../types';

export interface NodeLocation<
  TNode extends FlowNode = FlowNode,
  TBranch extends ConditionBranch = ConditionBranch,
  TGroup extends ConditionGroupNode = ConditionGroupNode
> {
  container: FlowNode[];
  node: TNode;
  index: number;
  parentBranch: TBranch | null;
  parentGroup: TGroup | null;
}

export interface BranchLocation<
  TBranch extends ConditionBranch = ConditionBranch,
  TGroup extends ConditionGroupNode = ConditionGroupNode
> {
  group: TGroup;
  branch: TBranch;
  branchIndex: number;
}

const findNodeLocationInBranch = (
  group: ConditionGroupNode,
  branch: ConditionBranch,
  nodeId: string
): NodeLocation | null => {
  let result: NodeLocation | null = null;

  branch.children.some((node, index) => {
    if (node.id === nodeId) {
      result = {
        container: branch.children,
        node,
        index,
        parentBranch: branch,
        parentGroup: group,
      };
      return true;
    }

    if (node.type === 'condition-group') {
      const nestedResult = node.branches.reduce<NodeLocation | null>((acc, nestedBranch) => {
        if (acc) {
          return acc;
        }

        return findNodeLocationInBranch(node, nestedBranch, nodeId);
      }, null);

      if (nestedResult) {
        result = nestedResult;
        return true;
      }
    }

    return false;
  });

  return result;
};

const findNodeLocationInGroup = (group: ConditionGroupNode, nodeId: string): NodeLocation | null => {
  let result: NodeLocation | null = null;

  group.branches.some((branch) => {
    const branchResult = findNodeLocationInBranch(group, branch, nodeId);
    if (branchResult) {
      result = branchResult;
      return true;
    }

    return false;
  });

  return result;
};

export function findNodeById<TNode extends FlowNode = FlowNode>(nodes: FlowNode[], nodeId: string): TNode | null {
  let result: FlowNode | null = null;

  nodes.some((node) => {
    if (node.id === nodeId) {
      result = node;
      return true;
    }

    if (node.type === 'condition-group') {
      const branchResult = node.branches.reduce<FlowNode | null>((acc, branch) => {
        if (acc) {
          return acc;
        }

        return findNodeById(branch.children, nodeId);
      }, null);

      if (branchResult) {
        result = branchResult;
        return true;
      }
    }

    return false;
  });

  return result as TNode | null;
}

export function findNodeLocation<TNode extends FlowNode = FlowNode>(
  nodes: FlowNode[],
  nodeId: string
): NodeLocation<TNode> | null {
  let result: NodeLocation | null = null;

  nodes.some((node, index) => {
    if (node.id === nodeId) {
      result = {
        container: nodes,
        node,
        index,
        parentBranch: null,
        parentGroup: null,
      };
      return true;
    }

    if (node.type === 'condition-group') {
      const branchResult = findNodeLocationInGroup(node, nodeId);
      if (branchResult) {
        result = branchResult;
        return true;
      }
    }

    return false;
  });

  return result as NodeLocation<TNode> | null;
}

export function findConditionGroupById(nodes: FlowNode[], groupId: string): ConditionGroupNode | null {
  const found = findNodeById(nodes, groupId);
  return found?.type === 'condition-group' ? found : null;
}

export function findBranchLocation<TBranch extends ConditionBranch = ConditionBranch>(
  nodes: FlowNode[],
  branchId: string
): BranchLocation<TBranch> | null {
  let result: BranchLocation | null = null;

  nodes.some((node) => {
    if (node.type !== 'condition-group') {
      return false;
    }

    const branchIndex = node.branches.findIndex((branch) => branch.id === branchId);
    if (branchIndex >= 0) {
      result = {
        group: node,
        branch: node.branches[branchIndex],
        branchIndex,
      };
      return true;
    }

    const nestedResult = node.branches.reduce<BranchLocation | null>((acc, branch) => {
      if (acc) {
        return acc;
      }

      return findBranchLocation(branch.children, branchId);
    }, null);

    if (nestedResult) {
      result = nestedResult;
      return true;
    }

    return false;
  });

  return result as BranchLocation<TBranch> | null;
}

export function findBranchById<TBranch extends ConditionBranch = ConditionBranch>(
  nodes: FlowNode[],
  branchId: string
): TBranch | null {
  return findBranchLocation<TBranch>(nodes, branchId)?.branch ?? null;
}
