import { useI18n } from '@lib/shared/hooks/useI18n';

import { FormulaErrorCode } from '../../config';
import { DiagnoseRule } from '../../types';
import { isIllegalFunctionCall, isInsideFunctionArgs, isValueEnd, isValueStart } from './utils';

const { t } = useI18n();

//  连续操作符规则
const duplicateOperatorRule: DiagnoseRule = {
  name: 'duplicate-operator',

  check(ctx) {
    const { cur, prev, index, tokens } = ctx;

    if (cur?.type !== 'operator' || prev?.type !== 'operator') return;

    // 只在连续 operator 的起点报
    if (index > 1 && tokens[index - 2]?.type === 'operator') return;

    let end = index;
    while (end + 1 < tokens.length && tokens[end + 1]?.type === 'operator') {
      end++;
    }

    ctx.push({
      type: 'error',
      code: FormulaErrorCode.SYNTAX_ERROR,
      message: t('formulaEditor.diagnostics.duplicateOperator'),
      highlight: {
        tokenRange: [index - 1, end],
      },
    });
  },
};

// 非法字符
const illegalTextRule: DiagnoseRule = {
  name: 'illegal-text',

  check(ctx) {
    const { cur, prev, index } = ctx;
    if (cur?.type === 'text' && prev?.type !== 'text') {
      ctx.push({
        type: 'error',
        code: FormulaErrorCode.INVALID_CHAR,
        message: `${t('formulaEditor.diagnostics.illegalCharacter')} "${cur.value}"`,
        highlight: { tokenRange: [index, index] },
      });
    }
  },
};

// 非法逗号字符
const illegalCommaCharRule: DiagnoseRule = {
  name: 'illegal-comma-char',

  check(ctx) {
    const { cur, index } = ctx;
    if (cur?.type !== 'comma') return;
    if (cur.value !== '，') return;

    ctx.push({
      type: 'error',
      code: FormulaErrorCode.INVALID_CHAR,
      message: `${t('formulaEditor.diagnostics.illegalCharacter')} "${cur.value}"`,
      highlight: { tokenRange: [index, index] },
    });
  },
};

// 连续符号
const duplicateCommaRule: DiagnoseRule = {
  name: 'duplicate-comma',

  check(ctx) {
    const { cur, prev, index, tokens } = ctx;
    if (cur?.type !== 'comma') return;
    if (prev?.type === 'comma') return; // 只从第一个开始报

    let end = index;
    while (tokens[end + 1]?.type === 'comma') {
      end++;
    }

    if (end === index) return;

    ctx.push({
      type: 'error',
      code: FormulaErrorCode.DUPLICATE_SEPARATOR,
      message: t('formulaEditor.diagnostics.duplicateSeparatorOfBeginning'),
      highlight: { tokenRange: [index, end] },
    });
  },
};

// 开头结尾逗号
const commaPositionRule: DiagnoseRule = {
  name: 'comma-position',

  check(ctx) {
    const { cur, prev, index, tokens } = ctx;
    if (cur?.type !== 'comma') return;

    // 表达式开头
    if (!prev) {
      ctx.push({
        type: 'error',
        code: FormulaErrorCode.SYNTAX_ERROR,
        message: t('formulaEditor.diagnostics.unexpectedTokenAtBeginning'),
        highlight: { tokenRange: [index, index] },
      });
      return;
    }

    // 表达式结尾
    if (index === tokens.length - 1) {
      ctx.push({
        type: 'error',
        code: FormulaErrorCode.SYNTAX_ERROR,
        message: t('formulaEditor.diagnostics.unexpectedTokenAtEnd'),
        highlight: { tokenRange: [index, index] },
      });
    }
  },
};

export const illegalCommaRule: DiagnoseRule = {
  name: 'illegal-comma',

  check(ctx) {
    const { cur, index, tokens } = ctx;
    if (!cur) return;
    const inFunctionArgs = isInsideFunctionArgs(tokens, index);
    if (cur.type === 'comma' && !inFunctionArgs) {
      ctx.push({
        type: 'error',
        code: FormulaErrorCode.SYNTAX_ERROR,
        message: t('formulaEditor.diagnostics.missingOperatorUsedAsSeparator'),
        highlight: {
          tokenRange: [index, index],
        },
      });
    }
  },
};

// 相邻规则
const adjacentValueRule: DiagnoseRule = {
  name: 'adjacent-value',

  check(ctx) {
    const { prev, cur, index, tokens } = ctx;
    if (!prev || !cur) return;
    //  illegal-function-call 处理
    if (isIllegalFunctionCall(prev, cur)) return;
    if (isValueEnd(prev) && isValueStart(cur)) {
      const inFunctionArgs = isInsideFunctionArgs(tokens, index);
      ctx.push({
        type: 'error',
        code: FormulaErrorCode.SYNTAX_ERROR,
        message: inFunctionArgs
          ? t('formulaEditor.diagnostics.missingSeparator')
          : t('formulaEditor.diagnostics.missingOperator'),
        highlight: {
          tokenRange: [index - 1, index],
        },
      });
    }
  },
};

// 括号平衡规则
const parenBalanceRule: DiagnoseRule = {
  name: 'paren-balance',

  check(ctx) {
    const { cur, index } = ctx;
    if (cur?.type !== 'paren') return;

    if (cur.value === '(') {
      ctx.parenBalance++;
    }

    if (cur.value === ')') {
      ctx.parenBalance--;

      if (ctx.parenBalance < 0 && !ctx.hasUnexpectedRightParen) {
        ctx.push({
          type: 'error',
          code: FormulaErrorCode.SYNTAX_ERROR,
          message: t('formulaEditor.diagnostics.unexpectedRightParen'),
          highlight: { tokenRange: [index, index] },
        });
        ctx.hasUnexpectedRightParen = true;
        ctx.parenBalance = 0;
      }
    }
  },

  afterAll(ctx) {
    if (ctx.parenBalance > 0) {
      ctx.push({
        type: 'error',
        code: FormulaErrorCode.SYNTAX_ERROR,
        message: t('formulaEditor.diagnostics.missingRightParen'),
        highlight: {
          tokenRange: [ctx.tokens.length - 1, ctx.tokens.length - 1],
        },
      });
    }
  },
};

// 非法函数调用
const invalidFunctionCallRule: DiagnoseRule = {
  name: 'invalid-function-call',

  check(ctx) {
    const { cur, prev, index } = ctx;
    if (cur?.type === 'paren' && cur.value === '(' && prev?.type === 'paren' && prev.value === ')') {
      ctx.push({
        type: 'error',
        code: FormulaErrorCode.INVALID_FUNCTION_CALL,
        message: t('formulaEditor.diagnostics.invalidFunctionCall'),
        highlight: { tokenRange: [index, index] },
      });
    }
  },
};

const leadingOperatorRule: DiagnoseRule = {
  name: 'leading-operator',

  check(ctx) {
    const { index, cur } = ctx;

    // 只检查第一个 token
    if (index !== 0) return;

    // + - * /
    if (cur?.type === 'operator') {
      ctx.push({
        type: 'error',
        code: FormulaErrorCode.SYNTAX_ERROR,
        message: t('formulaEditor.diagnostics.unexpectedOperatorAtBeginning'),
        highlight: {
          tokenRange: [0, 0],
        },
      });
    }
  },
};
// 通用规则
const RULES: DiagnoseRule[] = [
  leadingOperatorRule, // 放在最前面
  duplicateOperatorRule, // 连续操作符
  illegalTextRule, //  非法字符
  illegalCommaRule, // 非法逗号
  illegalCommaCharRule, // 非法字符
  duplicateCommaRule, // 连续逗号
  commaPositionRule, // 逗号位置
  adjacentValueRule, // 相邻值
  parenBalanceRule, // 括号平衡
  invalidFunctionCallRule, // 非法函数调用
];

export default RULES;
