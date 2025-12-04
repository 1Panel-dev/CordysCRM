<template>
  <inputNumber
    v-model:value="value"
    path="fieldValue"
    :field-config="fieldConfig"
    :is-sub-table-field="props.isSubTableField"
    :is-sub-table-render="props.isSubTableRender"
    @change="handleChange"
  />
</template>

<script setup lang="ts">
  import { debounce } from 'lodash-es';

  import inputNumber from '../basic/inputNumber.vue';

  import { FormCreateField } from '../../types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
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

  /**
   * 传入整个当前表达式和当前 token（例如 "${(123 / 100)}"：百分比）
   * 根据 token 在 expression 中位置左右的运算符判断默认值：
   *  - 如果前面是 * / % 返回 '1'
   *  - 加减返回 '0'
   */
  function determineDefaultValue(expression: string, token: string): string {
    const idx = expression.indexOf(token);
    // 找前一个非空白字符
    let i = idx - 1;
    while (i >= 0 && /\s/.test(expression[i])) i--;
    const prevChar = expression[i] ?? '+';

    if (['*', '/', '%'].includes(prevChar)) {
      return '1';
    }
    return '0';
  }

  function calcFormula(formula: string, getter: (id: string) => any): number | null {
    if (!formula) return null;

    // 清洗富文本或特定带入的字符串
    let express = formula.replace(/[\u200B-\u200D\uFEFF]/g, '');

    // 替换变量:match 是整块 ${...}， innerContent 是花括号内的内容
    express = express.replace(/\$\{(.+?)\}/g, (match, innerContent) => {
      // 匹配innerContent第一个数字 id（兼容带括号或后缀的情况）
      const fieldIdMatch = innerContent.match(/^\s*\(?\s*(\d+)\s*\)?/);
      if (!fieldIdMatch) return '0';
      const realId = fieldIdMatch[1];
      const rawVal = getter(realId);
      const parsed = parseFloat(String(rawVal));
      // 判断 innerContent 是否包含 "/100"百分号
      const hasPercent = /\/\s*100\b/.test(innerContent);
      const hasPercentLiteral = /%/.test(innerContent);

      // 如果值未填写:填充 '0' 或 '1'
      if (Number.isNaN(parsed)) {
        return determineDefaultValue(express, match);
      }

      // 填写值有效:如果原始占位是百分比格式，则返回 (parsed/100)，否则返回数字字符串
      if (hasPercent || hasPercentLiteral) {
        return `(${String(parsed)} / 100)`;
      }

      return String(parsed);
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

  function getFieldValue(fieldId: string) {
    // 父级字段
    if (!props.isSubTableRender) {
      return props.formDetail?.[fieldId];
    }

    const pathMatch = props.path.match(/^([^[]+)\[(\d+)\]\.(.+)$/);
    if (pathMatch) {
      const [, tableKey, rowIndexStr, currentFieldId] = pathMatch;
      const rowIndex = parseInt(rowIndexStr, 10);

      if (fieldId === currentFieldId) {
        const row = props.formDetail?.[tableKey]?.[rowIndex];
        return row?.[fieldId];
      }

      const row = props.formDetail?.[tableKey]?.[rowIndex];
      return row?.[fieldId];
    }

    const paths = props.path.split('.');
    const tableKey = paths[0];
    const rowIndex = Number(paths[1]);

    const row = props.formDetail?.[tableKey]?.[rowIndex];
    return row?.[fieldId];
  }

  // 根据公式实时计算
  const updateValue = debounce(() => {
    const { formula } = props.fieldConfig;
    if (!formula) return;
    const result = calcFormula(formula, getFieldValue);
    value.value = result !== null ? Number(result.toFixed(2)) : 0;
    emit('change', value.value);
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
      updateValue();
    },
    { deep: true }
  );

  function handleChange(val: number | null) {
    emit('change', val);
  }
</script>

<style lang="less" scoped></style>
