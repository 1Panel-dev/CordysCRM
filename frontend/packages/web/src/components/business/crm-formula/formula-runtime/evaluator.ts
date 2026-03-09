import { IRNodeType } from '@lib/shared/enums/formula';

import { functionRegistry } from './function-registry';
import { excelCompare } from './runtime/excel-runtime';
import { EvaluateContext, IRBinaryNode, IRLiteralNode, IRNode } from './types';

// todo xinxinwu
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

export function resolveFieldValue(rawVal: any, node: IRNode, ctx?: EvaluateContext): any {
  if (rawVal == null || rawVal === '') return 0;
  const meta = node.type === 'field' ? ctx?.getFieldMeta?.(node.fieldId) : undefined;

  const valueType = meta?.valueType;
  const numberType = meta?.numberType;

  // ---------- date ----------
  if (valueType === 'date') {
    const serial = parseDateWithPrecision(rawVal);
    return Math.floor(serial);
  }

  // ---------- string ----------
  if (valueType === 'string') {
    return String(rawVal ?? '');
  }

  // ---------- boolean ----------
  if (valueType === 'boolean') {
    return Boolean(rawVal);
  }

  // ---------- number ----------
  let num: number;

  if (typeof rawVal === 'number') {
    num = rawVal;
  } else {
    num = Number(String(rawVal).replace(/,/g, '').replace(/%/g, ''));
  }

  if (Number.isNaN(num)) return 0;

  if (numberType === 'percent') {
    return num / 100;
  }

  return num;
}

function compareValues(op: '=' | '<>' | '>' | '>=' | '<' | '<=', left: any, right: any): boolean {
  // 先做最小语义：两边都是 number 用数值，否则用原值比较/字符串相等
  const bothNumber = typeof left === 'number' && typeof right === 'number';

  // '=' / '<>'：非 number 时按“字符串”比较更贴近 Excel 文本比对
  if (op === '=' || op === '<>') {
    const eq = bothNumber ? left === right : String(left ?? '') === String(right ?? '');
    return op === '=' ? eq : !eq;
  }

  // 其它比较：优先 number，否则退化为字符串比较（先跑通，后面再升级 Excel coercion）
  const l = bothNumber ? left : String(left ?? '');
  const r = bothNumber ? right : String(right ?? '');

  switch (op) {
    case '>':
      return l > r;
    case '>=':
      return l >= r;
    case '<':
      return l < r;
    case '<=':
      return l <= r;
    default:
      return false;
  }
}

export default function evaluateIR(node: IRNode, ctx: EvaluateContext): any {
  switch (node.type) {
    case IRNodeType.Literal: {
      const literal = node as IRLiteralNode;

      if (literal.valueType === 'number') {
        return Number(literal.value) || 0;
      }

      if (literal.valueType === 'string') {
        return String(literal.value ?? '');
      }

      return literal.value;
    }

    case IRNodeType.Field: {
      // 子表字段：返回一组 number
      if (node.fieldId.includes('.')) {
        const values = ctx.getTableColumnValues(node.fieldId);
        return values.map((v) => resolveFieldValue(v, node, ctx));
      }

      // 普通字段
      const rawValue = ctx.getScalarFieldValue(node.fieldId, ctx.context);
      return resolveFieldValue(rawValue, node, ctx);
    }

    case IRNodeType.Binary: {
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
          ctx.warn?.(`Unknown operator ${(node as IRBinaryNode)?.operator}`);
          return null;
      }
    }

    case IRNodeType.Compare: {
      const left = evaluateIR(node.left, ctx);
      const right = evaluateIR(node.right, ctx);
      return excelCompare(left, right, node.operator, ctx.warn);
    }

    case IRNodeType.Function: {
      const spec = functionRegistry.get(node.name);

      if (!spec) {
        ctx.warn?.(`Function ${node.name} not implemented`);
        return null;
      }

      if (spec.lazy) {
        const thunks = node.args.map((arg) => {
          return () => evaluateIR(arg, ctx);
        });

        return spec.fn(ctx, ...thunks);
      }

      const args = node.args.map((arg) => evaluateIR(arg, ctx));
      return spec.fn(ctx, ...args);
    }
    default:
      ctx.warn?.(`Unknown node type ${(node as IRNode).type}`);
      return null;
  }
}
