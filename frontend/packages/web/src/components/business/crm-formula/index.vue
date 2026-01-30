<template>
  <n-tooltip trigger="hover" placement="top">
    <template #trigger>
      <inputNumber
        v-model:value="value"
        path="fieldValue"
        :field-config="fieldConfig"
        :form-config="props.formConfig"
        :is-sub-table-field="props.isSubTableField"
        :is-sub-table-render="props.isSubTableRender"
        :need-init-detail="needInitDetail"
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

  import basicComponents from '@/components/business/crm-form-create/components/basic/index';

  import { safeParseFormula } from '../crm-formula-editor/utils';
  import { normalizeExpression, normalizeToNumber, resolveFunctions } from './index';
  import type { FormCreateField } from '@cordys/web/src/components/business/crm-form-create/types';

  const { t } = useI18n();
  const { inputNumber } = basicComponents;

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

  function calcFormula(formula: string, getter: (id: string) => any, warn: (msg: string) => void = console.warn) {
    if (!formula) return null;

    // 清洗富文本或特定带入的字符串
    let express = normalizeExpression(formula);

    // 替换变量
    express = express.replace(/\$\{(.+?)\}/g, (_, fieldId) => {
      const fieldIdMatch = fieldId.match(/^\(?([A-Za-z0-9_]+)\)?/);
      if (!fieldIdMatch) return '0';
      const realId = fieldIdMatch[1];
      const rawVal = getter(realId);
      const val = normalizeToNumber(rawVal);
      return String(val);
    });

    express = resolveFunctions(express, { getValue: getter, warn });

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

  const defaultFormulaConfig = {
    source: '',
    display: '',
    fields: [],
  };

  const formulaTooltip = computed(
    () => safeParseFormula(props.fieldConfig.formula ?? '').display || t('crmFormDesign.formulaTooltip')
  );

  // // 根据公式实时计算 todo xinxinwu
  // const updateValue = debounce(() => {
  //   const { formula } = props.fieldConfig;
  //   const { source } = safeParseFormula(formula ?? '');
  //   // if (!formulaValue) return;
  //   // const result = calcFormula(formulaValue, getFieldValue, (msg) => {
  //   //   console.warn(msg);
  //   //   // todo 校验提示
  //   // });
  //   // const next = result !== null ? Number(result.toFixed(2)) : 0;
  //   // // 如果值未变，不需要更新
  //   // if (Object.is(next, value.value)) return;
  //   // value.value = next;
  //   // emit('change', next);
  // }, 300);

  watch(
    () => props.fieldConfig.defaultValue,
    (val) => {
      if (props.needInitDetail) return;
      if (val !== undefined && val !== null) {
        value.value = val;
      } else if (value.value == null) {
        value.value = 0;
      }
    },
    {
      immediate: true,
    }
  );
  //  todo xinxinwu
  // watch(
  //   () => props.formDetail,
  //   () => {
  //     updateValue.flush?.();
  //     updateValue();
  //   },
  //   { deep: true }
  // );

  watch(
    value,
    (val) => {
      if (val == null) value.value = 0;
    },
    { immediate: true }
  );

  function handleChange(val: number | null) {
    emit('change', val);
  }
</script>

<style lang="less" scoped></style>
