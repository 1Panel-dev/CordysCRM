import { cloneDeep } from 'lodash-es';
import dayjs from 'dayjs';
import JSEncrypt from 'jsencrypt';

import { isObject } from './is';
import { CHINA_PCD, COUNTRIES_TREE } from '@cordys/web/src/components/business/crm-city-select/config';
import type {
  FormCreateField,
  FormCreateFieldDateType,
} from '@cordys/web/src/components/business/crm-form-create/types';
import { getLocalStorage } from '@lib/shared/method/local-storage';

/**
 * 递归深度合并
 * @param src 源对象
 * @param target 待合并的目标对象
 * @returns 合并后的对象
 */
export const deepMerge = <T = any>(src: any = {}, target: any = {}): T => {
  Object.keys(target).forEach((key) => {
    src[key] = isObject(src[key]) ? deepMerge(src[key], target[key]) : (src[key] = target[key]);
  });
  return src;
};

/**
 * 遍历对象属性并一一添加到 url 地址参数上
 * @param baseUrl 需要添加参数的 url
 * @param obj 参数对象
 * @returns 拼接后的 url
 */
export function setObjToUrlParams(baseUrl: string, obj: any): string {
  let parameters = '';
  Object.keys(obj).forEach((key) => {
    parameters += `${key}=${encodeURIComponent(obj[key])}&`;
  });
  parameters = parameters.replace(/&$/, '');
  return /\?$/.test(baseUrl) ? baseUrl + parameters : baseUrl.replace(/\/?$/, '?') + parameters;
}

/**
 * 加密
 * @param input 输入的字符串
 * @param publicKey 公钥
 * @returns
 */
export function encrypted(input: string) {
  const publicKey = getLocalStorage('publicKey') || '';
  const encrypt = new JSEncrypt({ default_key_size: '1024' });
  encrypt.setPublicKey(publicKey);

  return encrypt.encrypt(input);
}

/**
 * 休眠
 * @param ms 睡眠时长，单位毫秒
 * @returns
 */
export function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => {
    setTimeout(() => resolve(), ms);
  });
}

export function getQueryVariable(variable: string) {
  const urlString = window.location.href;
  const queryIndex = urlString.indexOf('?');
  if (queryIndex !== -1) {
    // 先获取?到#之间的内容，如果没有#则获取到结尾
    const hashIndex = urlString.indexOf('#');
    const queryEnd = hashIndex !== -1 ? hashIndex : urlString.length;
    const query = urlString.substring(queryIndex + 1, queryEnd);

    // 分割查询参数
    const params = query.split('&');
    // 遍历参数，找到 _token 参数的值
    let variableValue;
    params.forEach((param) => {
      const equalIndex = param.indexOf('=');
      const variableName = param.substring(0, equalIndex);
      if (variableName === variable) {
        variableValue = param.substring(equalIndex + 1);
      }
    });
    return variableValue;
  }
}

export function getUrlParameterWidthRegExp(name: string) {
  const url = window.location.href;
  name = name.replace(/[[\]]/g, '\\$&');
  const regex = new RegExp(`[?&]${name}(=([^&#]*)|&|#|$)`);
  const results = regex.exec(url);
  if (!results) return null;
  if (!results[2]) return '';
  return decodeURIComponent(results[2].replace(/\+/g, ' '));
}

/**
 * 建立 SSE 连接
 * @param url 连接地址
 * @param host 连接主机
 * @returns EventSource 实例
 */
export const apiSSE = (url: string, host?: string): EventSource => {
  let protocol = 'http://';

  // 判断是否使用 HTTPS
  if (!host?.startsWith('http') && (window.location.protocol === 'https:' || host?.startsWith('https'))) {
    protocol = 'https://';
  }

  // 解析 URL，自动适配 host
  const uri = protocol + (host?.split('://')[1] || window.location.host) + url;

  return new EventSource(uri, {
    withCredentials: true,
  });
};

/**
 * 获取 SSE 连接
 * @param sseUrl，自定义 SSE 地址
 * @param host 自定义主机
 * @returns EventSource 实例
 */
export function getSSE(sseUrl: string, params: Record<string, string>, host?: string): EventSource {
  const queryString = new URLSearchParams(params).toString();
  return apiSSE(`${sseUrl}?${queryString}`, host);
}

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
 * 过滤树形数组或树
 * @param tree 树形数组或树
 * @param customNodeFn 自定义节点函数
 * @param customChildrenKey 自定义子节点的key
 * @returns 遍历后的树形数组
 */
export function filterTree<T>(
  tree: TreeNode<T> | TreeNode<T>[] | T | T[],
  filterFn: (node: TreeNode<T>, nodeIndex: number, parent?: TreeNode<T> | null) => boolean,
  customChildrenKey = 'children',
  parentNode: TreeNode<T> | null = null
): TreeNode<T>[] {
  if (!Array.isArray(tree)) {
    tree = [tree];
  }
  const filteredTree: TreeNode<T>[] = [];
  for (let i = 0; i < tree.length; i++) {
    const node = (tree as TreeNode<T>[])[i];
    // 如果节点满足过滤条件，则保留该节点，并递归过滤子节点
    if (filterFn(node, i, parentNode)) {
      const newNode = cloneDeep({ ...node, [customChildrenKey]: [] });
      if (node[customChildrenKey] && node[customChildrenKey].length > 0) {
        // 递归过滤子节点，并将过滤后的子节点添加到当前节点中
        newNode[customChildrenKey] = filterTree(node[customChildrenKey], filterFn, customChildrenKey, node);
      } else {
        newNode[customChildrenKey] = [];
      }
      filteredTree.push(newNode);
    }
  }
  return filteredTree;
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

/**
 * 字符串脱敏
 * @param str 需要脱敏的字符串
 * @returns 脱敏后的字符串
 */
export function desensitize(str: string): string {
  if (!str || typeof str !== 'string') {
    return '';
  }

  return str.replace(/./g, '*');
}

/**
 * 对话框标题动态内容字符限制
 * @param str 标题的动态内容
 * @returns 转化后的字符串
 */
export function characterLimit(str?: string, length?: number): string {
  if (!str) return '';
  if (str.length <= (length || 20)) {
    return str;
  }
  return `${str.slice(0, length || 20 - 3)}...`;
}

/**
 * 根据属性 key 查找树形数组中匹配的某个节点
 * @param trees 属性数组
 * @param targetKey 需要匹配的属性值
 * @param customKey 默认为 key，可自定义需要匹配的属性名
 * @returns 匹配的节点/null
 */
export function findNodeByKey<T>(
  trees: TreeNode<T>[],
  targetKey: string | number,
  customKey = 'key',
  dataKey: string | undefined = undefined
): TreeNode<T> | T | null {
  for (let i = 0; i < trees.length; i++) {
    const node = trees[i];
    if (dataKey ? node[dataKey]?.[customKey] === targetKey : node[customKey] === targetKey) {
      return node; // 如果当前节点的 key 与目标 key 匹配，则返回当前节点
    }

    if (Array.isArray(node.children) && node.children.length > 0) {
      const _node = findNodeByKey(node.children, targetKey, customKey, dataKey); // 递归在子节点中查找
      if (_node) {
        return _node; // 如果在子节点中找到了匹配的节点，则返回该节点
      }
    }
  }

  return null; // 如果在整个树形数组中都没有找到匹配的节点，则返回 null
}

/**
 * 根据 key 遍历树，并返回找到的节点路径和节点
 */
export function findNodePathByKey<T>(
  tree: TreeNode<T>[],
  targetKey: string,
  dataKey?: string,
  customKey = 'key'
): TreeNode<T> | null {
  for (let i = 0; i < tree.length; i++) {
    const node = tree[i];
    if (dataKey ? node[dataKey]?.[customKey] === targetKey : node[customKey] === targetKey) {
      return { ...node, treePath: [dataKey ? node[dataKey] : node] }; // 如果当前节点的 key 与目标 key 匹配，则返回当前节点
    }

    if (Array.isArray(node.children) && node.children.length > 0) {
      const result = findNodePathByKey(node.children, targetKey, dataKey, customKey); // 递归在子节点中查找
      if (result) {
        result.treePath.unshift(dataKey ? node[dataKey] : node);
        return result; // 如果在子节点中找到了匹配的节点，则返回该节点
      }
    }
  }

  return null;
}

/**
 * 根据 cityId 返回城市路径
 */
export function getCityPath(cityId: string | null): string {
  if (!cityId) return '';
  const nodePathObject = findNodePathByKey([CHINA_PCD, ...COUNTRIES_TREE], cityId, undefined, 'value');
  const nodePathName = (nodePathObject?.treePath || []).map((item: any) => item.label);
  return nodePathName.length === 1 ? nodePathName[0] : nodePathName.join('/');
}

/**
 * 返回添加节点下一个有效未命名name
 * @param existingNames 已存在名称列表
 * @param baseName 基础名称
 */
export function getNextAvailableName(existingNames: string[], baseName: string): string {
  const baseNamePattern = new RegExp(`^${baseName}(\\d+)$`);

  const existingSuffixes = existingNames.reduce((suffixes: number[], name: string) => {
    const match = baseNamePattern.exec(name);
    if (match) {
      suffixes.push(parseInt(match[1], 10));
    }
    return suffixes;
  }, []);

  if (existingSuffixes.length === 0) {
    return existingNames.includes(baseName) ? `${baseName}1` : baseName;
  }

  return `${baseName}${Math.max(...existingSuffixes) + 1}`;
}

/**
 * 分步处理分数表达式
 * @param str 分数表达式
 */
export function safeFractionConvert(str: string | number) {
  if (!str) {
    return 1;
  }
  if (typeof str === 'number') {
    return str;
  }
  const parts = str.split('/').map(Number); // 分割分子分母
  if (parts.length !== 2 || parts.some((e) => Number.isNaN(e))) return 1;
  return parts[0] / parts[1];
}

/**
 * 打开网页链接
 * @param url 链接地址
 */
export function openDocumentLink(url: string) {
  const a = document.createElement('a');
  a.href = url;
  a.target = '_blank';
  a.rel = 'noopener noreferrer'; // 防止打开页面控制当前页面
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
}

/**
 * 格式化时间
 * @param value 时间戳
 * @param type 类型
 */
export function formatTimeValue(value: string | number, type?: FormCreateFieldDateType) {
  if (value) {
    const date = dayjs(Number(value));
    switch (type) {
      case 'month':
        return date.format('YYYY-MM');
      case 'date':
        return date.format('YYYY-MM-DD');
      case 'datetime':
      default:
        return date.format('YYYY-MM-DD HH:mm:ss');
    }
  }
  return '-';
}

/**
 * 格式化数字
 * @param value 数字
 * @param type 类型
 */
export function formatNumberValue(value: string | number, item: FormCreateField) {
  if (value) {
    if (item.numberFormat === 'percent') {
      return item.precision ? `${Number(value).toFixed(item.precision)}%` : `${value}%`;
    }
    if (item.showThousandsSeparator) {
      return (item.precision ? Number(Number(value).toFixed(item.precision)) : Number(value)).toLocaleString('en-US');
    }
    return item.precision ? Number(value).toFixed(item.precision) : value.toString();
  }
  return '-';
}

/**
 * 下载文件
 * @param byte 字节流
 * @param fileName 文件名
 */
export const downloadByteFile = (byte: BlobPart, fileName: string) => {
  // 创建一个Blob对象
  const blob = new Blob([byte], { type: 'application/octet-stream' });
  // 创建一个URL对象，用于生成下载链接
  const url = window.URL.createObjectURL(blob);
  // 创建一个虚拟的<a>标签来触发下载
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName; // 设置下载文件的名称
  document.body.appendChild(link);
  link.click();

  // 释放URL对象
  window.URL.revokeObjectURL(url);
  document.body.removeChild(link);
};

/**
 * 获取每三位使用逗号隔开数字格式
 * @param number 目标值
 */

export function addCommasToNumber(number: number) {
  if (number === 0 || number === undefined) {
    return '0';
  }
  // 将数字转换为字符串
  const numberStr = number.toString();

  // 分割整数部分和小数部分
  const parts = numberStr.split('.');
  const integerPart = parts[0];
  const decimalPart = parts[1] || ''; // 如果没有小数部分，则设为空字符串

  // 对整数部分添加逗号分隔
  const integerWithCommas = integerPart.replace(/\B(?=(\d{3})+(?!\d))/g, ',');

  // 拼接整数部分和小数部分（如果有）
  const result = decimalPart ? `${integerWithCommas}.${decimalPart}` : integerWithCommas;

  return result;
}
// 是否是企业微信端打开
export function isWeComBrowser() {
  const ua = window.navigator.userAgent.toLowerCase();
  return ua.includes('wxwork'); // 企业微信 UA 一定包含 wxwork
}

/**
 * 国际单位数字缩写
 * @param number 目标值
 * return: 数字+单位
 */
export function abbreviateNumber(num: number) {
  const format = (value: number, unit: string) => ({
    value: `${Math.floor(value * 1000) / 1000}`,
    unit,
  });

  if (num >= 100000000) {
    return format(num / 100000000, 'B');
  }
  if (num >= 1000000) {
    return format(num / 1000000, 'M');
  }
  if (num >= 10000) {
    return format(num / 10000, 'W');
  }
  if (num >= 1000) {
    return format(num / 1000, 'K');
  }
  return {
    value: num.toString(),
    unit: '',
  };
}
