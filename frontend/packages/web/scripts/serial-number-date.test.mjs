import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { test } from 'node:test';
import { runInNewContext } from 'node:vm';
import { computed, effectScope, nextTick, ref, watch } from 'vue';
import ts from 'typescript';

// 执行生产组件中的响应式逻辑和保存载荷构造，避免复制实现或引入测试框架。
const source = readFileSync(
  new URL('../src/components/business/crm-form-design/components/formAttrConfig/fieldAttr.vue', import.meta.url),
  'utf8'
).match(/<script setup lang="ts">([^]*?)<\/script>/)[1];
const ast = ts.createSourceFile('fieldAttr.ts', source, ts.ScriptTarget.Latest, true);
const declarations = ast.statements.filter((statement) => {
  const text = statement.getText(ast);
  if (ts.isVariableStatement(statement)) {
    return /const (serialNumber|previewValue|currentField(HasAccept|HasLimitSize|LimitSize)|showSumColumn)/.test(text);
  }
  return ts.isExpressionStatement(statement) && text.startsWith('watch(') && text.includes('serialNumberRules');
});
const compiled = ts.transpile(declarations.map((statement) => statement.getText(ast)).join('\n'));
const configSource = readFileSync(
  new URL('../src/components/business/crm-form-design-drawer/useFormDesignConfig.ts', import.meta.url),
  'utf8'
)
  .replace(/^import[^;]*;\s*/gm, '')
  .replace(/export /g, '');

test('自定义日期编辑、回显、保存及异步预览保持一致', async () => {
  const scope = effectScope();
  const fieldConfig = ref({ id: 'first', type: 'SERIAL_NUMBER', serialNumberRules: ['Opp', '-', 'yyyyMM', '-', 6] });
  const timers = new Map();
  const requests = [];
  let timerId = 0;
  const context = {
    ref,
    computed,
    watch,
    nextTick,
    fieldConfig,
    FieldTypeEnum: { SERIAL_NUMBER: 'SERIAL_NUMBER' },
    t: (key) => key,
    setTimeout: (callback) => {
      timers.set(++timerId, callback);
      return timerId;
    },
    clearTimeout: (id) => timers.delete(id),
    previewSerialNumberDate: (pattern) => new Promise((resolve, reject) => requests.push({ pattern, resolve, reject })),
    useI18n: () => ({ t: (value) => value }),
    useMessage: () => ({ error: assert.fail }),
    safeFractionConvert: (value) => value,
  };
  try {
    const state = scope.run(() =>
      runInNewContext(
        `${compiled}\n({ serialNumberRules3, serialNumberDatePreview, serialNumberDateError, previewValue });`,
        context
      )
    );
    const useConfig = runInNewContext(`${ts.transpile(configSource)}\nuseFormDesignConfig;`, context);
    const config = scope.run(() => useConfig({ formKey: ref('opportunity') }));
    const runTimers = () => {
      const callbacks = [...timers.values()];
      timers.clear();
      return Promise.all(callbacks.map((callback) => callback()));
    };

    let pending = runTimers();
    requests.at(-1).resolve('202610');
    await pending;
    assert.equal(state.previewValue.value, 'Opp-202610-000001');

    state.serialNumberRules3.value = 'yyyy-MM-dd';
    await nextTick();
    assert.equal(fieldConfig.value.serialNumberRules[2], 'yyyy-MM-dd');
    const stale = runTimers();
    const staleRequest = requests.at(-1);
    state.serialNumberRules3.value = 'yyyy/MM/dd';
    await nextTick();
    pending = runTimers();
    requests.at(-1).resolve('2026/10/13');
    await pending;
    staleRequest.resolve('2026-10-13');
    await stale;
    assert.equal(state.previewValue.value, 'Opp-2026/10/13-000001');

    config.setFormConfigDetail({ fields: [fieldConfig.value], formProp: {} });
    const saved = JSON.parse(JSON.stringify(config.buildSavePayload()));
    assert.equal(saved.fields[0].serialNumberRules[2], 'yyyy/MM/dd');
    config.setFormConfigDetail({ fields: saved.fields, formProp: saved.formProp });
    assert.equal(config.fieldList.value[0].serialNumberRules[2], 'yyyy/MM/dd');

    fieldConfig.value = { id: 'second', type: 'SERIAL_NUMBER', serialNumberRules: ['Case', '/', 'yyyy', '/', 4] };
    await nextTick();
    assert.equal(state.serialNumberRules3.value, 'yyyy');
    assert.equal(saved.fields[0].serialNumberRules[2], 'yyyy/MM/dd');
    pending = runTimers();
    requests.at(-1).resolve('2026');
    await pending;
    assert.equal(state.previewValue.value, 'Case/2026/0001');

    state.serialNumberRules3.value = '';
    await nextTick();
    pending = runTimers();
    requests.at(-1).resolve(null);
    await pending;
    assert.equal(state.serialNumberDateError.value, 'crmFormDesign.serialNumberDateInvalid');
    assert.equal(state.previewValue.value, '');
    assert.equal(fieldConfig.value.serialNumberRules[2], '');

    state.serialNumberRules3.value = 'yyyyMMddHHmmss';
    await nextTick();
    pending = runTimers();
    assert.equal(requests.at(-1).pattern, 'yyyyMMddHHmmss');
    requests.at(-1).resolve(null);
    await pending;
    assert.equal(state.serialNumberDateError.value, 'crmFormDesign.serialNumberDateInvalid');
    assert.equal(state.previewValue.value, '');

    state.serialNumberRules3.value = 'yyyyMMdd';
    await nextTick();
    pending = runTimers();
    requests.at(-1).reject(new Error('预览服务不可用'));
    await pending;
    assert.equal(state.serialNumberDateError.value, 'crmFormDesign.serialNumberDatePreviewFailed');
  } finally {
    scope.stop();
  }
});
