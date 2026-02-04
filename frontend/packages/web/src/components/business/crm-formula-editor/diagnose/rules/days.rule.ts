import { useI18n } from '@lib/shared/hooks/useI18n';

import { FormulaErrorCode } from '../../config';
import { ASTNode, FormulaDiagnostic, FormulaFunctionRule } from '../../types';

const { t } = useI18n();

function isColumnField(fieldId: string) {
  return fieldId.includes('.');
}

export const DAYS_RULE: FormulaFunctionRule = {
  name: 'DAYS',

  diagnose({ fnNode, args }) {
    const diagnostics: FormulaDiagnostic[] = [];

    /**  参数类型校验（逐个） */
    args.forEach((arg: ASTNode, index: number) => {
      if (arg.type === 'field' && isColumnField(arg.fieldId)) {
        diagnostics.push({
          type: 'error',
          code: FormulaErrorCode.INVALID_FUNCTION_CALL,
          functionName: fnNode.name,
          message: t('formulaEditor.diagnostics.invalidArgOfDAYS', {
            index: index + 1,
          }),
          highlight: {
            tokenRange: [arg.startTokenIndex, arg.endTokenIndex],
          },
        });
      }
    });

    /**  参数个数校验 */
    if (args?.length !== 2) {
      diagnostics.push({
        type: 'error',
        code: FormulaErrorCode.ARG_COUNT_ERROR,
        functionName: fnNode.name,
        message: t('formulaEditor.diagnostics.argCountErrorOfDAYS'),
        highlight: {
          tokenRange: [fnNode.startTokenIndex, fnNode.endTokenIndex],
        },
      });
      return diagnostics;
    }

    return diagnostics;
  },
};
export default DAYS_RULE;
