export type FlowNodeType = 'start' | 'action' | 'condition-group' | 'end';

export type FlowActionType = 'approval'; // TODO lmy

export interface FlowSchema {
  id: string;
  name: string;
  nodes: FlowNode[];
  // TODO lmy
}

export interface BaseFlowNode {
  id: string;
  type: FlowNodeType;
  name: string;
}

export interface StartNode extends BaseFlowNode {
  type: 'start';
  description?: string;
}

export interface EndNode extends BaseFlowNode {
  type: 'end';
}

export interface ActionNode extends BaseFlowNode {
  type: 'action';
  actionType: FlowActionType;
  description?: string;
  config: Record<string, unknown>;
}

export interface ConditionBranch {
  id: string;
  name: string;
  isElse: boolean;
  description?: string;
  children: FlowNode[];
}

export interface ConditionGroupNode extends BaseFlowNode {
  type: 'condition-group';
  branches: ConditionBranch[];
}

export type FlowNode = StartNode | ActionNode | ConditionGroupNode | EndNode;

// TODO lmy
export type SelectionType = 'none' | 'node' | 'branch';

export interface NodeSelectionState {
  selectionType: SelectionType;
  selectedNode: FlowNode | null;
  selectedBranch: ConditionBranch | null;
}
