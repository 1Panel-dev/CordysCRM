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

  function calcFormula(formula: string, getter: (id: string) => any) {
    if (!formula) return null;

    // 清洗富文本或特定带入的字符串
    let express = formula.replace(/[\u200B-\u200D\uFEFF]/g, '');

    // 替换变量
    express = express.replace(/\$\{(.+?)\}/g, (_, fieldId) => {
      const fieldIdMatch = fieldId.match(/^\(?(\d+)\)?/);
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
