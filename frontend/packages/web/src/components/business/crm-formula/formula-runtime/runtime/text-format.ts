// /formula-runtime/runtime/text-format.ts
import { toNumber, toString } from './excel-runtime';

/**
 * Excel serial -> Date
 */
const DAY_MS = 24 * 60 * 60 * 1000;
const EXCEL_EPOCH = new Date(1899, 11, 30).getTime();

function serialToDate(serial: number): Date {
  const base = new Date(1899, 11, 30);

  const days = Math.floor(serial);

  const date = new Date(base.getFullYear(), base.getMonth(), base.getDate() + days);

  const fraction = serial - days;

  if (fraction > 0) {
    const seconds = Math.round(fraction * 86400);

    date.setSeconds(date.getSeconds() + seconds);
  }

  return date;
}

function pad(value: number, len = 2): string {
  return String(value).padStart(len, '0');
}

function isDateFormat(format: string): boolean {
  const fmt = format.toLowerCase();
  return /y|m|d|h|s/.test(fmt);
}

function hasThousands(format: string): boolean {
  return format.includes(',');
}

function getDecimalPlaces(format: string): number {
  const match = format.match(/\.(0+)/);
  return match ? match[1].length : 0;
}

function isPercentFormat(format: string): boolean {
  return format.includes('%');
}

function isZeroPadFormat(format: string): boolean {
  return /^0+$/.test(format);
}

/**
 * 解决 Excel 风格格式中 mm 同时表示“月/分”的问题
 *
 * 约定支持：
 * - yyyy-mm-dd
 * - yyyy/mm/dd
 * - yyyymmdd
 * - yyyy-mm-dd hh:mm:ss
 * - hh:mm:ss
 *
 * 规则：
 * - 先替换 yyyy / dd / hh / ss
 * - 处于 "hh:mm" 或 ":mm:" 或 ":mm" 语境中的 mm 视为“分钟”
 * - 其余 mm 视为“月份”
 */
function formatDateByPattern(date: Date, format: string): string {
  const yyyy = String(date.getFullYear());
  const month = pad(date.getMonth() + 1);
  const day = pad(date.getDate());
  const hour = pad(date.getHours());
  const minute = pad(date.getMinutes());
  const second = pad(date.getSeconds());

  let result = format;

  // 先替换不会冲突的 token
  result = result.replace(/yyyy/g, yyyy).replace(/dd/g, day).replace(/hh/g, hour).replace(/ss/g, second);

  // 把时间部分的 mm 先替换成分钟
  // hh:mm
  result = result.replace(/hh:mm/g, `${hour}:${minute}`);
  // :mm:
  result = result.replace(/:mm:/g, `:${minute}:`);
  // :mm 结尾
  result = result.replace(/:mm\b/g, `:${minute}`);

  // 剩余的 mm 统一当月份
  result = result.replace(/mm/g, month);

  return result;
}

/**
 * 仅支持一版常用 Excel 风格数字格式：
 * 0
 * 0.00
 * #,##0
 * #,##0.00
 * 0%
 * 0.00%
 * 00000
 */
export function formatNumberByPattern(value: number, format: string): string {
  if (!Number.isFinite(value)) return '';

  // 00000 这种补零
  if (isZeroPadFormat(format)) {
    const width = format.length;
    return String(Math.trunc(value)).padStart(width, '0');
  }

  let num = value;
  let suffix = '';

  if (isPercentFormat(format)) {
    num *= 100;
    suffix = '%';
  }

  const decimalPlaces = getDecimalPlaces(format);
  let result = num.toFixed(decimalPlaces);

  if (hasThousands(format)) {
    const [intPart, decimalPart] = result.split('.');
    const formattedInt = Number(intPart).toLocaleString('en-US');
    result = decimalPart != null ? `${formattedInt}.${decimalPart}` : formattedInt;
  }

  return result + suffix;
}

/**
 * 入口：TEXT(value, format)
 */
export function formatTextValue(value: any, format: any): string {
  const fmt = toString(format).trim();

  if (!fmt) {
    return toString(value);
  }

  // 日期格式
  if (isDateFormat(fmt)) {
    let date: Date | null = null;

    if (value instanceof Date) {
      date = value;
    } else {
      const num = toNumber(value);
      if (!Number.isNaN(num)) {
        date = serialToDate(num);
      }
    }

    if (!date || Number.isNaN(date.getTime())) {
      return '';
    }

    return formatDateByPattern(date, fmt);
  }

  // 数字格式
  const num = toNumber(value);
  if (!Number.isNaN(num)) {
    return formatNumberByPattern(num, fmt);
  }

  // 兜底：按字符串返回
  return toString(value);
}
