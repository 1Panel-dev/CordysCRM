<template>
  <n-tooltip trigger="hover" placement="top">
    <template #trigger>
      <inputNumber
        v-model:value="value"
        path="fieldValue"
        :field-config="fieldConfig"
        :is-sub-table-field="props.isSubTableField"
        :is-sub-table-render="props.isSubTableRender"
        :form-config="props.formConfig"
        @change="handleChange"
      />
    </template>
    {{ formulaTooltip }}
  </n-tooltip>
</template>

<script setup lang="ts">
  import { NTooltip } from 'naive-ui';
  import { debounce } from 'lodash-es';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { FormConfig } from '@lib/shared/models/system/module';

  import inputNumber from '../basic/inputNumber.vue';

  import { FormCreateField } from '../../types';

  const { t } = useI18n();

  const props = defineProps<{
    fieldConfig: FormCreateField;
    formConfig?: FormConfig;
    path: string;
    formDetail?: Record<string, any>;
    needInitDetail?: boolean; // 判断是否编辑情况
    isSubTableField?: boolean; // 是否是子表字段
    isSubTableRender?: boolean; // 是否是子表渲染
  }>();

  const emit = defineEmits<{
    (e: 'change', value: number | null): void;
  }>();

  const value = defineModel<number | null>('value', {
    default: 0,
  });

  function normalizeExpression(str: string) {
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

  function calcFormula(formula: string, getter: (id: string) => any) {
    if (!formula) return null;

    // 清洗富文本或特定带入的字符串
    let express = normalizeExpression(formula);

    // 替换变量
    express = express.replace(/\$\{(.+?)\}/g, (_, fieldId) => {
      const fieldIdMatch = fieldId.match(/^\(?([A-Za-z0-9_]+)\)?/);
      if (!fieldIdMatch) return '0';
      const realId = fieldIdMatch[1];
      const rawVal = getter(realId);
      // 转换为数字，如果无效则默认为 0
      const num = parseFloat(String(rawVal));
      if (Number.isNaN(num)) return '0';
      // 把表达式里原来 ID 替换成实际数值
      return fieldId.replace(realId, String(num));
    });

    try {
      //  安全性检查确保表达式只包含数字、运算符、小数点、括号
      if (/[^0-9+\-*/().\s%]/.test(express)) {
        // eslint-disable-next-line no-console
        console.warn('The formula contains an invalid character and terminates the computation:', express);
        return null;
      }

      // eslint-disable-next-line no-new-func
      const result = new Function(`return (${express})`)();
      return parseFloat(Number(result).toPrecision(12));
    } catch (err) {
      // eslint-disable-next-line no-console
      console.warn('Formula calculation exception:', formula, err);
      return null;
    }
  }

  function normalizeNumber(val: any): number | null {
    if (val === null || val === undefined || val === '') return null;
    // 已经是 number 直接返回
    if (typeof val === 'number') return val;
    let str = String(val).trim();
    // 是否是百分比格式（可能带千分位）
    if (str.endsWith('%')) {
      str = str.slice(0, -1); // 去掉 %
    }
    // 去除千分位 ","
    str = str.replace(/,/g, '');
    // 转数字
    const num = Number(str);
    if (Number.isNaN(num)) return null;
    return num;
  }

  function getFieldValue(fieldId: string) {
    // 父级字段
    if (!props.isSubTableRender) {
      return props.formDetail?.[fieldId];
    }

    const pathMatch = props.path.match(/^([^[]+)\[(\d+)\]\.(.+)$/);
    if (pathMatch) {
      const [, tableKey, rowIndexStr] = pathMatch;
      const rowIndex = parseInt(rowIndexStr, 10);
      const row = props.formDetail?.[tableKey]?.[rowIndex];
      const rawValue = row?.[fieldId];
      return normalizeNumber(rawValue);
    }
  }

  function safeParseFormula(formulaString: string) {
    const tooltip = t('crmFormDesign.formulaTooltip');
    if (!formulaString) return { type: 'string', formula: '', tooltip };
    try {
      const parsed = JSON.parse(formulaString);
      return {
        type: 'json',
        formula: parsed.formula ?? '',
        tooltip: parsed.tooltip ?? t('crmFormDesign.formulaTooltip'),
      };
    } catch {
      return { type: 'string', formula: formulaString, tooltip };
    }
  }

  const formulaTooltip = computed(() => safeParseFormula(props.fieldConfig.formula ?? '').tooltip);

  // 根据公式实时计算
  const updateValue = debounce(() => {
    const { formula } = props.fieldConfig;
    const { formula: formulaValue } = safeParseFormula(formula ?? '');
    if (!formulaValue) return;
    const result = calcFormula(formulaValue, getFieldValue);
    const next = result !== null ? Number(result.toFixed(2)) : 0;
    // 如果值未变，不需要更新
    if (Object.is(next, value.value)) return;
    value.value = next;
    emit('change', next);
  }, 300);

  watch(
    () => props.fieldConfig.defaultValue,
    (val) => {
      if (!props.needInitDetail) {
        value.value = val || value.value || 0;
      } else {
        updateValue();
      }
    },
    {
      immediate: true,
    }
  );

  watch(
    () => props.formDetail,
    () => {
      updateValue.flush?.();
      updateValue();
    },
    { deep: true }
  );

  function handleChange(val: number | null) {
    emit('change', val);
  }
</script>

<style lang="less" scoped></style>
