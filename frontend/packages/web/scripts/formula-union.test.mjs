import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { test } from 'node:test';
import { runInNewContext } from 'node:vm';
import ts from 'typescript';

const source = readFileSync(
  new URL('../src/components/business/crm-formula/formula-runtime/functions/union.ts', import.meta.url),
  'utf8'
);
const compiled = ts.transpile(source, { module: ts.ModuleKind.CommonJS });
const exports = {};
runInNewContext(compiled, { exports });
const union = exports.default;

const formulaUtilsSource = readFileSync(
  new URL('../src/components/business/crm-formula/utils.ts', import.meta.url),
  'utf8'
);
const displayValueStart = formulaUtilsSource.indexOf('export function getFormulaDataSourceDisplayValue(');
const displayValueEnd = formulaUtilsSource.indexOf('\nexport function flatAllFields(', displayValueStart);
assert.notEqual(displayValueStart, -1, 'Missing production data source display resolver');
assert.notEqual(displayValueEnd, -1, 'Missing next production utility export');
const displayValueSource = formulaUtilsSource.slice(displayValueStart, displayValueEnd).replace('export ', '');
const displayValueExports = {};
runInNewContext(
  ts.transpile(`${displayValueSource}\nexports.getFormulaDataSourceDisplayValue = getFormulaDataSourceDisplayValue;`),
  {
    exports: displayValueExports,
  }
);
const getFormulaDataSourceDisplayValue = displayValueExports.getFormulaDataSourceDisplayValue;

test('UNION 合并多个数字和文本标量', () => {
  assert.equal(union({}, 12, 'abc', 3), '12abc3');
});

test('UNION 按顺序展开标量与数字/文本数组，不去重', () => {
  assert.equal(union({}, 'a', [1, 'b', 1], ['c', 2]), 'a1b1c2');
});

test('UNION 将空值识别为空串并保持其他值', () => {
  assert.equal(union({}, null, undefined, '', '  ', [0, null, 'x']), '0x');
});

test('公式数据源字段按 initialOptions 将 ID 映射为 name', () => {
  assert.equal(
    JSON.stringify(
      getFormulaDataSourceDisplayValue({}, 'sourceId', 'customer-1', [{ id: 'customer-1', name: 'Acme' }])
    ),
    '["Acme"]'
  );
});

test('公式数据源数组按 initialOptions 映射名称并保留未匹配值', () => {
  assert.equal(
    JSON.stringify(
      getFormulaDataSourceDisplayValue({}, 'sourceIds', ['customer-1', 'missing'], [{ id: 'customer-1', name: 'Acme' }])
    ),
    '["Acme","missing"]'
  );
});

test('initialOptions 为空时按字段别名使用运行时数据源选项', () => {
  assert.equal(
    JSON.stringify(
      getFormulaDataSourceDisplayValue(
        { sourceBusinessKey: { parserName: true, options: [{ id: 'customer-1', name: 'Acme' }] } },
        'tableId.sourceBusinessKey',
        'customer-1',
        []
      )
    ),
    '["Acme"]'
  );
});
