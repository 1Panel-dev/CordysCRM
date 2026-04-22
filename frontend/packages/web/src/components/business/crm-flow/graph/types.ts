import type { FlowNodeType } from '../types';
import type { Graph } from '@antv/x6';

export type FlowGraphDataKind = 'start' | 'action' | 'end' | 'condition-branch' | 'add-condition' | 'add-node';

export interface FlowGraphNodeData {
  kind: FlowGraphDataKind; // 图层节点种类：既包含 DSL 节点，也包含 add-node 等图层辅助节点
  nodeId?: string; // 对应 DSL 节点 id（主链节点）
  nodeType?: FlowNodeType; // 锚点业务节点类型（常用于 add-node 场景下识别来源节点类型）
  groupId?: string; // 条件组 id（condition-group）
  branchId?: string; // 条件分支 id（if / else 分支）
  name?: string;
  description?: string;
  actionType?: string; // 动作节点的扩展类型（如 approval）。
  showContent?: boolean; // 当前视图模式下是否显示节点第二行内容（description）
  isElse?: boolean;
  selected?: boolean; // 选中
}

export interface FlowGraphLayoutNode {
  id: string;
  shape: string;
  x: number;
  y: number;
  width: number;
  height: number;
  data: FlowGraphNodeData;
}

export interface FlowGraphLayoutEdge {
  source: string | { x: number; y: number };
  target: string | { x: number; y: number };
  id?: string;
  vertices?: Array<{ x: number; y: number }>;
}

export interface FlowGraphLayoutResult {
  nodes: FlowGraphLayoutNode[];
  edges: FlowGraphLayoutEdge[];
}

export interface FlowGraphClickPosition {
  x: number;
  y: number;
}

export interface NodeClickPayload {
  nodeId: string;
  nodeType: FlowNodeType;
}

export interface BranchClickPayload {
  groupId: string;
  branchId: string;
}

export interface AddNodeClickPayload {
  position: FlowGraphClickPosition;
  nodeId?: string;
  nodeType?: FlowNodeType;
  groupId?: string;
  branchId?: string;
}

export interface FlowGraphNodeClickPayload {
  data: FlowGraphNodeData;
  position: FlowGraphClickPosition | null;
}

export interface FlowGraphEventHandlers {
  onNodeClick?: (payload: FlowGraphNodeClickPayload) => void;
  onNodeDelete?: (payload: { data: FlowGraphNodeData }) => void;
  onBlankClick?: () => void;
}

export interface FlowGraphController {
  getGraph: () => Graph | null;
  getZoom: () => number;
  setZoom: (value: number) => void;
  render: (cells: unknown[]) => void;
  zoomIn: () => void;
  zoomOut: () => void;
  fitToContent: () => void;
  centerContent: () => void;
  setPanMode: (enabled: boolean) => void;
  isPanMode: () => boolean;
  dispose: () => void;
}
