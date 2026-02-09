import FUNCTION_IMPL from './functions';
import { EvaluateContext, IRFieldNode, IRNode } from './types';

const DAY_MS = 24 * 60 * 60 * 1000;
const EXCEL_EPOCH = new Date(1899, 11, 30).getTime(); // Excel 的日期序列号是从 1900-01-01 开始的，但为了兼容 Lotus 1-2-3 的错误，Excel 实际上把 1900-02-29 也当成了一个有效日期，所以 Excel 的 epoch 是 1899-12-30

function dateToSerial(date: Date | string): number {
  let t: number;

  if (date instanceof Date) {
    t = date.getTime();
  } else {
    // YYYY-MM-DD / YYYY-MM-DD HH:mm:ss
    const m = date.match(/^(\d{4})-(\d{2})-(\d{2})(?:\s+(\d{2}):(\d{2})(?::(\d{2}))?)?$/);
    if (!m) return 0;

    const [, y, mo, d, h = '0', mi = '0', s = '0'] = m;
    // 月份在 Date 构造函数里是从 0 开始的，所以要减 1
    const localDate = new Date(Number(y), Number(mo) - 1, Number(d), Number(h), Number(mi), Number(s));

    t = localDate.getTime();
  }

  return (t - EXCEL_EPOCH) / DAY_MS;
}

function parseDateWithPrecision(raw: string | number | Date): number {
  //  number 在 date 语义里，只允许是 Excel serial
  if (typeof raw === 'number') {
    // 明显是毫秒时间戳
    if (raw > 1e10) {
      return (raw - EXCEL_EPOCH) / DAY_MS;
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
    if (/^\d{4}-\d{2}-\d{2}/.test(raw)) {
      return dateToSerial(raw);
    }
  }

  return 0;
}
export function resolveFieldValue(rawVal: any, node: IRNode): number {
  if (rawVal == null || rawVal === '') return 0;

  // 日期
  if ((node as IRFieldNode)?.numberType === 'date') {
    const serial = parseDateWithPrecision(rawVal);
    // 在这里统一 day精度
    return Math.floor(serial);
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
