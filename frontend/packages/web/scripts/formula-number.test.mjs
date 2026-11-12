import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { test } from 'node:test';
import { runInNewContext } from 'node:vm';
import ts from 'typescript';

// 直接执行生产函数，避免在用例内复制数值格式化算法或启动 Vue 环境。
const source = readFileSync(new URL('../src/components/business/crm-formula/utils.ts', import.meta.url), 'utf8');
const functions = ['normalizeFormulaNumber', 'keepDecimal'].map((name) => {
  const match = source.match(new RegExp(`export function ${name}\\([^]*?\\n}`));
  assert.ok(match, `Missing production function: ${name}`);
  return match[0].replace('export ', '');
});
const compiled = ts.transpile(functions.join('\n'));
const keepDecimal = runInNewContext(`${compiled}\nkeepDecimal;`);

test('科学计数法、负数与浮点尾差按精度四舍五入并保留末尾零', () => {
  for (const [input, precision, expected] of [
    [1e-7, 2, '0.00'],
    [1.234e-7, 2, '0.00'],
    [-1.234e-7, 2, '-0.00'],
    [1.239, 2, '1.24'],
    [-1.239, 2, '-1.24'],
    [0.1 + 0.2, 2, '0.30'],
    [123.99, 0, '124'],
    [1.23459, 4, '1.2346'],
    [1e21, 2, '1e+21'],
  ]) {
    assert.equal(keepDecimal(input, precision), expected, `input=${input}, precision=${precision}`);
  }
});

test('非有限数值维持原有语义', () => {
  assert.ok(Number.isNaN(keepDecimal(NaN, 2)));
  assert.equal(keepDecimal(Infinity, 2), Infinity);
});
