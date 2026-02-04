import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';

import type { FormCreateField } from '@cordys/web/src/components/business/crm-form-create/types';

const { t } = useI18n();

export const INPUT_NUMBER_COLOR = 'var(--info-blue)'; // 数字主题颜色
export const DATE_TIME_COLOR = 'var(--success-green)'; // 日期主题颜色
export const ARRAY_COLOR = 'var(--warning-yellow)'; // 数组主题颜色
export const FUN_COLOR = '#9170fd'; // 函数主题颜色

export const defaultFormulaConfig = {
  source: '',
  display: '',
  fields: [],
};

// 函数列表
export const allFunctionSource: (FormCreateField & { isFunction: boolean })[] = [
  {
    icon: 'SUM',
    name: 'SUM',
    id: 'SUM',
    isFunction: true,
    type: FieldTypeEnum.INPUT_NUMBER,
    showLabel: true,
    readable: false,
    editable: false,
    fieldWidth: 0,
    rules: [],
    description: t('crmFormDesign.formulaSUMDescription'),
  },
  {
    icon: 'DAYS',
    name: 'DAYS',
    id: 'DAYS',
    isFunction: true,
    type: FieldTypeEnum.DATE_TIME,
    showLabel: true,
    readable: false,
    editable: false,
    fieldWidth: 0,
    rules: [],
    description: t('crmFormDesign.formulaDAYSDescription'),
  },
];

export const FormulaErrorCode = {
  EMPTY_ARGS: 'EMPTY_ARGS', // 参数个数为空
  ARG_COUNT_ERROR: 'ARG_COUNT_ERROR', // 参数个数错误
  ARG_TYPE_ERROR: 'ARG_TYPE_ERROR', // 参数类型错误
  MISSING_SEPARATOR: 'MISSING_SEPARATOR', // 缺少分隔符
  SYNTAX_ERROR: 'SYNTAX_ERROR', // 语法错误
  SEPARATOR_ERROR_ARG: 'SEPARATOR_ERROR_ARG', // 分隔符缺少参数
  DUPLICATE_SEPARATOR: 'DUPLICATE_SEPARATOR', // 重复分隔符
  INVALID_CHAR: 'INVALID_CHAR', // 非法字符
  INCOMPLETE_EXPRESSION: 'INCOMPLETE_EXPRESSION', // 表达式不完整
  INVALID_FUNCTION_CALL: 'INVALID_FUNCTION_CALL', // 非法的函数调用
} as const;
