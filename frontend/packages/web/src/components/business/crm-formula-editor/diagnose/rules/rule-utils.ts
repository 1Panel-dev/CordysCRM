import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';

import { ASTNode } from '../../types';

export function isColumnField(fieldId: string) {
  return fieldId.includes('.');
}

export function isLogicalNode(node: ASTNode): boolean {
  if (!node) return false;

  if (node.type === 'compare') return true;

  if (node.type === 'literal' && node.valueType === 'boolean') return true;

  if (node.type === 'function') {
    return ['AND'].includes(node.name);
  }

  return false;
}

export function isTextNumberDateNode(node: ASTNode): boolean {
  if (!node) return false;

  if (node.type === 'literal') {
    return ['string', 'number'].includes(node.valueType);
  }

  if (node.type === 'field') {
    if (node.fieldType === FieldTypeEnum.DATE_TIME) return true;
    if (node.fieldType === FieldTypeEnum.INPUT_NUMBER) return true;

    // 其他字段类型按“文本”处理
    return true;
  }

  if (node.type === 'function') {
    // 当前版本这些函数返回文本 / 数字 / 日期
    return ['TEXT', 'TODAY', 'NOW', 'SUM', 'DAYS', 'CONCATENATE', 'IFS'].includes(node.name);
  }

  return false;
}

export function isTextNumberDateOrLogicalNode(node: ASTNode): boolean {
  return isTextNumberDateNode(node) || isLogicalNode(node);
}

/**
 * AND 参数允许：
 * - 逻辑表达式 / 布尔字面量
 * - 数字字面量
 * - 数字字段
 * - 日期字段（Excel 里日期本质是 number serial）
 * - 返回 number/date 的函数
 *
 * AND 参数不允许：
 * - 文本字面量
 * - 文本字段
 * - TEXT / CONCATENATE 等文本函数
 */
export function isLogicalLikeNodeForAnd(node: ASTNode): boolean {
  if (!node) return false;

  // 原生逻辑值
  if (isLogicalNode(node)) return true;

  // number literal 允许
  if (node.type === 'literal') {
    return node.valueType === 'number';
  }

  // 数字字段 / 日期字段允许
  if (node.type === 'field') {
    return [FieldTypeEnum.INPUT_NUMBER, FieldTypeEnum.DATE_TIME].includes(node.fieldType as any);
  }

  // 返回数值 / 日期的函数允许
  if (node.type === 'function') {
    return ['SUM', 'DAYS', 'NOW', 'TODAY'].includes(node.name);
  }

  // 二元数值表达式也允许，例如：数字1 + 数字2
  if (node.type === 'binary') {
    return ['+', '-', '*', '/'].includes(node.operator);
  }

  return false;
}
