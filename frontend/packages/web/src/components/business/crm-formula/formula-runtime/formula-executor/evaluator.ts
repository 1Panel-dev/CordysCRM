import { IRNodeType } from '@lib/shared/enums/formula';

import {
  EvaluateContext,
  IRBinaryNode,
  IRLiteralNode,
  IRNode,
} from '@/components/business/crm-formula/formula-runtime/types';

import { functionRegistry } from '../function-registry';
import { localDateToExcelSerial } from '../runtime/excel-date';
import { excelCompare, normalizeSerialToSecond, toNumber } from '../runtime/excel-runtime';

/**
 * Excel 日期计算的基准时间戳
 *
 * Excel 使用 1899-12-30 作为日期序列号的起点（序列号 0）
 * 这是因为 Lotus 1-2-3 中的历史遗留问题，Excel 为保持兼容性沿用了这个基准
 *
 * 用途：用于在 Excel 日期序列号与 JavaScript Date 对象之间进行转换
 * 例如：Excel 序列号 44197 对应 2021-01-01
 */
const DAY_MS = 24 * 60 * 60 * 1000;
const EXCEL_EPOCH = new Date(1899, 11, 30).getTime();

function dateToSerial(date: Date | string): number {
  let t: number;

  if (date instanceof Date) {
    t = new Date(
      date.getFullYear(),
      date.getMonth(),
      date.getDate(),
      date.getHours(),
      date.getMinutes(),
      date.getSeconds()
    ).getTime();
  } else {
    const m = date.match(/^(\d{4})-(\d{2})-(\d{2})(?:\s+(\d{2}):(\d{2})(?::(\d{2}))?)?$/);
    if (!m) return 0;

    const [, y, mo, d, h = '0', mi = '0', s = '0'] = m;
    const localDate = new Date(Number(y), Number(mo) - 1, Number(d), Number(h), Number(mi), Number(s));

    t = localDate.getTime();
  }

  return (t - EXCEL_EPOCH) / DAY_MS;
}

function parseDateWithPrecision(raw: string | number | Date): number {
  if (typeof raw === 'number') {
    // 毫秒时间戳
    if (raw > 1e10) {
      const d = new Date(raw);
      return localDateToExcelSerial(d);
    }

    // 否则认为是 Excel serial
    return raw;
  }

  if (raw instanceof Date) {
    return localDateToExcelSerial(raw);
  }

  if (typeof raw === 'string') {
    // YYYY-MM
    if (/^\d{4}-\d{2}$/.test(raw)) {
      return dateToSerial(`${raw}-01`);
    }

    // YYYY-MM-DD / YYYY-MM-DD HH:mm:ss
    if (/^\d{4}-\d{2}-\d{2}/.test(raw)) {
      return dateToSerial(raw);
    }
  }

  return 0;
}

export function resolveFieldValue(rawVal: any, node: IRNode, ctx?: EvaluateContext): any {
  const meta = node.type === 'field' ? ctx?.getFieldMeta?.(node.fieldId) : undefined;
  if (rawVal == null || rawVal === '') {
    if (meta?.valueType === 'date' || meta?.valueType === 'number') {
      return null;
    }

    return '';
  }

  if (Array.isArray(rawVal)) {
    return rawVal;
  }

  const valueType = meta?.valueType;
  const numberType = meta?.numberType;

  // ---------- date ----------
  if (valueType === 'date') {
    const serial = parseDateWithPrecision(rawVal);
    return serial;
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
        return values.map((v) => {
          const runtimeValue = ctx.resolveFieldRuntimeValue ? ctx.resolveFieldRuntimeValue(node.fieldId, v) : v;
          return resolveFieldValue(runtimeValue, node, ctx);
        });
      }

      // 普通字段
      const rawValue = ctx.getScalarFieldValue(node.fieldId, ctx.context);
      const runtimeValue = ctx.resolveFieldRuntimeValue
        ? ctx.resolveFieldRuntimeValue(node.fieldId, rawValue)
        : rawValue;

      return resolveFieldValue(runtimeValue, node, ctx);
    }

    case IRNodeType.Binary: {
      const left = evaluateIR(node.left, ctx);
      const right = evaluateIR(node.right, ctx);
      switch (node.operator) {
        case '+':
          return left + right;
        case '-': {
          const l = toNumber(left, ctx.warn);
          const r = toNumber(right, ctx.warn);
          if (Number.isNaN(l) || Number.isNaN(r)) {
            ctx.warn?.(`[formula] invalid subtraction operands: ${left} - ${right}`);
            return 0;
          }
          return l - r;
        }
        case '*': {
          const l = toNumber(left, ctx.warn);
          const r = toNumber(right, ctx.warn);
          if (Number.isNaN(l) || Number.isNaN(r)) {
            ctx.warn?.(`[formula] invalid multiplication operands: ${left} * ${right}`);
            return 0;
          }
          return l * r;
        }
        case '/': {
          const l = toNumber(left, ctx.warn);
          const r = toNumber(right, ctx.warn);
          if (Number.isNaN(l) || Number.isNaN(r)) {
            ctx.warn?.(`[formula] invalid division operands: ${left} / ${right}`);
            return 0;
          }
          return r === 0 ? 0 : l / r;
        }
        default:
          ctx.warn?.(`Unknown operator ${(node as IRBinaryNode)?.operator}`);
          return null;
      }
    }

    case IRNodeType.Compare: {
      let left = evaluateIR(node.left, ctx);
      let right = evaluateIR(node.right, ctx);

      const leftMeta = node.left.type === IRNodeType.Field ? ctx.getFieldMeta?.(node.left.fieldId) : undefined;

      const rightMeta = node.right.type === IRNodeType.Field ? ctx.getFieldMeta?.(node.right.fieldId) : undefined;

      const leftIsDate = leftMeta?.valueType === 'date';
      const rightIsDate = rightMeta?.valueType === 'date';

      // 只有日期比较才裁秒
      if (leftIsDate || rightIsDate) {
        if (typeof left === 'number') {
          left = normalizeSerialToSecond(left);
        }
        if (typeof right === 'number') {
          right = normalizeSerialToSecond(right);
        }
      }

      return excelCompare(left, right, node.operator);
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
