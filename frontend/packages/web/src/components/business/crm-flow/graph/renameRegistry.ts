import type { FlowGraphNodeData } from './types';

// 重命名处理函数：接收“新名称”，不返回结果
type RenameHandler = (value: string) => void;

// 运行期注册表：key -> 重命名处理函数
// key 示例：
// - 主链节点：node:<nodeId>
// - 条件分支：branch:<groupId>:<branchId>
const renameHandlerMap = new Map<string, RenameHandler>();

// 生成主链节点（start/action/end）的注册 key
function toMainNodeRenameKey(nodeId: string): string {
  return `node:${nodeId}`;
}

// 生成条件分支节点（condition-branch）的注册 key
function toBranchRenameKey(groupId: string, branchId: string): string {
  return `branch:${groupId}:${branchId}`;
}

// 清空本轮渲染的所有重命名处理器
export function clearRenameHandlers() {
  renameHandlerMap.clear();
}

// 注册主链节点的重命名处理器
export function registerMainNodeRenameHandler(nodeId: string, handler: RenameHandler) {
  renameHandlerMap.set(toMainNodeRenameKey(nodeId), handler);
}

// 注册条件分支节点的重命名处理器
export function registerBranchRenameHandler(groupId: string, branchId: string, handler: RenameHandler) {
  renameHandlerMap.set(toBranchRenameKey(groupId, branchId), handler);
}

// 根据图层节点 data 自动路由到主链节点或分支节点的处理器
export function renameFlowByGraphData(data: FlowGraphNodeData, value: string): boolean {
  // 主链节点（start/action/end）改名逻辑
  if (data.kind === 'action' || data.kind === 'start' || data.kind === 'end') {
    if (!data.nodeId) {
      return false;
    }
    // 按主链 key 读取对应处理器
    const handler = renameHandlerMap.get(toMainNodeRenameKey(data.nodeId));
    // 未注册处理器时返回失败
    if (!handler) {
      return false;
    }
    // 执行重命名（实际回写到 DSL）
    handler(value);
    return true;
  }

  // 条件分支节点改名逻辑
  if (data.kind === 'condition-branch') {
    if (!data.groupId || !data.branchId) {
      return false;
    }
    // 按分支 key 读取对应处理器
    const handler = renameHandlerMap.get(toBranchRenameKey(data.groupId, data.branchId));
    // 未注册处理器时返回失败
    if (!handler) {
      return false;
    }
    // 执行重命名（实际回写到 DSL）
    handler(value);
    return true;
  }

  return false;
}
