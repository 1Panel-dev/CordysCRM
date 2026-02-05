import FUNCTION_IMPL from './functions';
import { EvaluateContext, IRFieldNode, IRNode } from './types';

const DAY_MS = 24 * 60 * 60 * 1000;
const EXCEL_EPOCH = new Date('1899-12-30').getTime();

function dateToSerial(date: string | number | Date): number {
  let t: number;
  if (typeof date === 'number') {
    t = date;
  } else if (typeof date === 'string') {
    t = Date.parse(date);
  } else {
    t = date.getTime();
  }

  if (Number.isNaN(t)) return 0;

  return (t - EXCEL_EPOCH) / DAY_MS;
}

function parseDateWithPrecision(raw: string | number | Date): number {
  // number 可能是毫秒，也可能是 serial
  if (typeof raw === 'number') {
    // 明显是毫秒时间戳
    if (raw > 1e10) {
      return dateToSerial(raw);
    }
    // 否则认为是 serial
    return raw;
  }

  if (raw instanceof Date) {
    return dateToSerial(raw);
  }

  if (typeof raw === 'string') {
    // YYYY-MM
    if (/^\d{4}-\d{2}$/.test(raw)) {
      return dateToSerial(`${raw}-01`);
    }

    // YYYY-MM-DD
    if (/^\d{4}-\d{2}-\d{2}$/.test(raw)) {
      return dateToSerial(raw);
    }
  }

  return 0;
}
export function resolveFieldValue(rawVal: any, node: IRNode): number {
  if (rawVal == null || rawVal === '') return 0;

  // 日期
  if ((node as IRFieldNode)?.numberType === 'date') {
    return parseDateWithPrecision(rawVal);
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
          ctx.warn?.(`Unknown operator ${node.operator}`);
          return null;
      }
    }

    case 'function': {
      const fn = FUNCTION_IMPL[node.name];
      if (!fn) {
        ctx.warn?.(`Function ${node.name} not implemented`);
        return null;
      }
      const args = node.args.map((arg) => evaluateIR(arg, ctx));
      return fn(...args);
    }
    default:
      ctx.warn?.(`Unknown node type ${(node as IRNode).type}`);
      return null;
  }
}
