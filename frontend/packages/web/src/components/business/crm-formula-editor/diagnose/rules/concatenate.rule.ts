import { useI18n } from '@lib/shared/hooks/useI18n';

import { FormulaErrorCode } from '../../config';
import { FormulaDiagnostic, FormulaFunctionRule } from '../../types';
import { isTextNumberDateNode } from './rule-utils';

const { t } = useI18n();

export const CONCATENATE_RULE: FormulaFunctionRule = {
  name: 'CONCATENATE',

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

    // 2. 参数类型限制：文本、数字、日期
    args.forEach((arg, index) => {
      if (!isTextNumberDateNode(arg)) {
        diagnostics.push({
          type: 'error',
          code: FormulaErrorCode.INVALID_FUNCTION_CALL,
          functionName: fnNode.name,
          message: t('formulaEditor.diagnostics.invalidArgTypeOfCONCATENATE', {
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

export default CONCATENATE_RULE;
