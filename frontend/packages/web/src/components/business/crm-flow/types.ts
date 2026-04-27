export type FlowNodeType = 'start' | 'action' | 'condition-group' | 'end';

export type FlowActionType = 'approval';

export interface FlowSchema {
  nodes: FlowNode[];
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
  config?: Record<string, any>;
}

export interface ConditionBranch {
  id: string;
  name: string;
  isElse: boolean;
  description?: string;
  config?: Record<string, any>;
  children: FlowNode[];
}

export interface ConditionGroupNode extends BaseFlowNode {
  type: 'condition-group';
  branches: ConditionBranch[];
}

export type FlowNode = StartNode | ActionNode | ConditionGroupNode | EndNode;

export type NodeSelectionState =
  | { type: 'none' }
  | { type: 'node'; id: string; node: FlowNode }
  | { type: 'branch'; id: string; branch: ConditionBranch };
