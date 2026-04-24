/** 图布局：负责所有图形元素的位置和连线关系 */
import type { FlowNode, FlowSchema } from '../types';
import { clearRenameHandlers, registerBranchRenameHandler, registerMainNodeRenameHandler } from './renameRegistry';
import type { FlowGraphLayoutEdge, FlowGraphLayoutNode, FlowGraphLayoutResult } from './types';

export interface FlowLayoutOptions {
  cardWidth: number;
  cardHeight: number;
  showNodeDescription: boolean;
  nodeGapY: number;
  branchGapY: number; // 条件组内部垂直间距
  rootX: number; // 根节点链的起始 X 坐标
  rootStartY: number; // 根节点链的起始 Y 坐标
  branchColumnGap: number; // 同一条件组内，不同分支列之间的水平间距
  branchBusGapY: number; // 分支总线（水平连线）到分支节点或输出点的垂直偏移量
  addConditionWidth: number;
  addConditionHeight: number;
  addNodeSize: number;
  addNodeOffsetY: number; // 主节点底部到其下方“添加节点”按钮之间的垂直偏移量
}

export const DEFAULT_FLOW_LAYOUT_OPTIONS: FlowLayoutOptions = {
  cardWidth: 320,
  cardHeight: 104,
  showNodeDescription: true,
  nodeGapY: 48,
  branchGapY: 56,
  rootX: 80,
  rootStartY: 48,
  branchColumnGap: 96,
  branchBusGapY: 24,
  addConditionWidth: 104,
  addConditionHeight: 38,
  addNodeSize: 24,
  addNodeOffsetY: 10,
};

type FlowConnector = string | { x: number; y: number };
type MainChainNode = Exclude<FlowNode, { type: 'condition-group' }>; // 主链节点类型：排除掉 condition-group 类型，即普通节点（start/action/end）

// 决定用哪个组件
function shapeByNodeType(type: MainChainNode['type']): string {
  switch (type) {
    case 'start':
      return 'flow-start-node';
    case 'action':
      return 'flow-action-node';
    case 'end':
      return 'flow-end-node';
    default:
      return 'flow-action-node';
  }
}

function createMainNodeLayout(
  node: MainChainNode,
  x: number,
  y: number,
  options: FlowLayoutOptions
): FlowGraphLayoutNode {
  registerMainNodeRenameHandler(node.id, (value: string) => {
    node.name = value;
  });
  return {
    id: node.id,
    shape: shapeByNodeType(node.type),
    x,
    y,
    width: options.cardWidth,
    height: options.cardHeight,
    data: {
      kind: node.type,
      nodeId: node.id,
      nodeType: node.type,
      name: node.name,
      description: node.type !== 'end' ? node.description : undefined,
      actionType: node.type === 'action' ? node.actionType : undefined,
      showContent: options.showNodeDescription,
    },
  };
}

function createBranchNodeId(groupId: string, branchId: string): string {
  return `branch_${groupId}_${branchId}`;
}

function createAddConditionId(groupId: string): string {
  return `add_condition_${groupId}`;
}

function createAddNodeId(anchorId: string): string {
  return `add_node_${anchorId}`;
}

// 创建“添加节点”的按钮布局
function createNodeAddLayout(
  node: MainChainNode,
  x: number,
  y: number,
  options: FlowLayoutOptions
): FlowGraphLayoutNode {
  return {
    id: createAddNodeId(node.id),
    shape: 'flow-add-node',
    x: x + options.cardWidth / 2 - options.addNodeSize / 2,
    y: y + options.cardHeight + options.addNodeOffsetY,
    width: options.addNodeSize,
    height: options.addNodeSize,
    data: {
      kind: 'add-node',
      nodeId: node.id,
      nodeType: node.type,
    },
  };
}

// 创建分支卡片下方“添加节点”的按钮布局
function createConditionBranchAddNodeLayout(
  groupId: string,
  branchId: string,
  branchNodeId: string,
  slotCenterX: number,
  branchStartY: number,
  options: FlowLayoutOptions
): FlowGraphLayoutNode {
  return {
    id: createAddNodeId(branchNodeId),
    shape: 'flow-add-node',
    x: slotCenterX - options.addNodeSize / 2,
    y: branchStartY + options.cardHeight + options.addNodeOffsetY,
    width: options.addNodeSize,
    height: options.addNodeSize,
    data: {
      kind: 'add-node',
      groupId,
      branchId,
    },
  };
}

// 创建“条件组汇总出口”下方 add-node（用于在整个条件组后继续插入节点）
function createConditionGroupOutputAddNodeLayout(
  groupId: string,
  centerX: number,
  outputPointY: number,
  options: FlowLayoutOptions
): FlowGraphLayoutNode {
  return {
    id: createAddNodeId(`group_output_${groupId}`),
    shape: 'flow-add-node',
    x: centerX - options.addNodeSize / 2,
    y: outputPointY - options.addNodeSize / 2,
    width: options.addNodeSize,
    height: options.addNodeSize,
    data: {
      kind: 'add-node',
      nodeId: groupId,
      nodeType: 'condition-group',
    },
  };
}

// 创建“添加条件”按钮的节点布局
function createAddConditionNodeLayout(
  groupId: string,
  centerX: number,
  addConditionY: number,
  options: FlowLayoutOptions
): FlowGraphLayoutNode {
  return {
    id: createAddConditionId(groupId),
    shape: 'flow-add-condition-node',
    x: centerX - options.addConditionWidth / 2,
    y: addConditionY,
    width: options.addConditionWidth,
    height: options.addConditionHeight,
    data: {
      kind: 'add-condition',
      groupId,
    },
  };
}

// 创建“条件分支卡片”节点布局
function createConditionBranchNodeLayout(
  groupId: string,
  branch: Extract<FlowNode, { type: 'condition-group' }>['branches'][number],
  branchNodeId: string,
  branchCardX: number,
  branchStartY: number,
  options: FlowLayoutOptions
): FlowGraphLayoutNode {
  registerBranchRenameHandler(groupId, branch.id, (value: string) => {
    branch.name = value;
  });
  return {
    id: branchNodeId,
    shape: 'flow-condition-branch-node',
    x: branchCardX,
    y: branchStartY,
    width: options.cardWidth,
    height: options.cardHeight,
    data: {
      kind: 'condition-branch',
      groupId,
      branchId: branch.id,
      name: branch.name,
      description: branch.description,
      showContent: options.showNodeDescription,
      isElse: branch.isElse,
    },
  };
}

// 计算一个节点（可能是普通节点或条件组）在布局时所占的宽度
function measureNodeWidth(node: FlowNode, options: FlowLayoutOptions): number {
  if (node.type !== 'condition-group') {
    return options.cardWidth;
  }

  // 条件组需要计算所有分支的最大宽度之和，然后取与 cardWidth 的较大值
  const branchWidths = node.branches.map(
    // eslint-disable-next-line no-use-before-define
    (branch) => Math.max(options.cardWidth, measureNodeChainWidth(branch.children, options))
  );
  const totalBranchesWidth =
    branchWidths.reduce((sum, width) => sum + width, 0) +
    Math.max(0, branchWidths.length - 1) * options.branchColumnGap;

  return Math.max(options.cardWidth, totalBranchesWidth);
}

function measureNodeChainWidth(nodes: FlowNode[], options: FlowLayoutOptions): number {
  return nodes.reduce((maxWidth, node) => Math.max(maxWidth, measureNodeWidth(node, options)), options.cardWidth);
}

// 主链布局
function layoutNodeChain(
  chainNodes: FlowNode[],
  x: number,
  startY: number,
  initialConnector: FlowConnector | null, // 上一个连接点
  options: FlowLayoutOptions
): {
  nodes: FlowGraphLayoutNode[];
  edges: FlowGraphLayoutEdge[];
  nextY: number;
  outputConnector: FlowConnector | null;
} {
  const nodes: FlowGraphLayoutNode[] = []; // 要渲染的节点
  const edges: FlowGraphLayoutEdge[] = []; // 要渲染的边

  let cursorY = startY; // 当前链路纵向游标，表示下一个节点的 y 起点
  let previousConnector = initialConnector; // 前驱连接点

  chainNodes.forEach((node) => {
    // 条件组走单独的布局分支
    if (node.type === 'condition-group') {
      // eslint-disable-next-line no-use-before-define
      const nestedLayout = layoutConditionGroup(node, x, cursorY, options);
      nodes.push(...nestedLayout.nodes); // 把条件组产生的节点并入当前结果
      edges.push(...nestedLayout.edges); // 把条件组产生的边并入当前结果

      // 如果有前驱连接点，则连到条件组入口
      if (previousConnector) {
        edges.push({
          source: previousConnector,
          target: nestedLayout.entryPoint,
        });
      }

      previousConnector = nestedLayout.outputPoint; // 条件组输出点作为后续节点的前驱连接点
      cursorY = nestedLayout.nextY; // 条件组占位后的下一行 y
      return; // 处理下一个主链节点
    }

    // 普通节点（start/action/end）布局
    const mainNode = createMainNodeLayout(node, x, cursorY, options);
    nodes.push(mainNode); // 放入主节点

    // 除 end 外，节点下方需要放 add-node
    if (node.type !== 'end') {
      nodes.push(createNodeAddLayout(node, x, cursorY, options));
    }

    // 如果有前驱连接点，则连接到当前主节点
    if (previousConnector) {
      edges.push({
        source: previousConnector,
        target: mainNode.id,
      });
    }

    // 当前主节点成为下一个节点的前驱连接点
    previousConnector = mainNode.id;
    // 纵向游标下移到下一张卡片起点
    cursorY = cursorY + options.cardHeight + options.nodeGapY;
  });

  return {
    nodes,
    edges,
    nextY: cursorY,
    outputConnector: previousConnector,
  };
}

// 根据 connector（可能是节点 ID 或坐标点）获取其 X 坐标
function getConnectorX(connector: FlowConnector, nodeCenterXMap: Map<string, number>, fallbackX: number): number {
  if (typeof connector !== 'string') {
    return connector.x;
  }

  return nodeCenterXMap.get(connector) ?? fallbackX;
}

// 条件组布局
function layoutConditionGroup(
  node: Extract<FlowNode, { type: 'condition-group' }>,
  x: number,
  y: number,
  options: FlowLayoutOptions
): {
  nodes: FlowGraphLayoutNode[];
  edges: FlowGraphLayoutEdge[];
  entryPoint: { x: number; y: number };
  outputPoint: { x: number; y: number };
  nextY: number;
} {
  const nodes: FlowGraphLayoutNode[] = []; // 收集条件组内部节点
  const edges: FlowGraphLayoutEdge[] = []; // 收集条件组内部边

  const centerX = x + options.cardWidth / 2; // 条件组中心 x（用于总线和汇合线）

  // “添加条件”节点 y
  const addConditionY = y + options.branchBusGapY;
  // 顶部总线 y
  const topBusY = addConditionY + options.addConditionHeight / 2;
  // 条件组入口点（主链连接到条件组时使用）
  const entryPoint = { x: centerX, y: topBusY };

  // 渲染顶部“添加条件”节点
  nodes.push(createAddConditionNodeLayout(node.id, centerX, addConditionY, options));

  // 分支卡片起始 y（位于 add-condition 下方）
  const branchStartY = addConditionY + options.addConditionHeight + options.branchGapY;
  // 分支 children 链路起始 y（位于分支卡片下方）
  const branchContentStartY = branchStartY + options.cardHeight + options.nodeGapY;
  // 每个分支槽位宽：至少一张卡片宽，若 children 更宽则取更宽
  const branchWidths = node.branches.map((branch) =>
    Math.max(options.cardWidth, measureNodeChainWidth(branch.children, options))
  );
  // 所有分支总宽（包含列间距）
  const totalBranchesWidth =
    branchWidths.reduce((sum, width) => sum + width, 0) +
    Math.max(0, branchWidths.length - 1) * options.branchColumnGap;
  // 当前分支槽位左边界，从整体居中位置开始
  let branchSlotLeft = centerX - totalBranchesWidth / 2;

  // 记录每个分支中心 x，用于绘制总线
  const branchCenters: number[] = [];
  // 记录每个分支尾部连接点，用于底部汇合
  const branchTailConnectors: FlowConnector[] = [];
  // 条件组底部最深 y（随分支内容增长）
  let maxBottomY = branchContentStartY;

  // 横向遍历所有分支
  node.branches.forEach((branch, index) => {
    // 当前分支槽位宽
    const slotWidth = branchWidths[index] ?? options.cardWidth;
    // 当前分支中心 x
    const slotCenterX = branchSlotLeft + slotWidth / 2;
    // 分支卡片左上角 x
    const branchCardX = slotCenterX - options.cardWidth / 2;
    // 分支可视节点 id
    const branchNodeId = createBranchNodeId(node.id, branch.id);

    // 记录分支中心供后续总线使用
    branchCenters.push(slotCenterX);

    // 渲染分支卡片节点（if/else）
    nodes.push(createConditionBranchNodeLayout(node.id, branch, branchNodeId, branchCardX, branchStartY, options));

    // 渲染分支卡片下方 add-node
    nodes.push(
      createConditionBranchAddNodeLayout(node.id, branch.id, branchNodeId, slotCenterX, branchStartY, options)
    );

    // 顶部总线连接到当前分支卡片
    edges.push({
      source: { x: slotCenterX, y: topBusY },
      target: branchNodeId,
    });

    // 递归布局当前分支 children
    const branchLayout = layoutNodeChain(branch.children, branchCardX, branchContentStartY, branchNodeId, options);
    // 合并分支 children 产生的节点
    nodes.push(...branchLayout.nodes);
    // 合并分支 children 产生的边
    edges.push(...branchLayout.edges);

    // 更新条件组底部最深 y
    maxBottomY = Math.max(maxBottomY, branchLayout.nextY);
    // 记录分支末端连接点（若无 children，则回退到分支卡片）
    branchTailConnectors.push(branchLayout.outputConnector ?? branchNodeId);

    // 移动到下一列分支槽位左边界
    branchSlotLeft = branchSlotLeft + slotWidth + options.branchColumnGap;
  });

  // 条件组输出点（主链从这里继续向下）
  const outputPoint = { x: centerX, y: maxBottomY + options.branchBusGapY };
  // 底部总线 y
  const bottomBusY = outputPoint.y - options.branchBusGapY / 2;
  // 顶/底总线左端 x
  const leftBusX = Math.min(...branchCenters);
  // 顶/底总线右端 x
  const rightBusX = Math.max(...branchCenters);

  // 多分支时绘制顶部横向总线
  if (branchCenters.length > 1) {
    edges.push({
      source: { x: leftBusX, y: topBusY },
      target: { x: rightBusX, y: topBusY },
    });
  }

  // 建立节点中心 x 索引，便于 connector->x 计算
  const nodeCenterXMap = new Map(nodes.map((item) => [item.id, item.x + item.width / 2]));
  // 每个分支尾部先连到“底部总线同 x 的落点”
  branchTailConnectors.forEach((connector) => {
    const tailCenterX = getConnectorX(connector, nodeCenterXMap, centerX);
    edges.push({
      source: connector,
      target: { x: tailCenterX, y: bottomBusY },
    });
  });

  // 多分支时绘制底部横向汇合总线
  if (branchCenters.length > 1) {
    edges.push({
      source: { x: leftBusX, y: bottomBusY },
      target: { x: rightBusX, y: bottomBusY },
    });
  }

  // 从底部总线中心连到条件组输出点
  edges.push({
    source: { x: centerX, y: bottomBusY },
    target: outputPoint,
  });

  // 在条件组汇总出口处放一个 add-node，用于“分支汇总后继续插入节点”
  nodes.push(createConditionGroupOutputAddNodeLayout(node.id, centerX, outputPoint.y, options));

  return {
    nodes,
    edges,
    entryPoint,
    outputPoint,
    nextY: outputPoint.y + options.nodeGapY,
  };
}

export default function buildFlowLayout(
  flow: FlowSchema,
  options: Partial<FlowLayoutOptions> = {}
): FlowGraphLayoutResult {
  clearRenameHandlers(); // 在每次重新布局前调用，避免旧 key 泄漏到新图数据中

  // 允许业务场景覆写默认布局参数
  const mergedOptions: FlowLayoutOptions = {
    ...DEFAULT_FLOW_LAYOUT_OPTIONS,
    ...options,
  };

  // 从主链根节点起进行递归布局，内部会展开条件组分支结构
  const layout = layoutNodeChain(flow.nodes, mergedOptions.rootX, mergedOptions.rootStartY, null, mergedOptions);

  return {
    nodes: layout.nodes,
    edges: layout.edges,
  };
}
