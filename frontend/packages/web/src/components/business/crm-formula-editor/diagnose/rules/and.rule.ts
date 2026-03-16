import { useI18n } from '@lib/shared/hooks/useI18n';

import { FormulaErrorCode } from '../../config';
import { FormulaDiagnostic, FormulaFunctionRule } from '../../types';
import { isLogicalLikeNodeForAnd } from './rule-utils';

const { t } = useI18n();

export const AND_RULE: FormulaFunctionRule = {
  name: 'AND',

  diagnose({ fnNode, args }) {
    const diagnostics: FormulaDiagnostic[] = [];

    // 1. 至少一个参数
    if (args.length < 1) {
      diagnostics.push({
        type: 'error',
        code: FormulaErrorCode.ARG_COUNT_ERROR,
        functionName: fnNode.name,
        message: t('formulaEditor.diagnostics.argCountError'),
        highlight: {
          tokenRange: [fnNode.startTokenIndex, fnNode.endTokenIndex],
        },
      });
      return diagnostics;
    }

    // 允许逻辑值，可转逻辑的 number/date 节点
    args.forEach((arg, index) => {
      if (!isLogicalLikeNodeForAnd(arg)) {
        diagnostics.push({
          type: 'error',
          code: FormulaErrorCode.INVALID_FUNCTION_CALL,
          functionName: fnNode.name,
          message: t('formulaEditor.diagnostics.invalidConditionTypeOfAND', {
            index: index + 1,
          }),
          highlight: {
            tokenRange: [arg.startTokenIndex, arg.endTokenIndex],
          },
        });
      }
    });

    return diagnostics;
  },
};

export default AND_RULE;
