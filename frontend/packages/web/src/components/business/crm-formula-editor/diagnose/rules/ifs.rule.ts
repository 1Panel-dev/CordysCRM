import { useI18n } from '@lib/shared/hooks/useI18n';

import { FormulaErrorCode } from '../../config';
import { FormulaDiagnostic, FormulaFunctionRule } from '../../types';
import { isLogicalNode, isTextNumberDateNode } from './rule-utils';

const { t } = useI18n();

export const IFS_RULE: FormulaFunctionRule = {
  name: 'IFS',

  diagnose({ fnNode, args }) {
    const diagnostics: FormulaDiagnostic[] = [];

    // 1. 参数个数必须为偶数，且至少 2 个
    if (args.length < 2 || args.length % 2 !== 0) {
      diagnostics.push({
        type: 'error',
        code: FormulaErrorCode.ARG_COUNT_ERROR,
        functionName: fnNode.name,
        message: t('formulaEditor.diagnostics.argCountErrorOfIFS'),
        highlight: {
          tokenRange: [fnNode.startTokenIndex, fnNode.endTokenIndex],
        },
      });
      return diagnostics;
    }

    // 2. 条件 - 结果 成对校验
    args.forEach((arg, index) => {
      const isCondition = index % 2 === 0;

      if (isCondition) {
        if (!isLogicalNode(arg)) {
          diagnostics.push({
            type: 'error',
            code: FormulaErrorCode.INVALID_FUNCTION_CALL,
            functionName: fnNode.name,
            message: t('formulaEditor.diagnostics.invalidConditionTypeOfIFS', {
              index: index + 1,
            }),
            highlight: {
              tokenRange: [arg.startTokenIndex, arg.endTokenIndex],
            },
          });
        }
      } else if (!isTextNumberDateNode(arg)) {
        diagnostics.push({
          type: 'error',
          code: FormulaErrorCode.INVALID_FUNCTION_CALL,
          functionName: fnNode.name,
          message: t('formulaEditor.diagnostics.invalidResultTypeOfIFS', {
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

export default IFS_RULE;
