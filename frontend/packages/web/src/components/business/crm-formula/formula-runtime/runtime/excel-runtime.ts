/**
 * Excel coercion + compare runtime
 */
import { CompareOperator } from '../types';

const SECOND_MS = 1000;
const DAY_MS = 24 * 60 * 60 * 1000;

/**
 * 把 Excel serial 裁到秒精度
 */
export function normalizeSerialToSecond(serial: number): number {
  return (Math.floor((serial * DAY_MS) / SECOND_MS) * SECOND_MS) / DAY_MS;
}

// 可选：用于 debug/告警（不想用就不传）
export type RuntimeWarn = (msg: string) => void;

function normalizeScalar(value: any, _?: RuntimeWarn): any {
  if (Array.isArray(value)) {
    if (value.length === 0) {
      return 0;
    }
    // 这里策略：隐式取第一个 / 取0 / 报警
    return value[0];
  }

  return value;
}
/**
 * Excel boolean coercion
 */
export function toBoolean(value: any, warn?: RuntimeWarn): boolean {
  const v0 = normalizeScalar(value, warn);
  if (v0 == null) return false;

  if (typeof v0 === 'boolean') return v0;
  if (typeof v0 === 'number') return v0 !== 0;

  if (typeof v0 === 'string') {
    const v = v0.trim().toLowerCase();
    if (v === '') return false;
    if (v === 'true') return true;
    if (v === 'false') return false;
    return true;
  }

  return Boolean(v0);
}

/**
 * Excel number coercion
 */
export function toNumber(value: any, warn?: RuntimeWarn): number {
  const v0 = normalizeScalar(value, warn);
  if (v0 == null) return 0;

  if (typeof v0 === 'number') return v0;
  if (typeof v0 === 'boolean') return v0 ? 1 : 0;

  if (typeof v0 === 'string') {
    const trimmed = v0.trim();
    if (trimmed === '') return 0;
    const n = Number(trimmed);
    if (!Number.isNaN(n)) return n;
  }

  return NaN;
}

/**
 * Excel string coercion
 */
export function toString(value: any, warn?: RuntimeWarn): string {
  const v0 = normalizeScalar(value, warn);
  if (v0 == null) return '';

  if (typeof v0 === 'string') return v0;
  if (typeof v0 === 'number') return String(v0);
  if (typeof v0 === 'boolean') return v0 ? 'TRUE' : 'FALSE';

  return String(v0);
}

function compareEqual(a: any, b: any, warn?: RuntimeWarn): boolean {
  const left = normalizeScalar(a, warn);
  const right = normalizeScalar(b, warn);

  // number vs string
  if (typeof left === 'number' && typeof right === 'string') {
    const n = toNumber(right, warn);
    if (!Number.isNaN(n)) return left === n;
  }
  if (typeof left === 'string' && typeof right === 'number') {
    const n = toNumber(left, warn);
    if (!Number.isNaN(n)) return n === right;
  }

  // boolean compare（含 boolean vs string / number）
  if (typeof left === 'boolean' || typeof right === 'boolean') {
    return toBoolean(left, warn) === toBoolean(right, warn);
  }

  return left === right;
}

/**
 * Excel compare
 */
export function excelCompare(left: any, right: any, operator: CompareOperator, warn?: RuntimeWarn): boolean {
  switch (operator) {
    case '=':
      return compareEqual(left, right, warn);

    case '<>':
      return !compareEqual(left, right, warn);

    case '>': {
      const l = toNumber(left, warn);
      const r = toNumber(right, warn);
      if (Number.isNaN(l) || Number.isNaN(r)) return false;
      return l > r;
    }

    case '<': {
      const l = toNumber(left, warn);
      const r = toNumber(right, warn);
      if (Number.isNaN(l) || Number.isNaN(r)) return false;
      return l < r;
    }

    case '>=': {
      const l = toNumber(left, warn);
      const r = toNumber(right, warn);
      if (Number.isNaN(l) || Number.isNaN(r)) return false;
      return l >= r;
    }

    case '<=': {
      const l = toNumber(left, warn);
      const r = toNumber(right, warn);
      if (Number.isNaN(l) || Number.isNaN(r)) return false;
      return l <= r;
    }

    default:
      return false;
  }
}
