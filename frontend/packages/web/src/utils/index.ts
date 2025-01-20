import { cloneDeep } from 'lodash-es';

export interface TreeNode<T> {
  children?: TreeNode<T>[];
  [key: string]: any;
}

/**
 * 递归遍历树形数组或树
 * @param tree 树形数组或树
 * @param customNodeFn 自定义节点函数
 * @param customChildrenKey 自定义子节点的key
 * @param continueCondition 继续递归的条件，某些情况下需要无需递归某些节点的子孙节点，可传入该条件
 */
export function traverseTree<T>(
  tree: TreeNode<T> | TreeNode<T>[] | T | T[],
  customNodeFn: (node: TreeNode<T>) => void,
  continueCondition?: (node: TreeNode<T>) => boolean,
  customChildrenKey = 'children'
) {
  if (!Array.isArray(tree)) {
    tree = [tree];
  }
  for (let i = 0; i < tree.length; i++) {
    const node = (tree as TreeNode<T>[])[i];
    if (typeof customNodeFn === 'function') {
      customNodeFn(node);
    }
    if (node[customChildrenKey] && Array.isArray(node[customChildrenKey]) && node[customChildrenKey].length > 0) {
      if (typeof continueCondition === 'function' && !continueCondition(node)) {
        // 如果有继续递归的条件，则判断是否继续递归
        break;
      }
      traverseTree(node[customChildrenKey], customNodeFn, continueCondition, customChildrenKey);
    }
  }
}

/**
 * 生成 id 序列号
 * @returns
 */
let lastTimestamp = 0;
let sequence = 0;
export const getGenerateId = () => {
  let timestamp = new Date().getTime();
  if (timestamp === lastTimestamp) {
    sequence++;
    if (sequence >= 100000) {
      // 如果超过999，则重置为0，等待下一秒
      sequence = 0;
      while (timestamp <= lastTimestamp) {
        timestamp = new Date().getTime();
      }
    }
  } else {
    sequence = 0;
  }

  lastTimestamp = timestamp;

  return timestamp.toString() + sequence.toString().padStart(5, '0');
};

/**
 * 删除树形数组中的某个节点
 * @param treeArr 目标树
 * @param targetKey 目标节点唯一值
 */
export function deleteNode<T>(treeArr: TreeNode<T>[], targetKey: string | number, customKey = 'key'): void {
  function deleteNodeInTree(tree: TreeNode<T>[]): void {
    for (let i = 0; i < tree.length; i++) {
      const node = tree[i];
      if (node[customKey] === targetKey) {
        tree.splice(i, 1); // 直接删除当前节点
        // 重新调整剩余子节点的 sort 序号
        for (let j = i; j < tree.length; j++) {
          tree[j].sort = j + 1;
        }
        return;
      }
      if (Array.isArray(node.children)) {
        deleteNodeInTree(node.children); // 递归删除子节点
      }
    }
  }

  deleteNodeInTree(treeArr);
}

/**
 * 递归遍历树形数组或树，返回新的树
 * @param tree 树形数组或树
 * @param customNodeFn 自定义节点函数
 * @param customChildrenKey 自定义子节点的key
 * @param parent 父节点
 * @param parentPath 父节点路径
 * @param level 节点层级
 * @returns 遍历后的树形数组
 */
export function mapTree<T>(
  tree: TreeNode<T> | TreeNode<T>[] | T | T[],
  customNodeFn: (node: TreeNode<T>, path: string, _level: number) => TreeNode<T> | null = (node) => node,
  customChildrenKey = 'children',
  parentPath = '',
  level = 0,
  parent: TreeNode<T> | null = null
): T[] {
  let cloneTree = cloneDeep(tree);
  if (!Array.isArray(cloneTree)) {
    cloneTree = [cloneTree];
  }

  function mapFunc(
    _tree: TreeNode<T> | TreeNode<T>[] | T | T[],
    _parentPath = '',
    _level = 0,
    _parent: TreeNode<T> | null = null
  ): T[] {
    if (!Array.isArray(_tree)) {
      _tree = [_tree];
    }
    return _tree
      .map((node: TreeNode<T>, i: number) => {
        const fullPath = node.path ? `${_parentPath}/${node.path}`.replace(/\/+/g, '/') : '';
        node.sort = i + 1; // sort 从 1 开始
        node.parent = _parent || undefined; // 没有父节点说明是树的第一层
        const newNode = typeof customNodeFn === 'function' ? customNodeFn(node, fullPath, _level) : node;
        if (newNode) {
          newNode.level = _level;
          if (newNode[customChildrenKey] && newNode[customChildrenKey].length > 0) {
            newNode[customChildrenKey] = mapFunc(newNode[customChildrenKey], fullPath, _level + 1, newNode);
          }
        }
        return newNode;
      })
      .filter((node: TreeNode<T> | null) => node !== null);
  }
  return mapFunc(cloneTree, parentPath, level, parent);
}

/**
 * 获取树形数据所有有子节点的父节点
 * @param treeData 树形数组
 * @param childrenKey 自定义子节点的key
 * @returns 遍历后父节点数组
 */
export function getAllParentNodeIds<T>(
  treeData: TreeNode<T>[],
  childrenKey = 'children',
  customKey = 'id'
): Array<string | number> {
  const parentIds: Array<string | number> = [];
  const traverse = (nodes: TreeNode<T>) => {
    for (let i = 0; i < nodes.length; i++) {
      const node = nodes[i];
      if (node[childrenKey] && node[childrenKey].length > 0) {
        parentIds.push(node[customKey]); // 记录当前节点的 ID
        traverse(node[childrenKey]); // 递归遍历子节点
      }
    }
  };

  traverse(treeData); // 开始递归
  return parentIds;
}

/**
 *
 * 返回文件的大小
 * @param fileSize file文件的大小size
 * @returns
 */
export function formatFileSize(fileSize: number): string {
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  let size = fileSize;
  let unitIndex = 0;

  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024;
    unitIndex++;
  }
  const unit = units[unitIndex];
  if (size) {
    const formattedSize = size.toFixed(2);
    return `${formattedSize} ${unit}`;
  }
  const formattedSize = 0;
  return `${formattedSize} ${unit}`;
}
