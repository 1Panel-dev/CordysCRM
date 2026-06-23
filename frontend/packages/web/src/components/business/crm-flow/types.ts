export type FlowNodeType = 'start' | 'action' | 'condition-group' | 'end';

export type FlowActionType = 'approval';

export interface FlowNodeDescriptionItem {
  id: string;
  name: string;
}

export interface FlowSchema {
  nodes: FlowNode[];
}

export interface BaseFlowNode {
  id: string;
  type: FlowNodeType;
  name: string;
  invalid?: boolean;
  number?: string;
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
  descriptionItems?: FlowNodeDescriptionItem[];
}

export interface ConditionBranch {
  id: string;
  name: string;
  number?: string;
  isElse: boolean;
  description?: string;
  invalid?: boolean;
  children: FlowNode[];
}

export interface ConditionGroupNode extends BaseFlowNode {
  type: 'condition-group';
  branches: ConditionBranch[];
}

export type FlowNode = StartNode | ActionNode | ConditionGroupNode | EndNode;

export type NodeSelectionState<TNode extends FlowNode = FlowNode, TBranch extends ConditionBranch = ConditionBranch> =
  | { type: 'none' }
  | { type: 'node'; id: string; node: TNode }
  | { type: 'branch'; id: string; branch: TBranch };
