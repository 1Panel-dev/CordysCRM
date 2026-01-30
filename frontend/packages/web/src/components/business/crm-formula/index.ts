// todo xinxinwu
export interface FormulaContext {
  getValue: (fieldId: string) => any;
  warn: (msg: string) => void;
}

export type FormulaFunction = (args: string[], ctx: FormulaContext) => string;

const functionRegistry = new Map<string, FormulaFunction>();

export function registerFormulaFunction(name: string, fn: FormulaFunction) {
  functionRegistry.set(name.toUpperCase(), fn);
}

export function getFormulaFunction(name: string) {
  return functionRegistry.get(name.toUpperCase());
}

export function normalizeExpression(str: string) {
  const fullWidthMap: Record<string, string> = {
    '（': '(',
    '）': ')',
    '【': '(',
    '】': ')',
    '｛': '(',
    '｝': ')',
    '＜': '<',
    '＞': '>',
    '：': ':',
    '，': ',',
    '。': '.',
    '＋': '+',
    '－': '-',
    '×': '*',
    '÷': '/',
  };

  return str
    .replace(/[\u200B-\u200D\uFEFF]/g, '') // 去零宽字符
    .replace(/./g, (c) => fullWidthMap[c] || c); // 统一替换
}

export function normalizeToNumber(val: any): number {
  if (val == null || val === '') return 0; // 没填写默认按照0
  if (typeof val === 'number') return val;

  if (val instanceof Date) {
    return Math.floor(val.getTime() / 86400000);
  }

  if (Array.isArray(val)) {
    return val.reduce((s, v) => s + normalizeToNumber(v), 0);
  }

  const num = Number(String(val).replace(/,/g, ''));
  return Number.isNaN(num) ? 0 : num;
}

export function toDate(val: any): Date | null {
  if (val instanceof Date) return val;
  const d = new Date(val);
  return Number.isNaN(d.getTime()) ? null : d;
}

// 函数解析器（支持无限扩展）
export function resolveFunctions(expr: string, ctx: FormulaContext): string {
  return expr.replace(/([A-Z]+)\s*\(([^()]*)\)/gi, (_, fnName, argsStr) => {
    const fn = getFormulaFunction(fnName);
    if (!fn) return '0';

    if (argsStr.includes('，')) {
      ctx.warn('分隔符错误');
      return '0';
    }

    const args = argsStr
      .split(',')
      .map((a: any) => a.trim())
      .filter(Boolean);

    return fn(args, ctx);
  });
}

// 注册SUM函数
registerFormulaFunction('SUM', (args, ctx) => {
  if (args.length === 0) {
    ctx.warn('参数个数不能为空');
    return '0';
  }

  const values = args.map((arg) => {
    if (/^[A-Za-z0-9_]+$/.test(arg)) {
      return normalizeToNumber(ctx.getValue(arg));
    }
    const n = Number(arg);
    return Number.isNaN(n) ? 0 : n;
  });

  return `(${values.join('+')})`;
});

// 注册DAYS函数
registerFormulaFunction('DAYS', (args, ctx) => {
  if (args.length !== 2) {
    ctx.warn('DAYS 函数参数错误');
    return '0';
  }

  const end = toDate(ctx.getValue(args[0]));
  const start = toDate(ctx.getValue(args[1]));

  if (!end || !start) {
    ctx.warn('日期参数无效');
    return '0';
  }

  const diff = Math.floor((end.getTime() - start.getTime()) / 86400000);

  return String(diff);
});
