import FUNCTION_IMPL from './functions';
import { EvaluateContext, IRFieldNode, IRNode } from './types';

export function resolveFieldValue(rawVal: any, node: IRNode): number {
  if (rawVal == null || rawVal === '') return 0;

  // 日期
  if ((node as IRFieldNode)?.numberType === 'date') {
    // 时间戳
    if (typeof rawVal === 'number') return rawVal;

    // 字符串日期
    if (typeof rawVal === 'string') {
      const t = Date.parse(rawVal);
      return Number.isNaN(t) ? 0 : t;
    }
  }

  // 数字统一解析
  let num: number;
  if (typeof rawVal === 'number') {
    num = rawVal;
  } else {
    num = Number(String(rawVal).replace(/,/g, '').replace(/%/g, ''));
  }

  if (Number.isNaN(num)) return 0;

  // 语义处理（只在这里）
  if ((node as IRFieldNode)?.numberType === 'percent') {
    return num / 100;
  }

  return num;
}

export default function evaluateIR(node: IRNode, ctx: EvaluateContext): any {
  switch (node.type) {
    case 'number':
      return node.value;

    case 'field': {
      // 子表字段：返回一组 number
      if (node.fieldId.includes('.')) {
        const values = ctx.getTableColumnValues(node.fieldId);
        return values.map((v) => resolveFieldValue(v, node));
      }

      // 普通字段
      const rawValue = ctx.getScalarFieldValue(node.fieldId, ctx.context);
      return resolveFieldValue(rawValue, node);
    }

    case 'binary': {
      const left = evaluateIR(node.left, ctx);
      const right = evaluateIR(node.right, ctx);
      switch (node.operator) {
        case '+':
          return left + right;
        case '-':
          return left - right;
        case '*':
          return left * right;
        case '/':
          return right === 0 ? 0 : left / right;
        default:
          throw new Error(`Unknown operator ${node.operator}`);
      }
    }

    case 'function': {
      const fn = FUNCTION_IMPL[node.name];
      if (!fn) throw new Error(`Function ${node.name} not implemented`);
      const args = node.args.map((arg) => evaluateIR(arg, ctx));
      return fn(...args);
    }
    default:
      throw new Error(`Unknown node type ${(node as IRNode).type}`);
  }
}
