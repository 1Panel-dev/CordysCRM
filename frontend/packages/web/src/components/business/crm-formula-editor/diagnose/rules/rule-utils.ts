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
    return ['TEXT', 'TODAY', 'NOW', 'SUM', 'DAYS', 'CONCATENATE'].includes(node.name);
  }

  return false;
}

export function isTextNumberDateOrLogicalNode(node: ASTNode): boolean {
  return isTextNumberDateNode(node) || isLogicalNode(node);
}
