import { useI18n } from '@lib/shared/hooks/useI18n';

import { FormulaErrorCode } from '../../config';
import { FormulaDiagnostic, FormulaFunctionRule } from '../../types';

const { t } = useI18n();

export const IF_RULE: FormulaFunctionRule = {
  name: 'IF',

  diagnose({ fnNode, args }) {
    const diagnostics: FormulaDiagnostic[] = [];

    if (args.length < 2 || args.length > 3) {
      diagnostics.push({
        type: 'error',
        code: FormulaErrorCode.ARG_COUNT_ERROR,
        functionName: fnNode.name,
        message: t('formulaEditor.diagnostics.argCountErrorOfIF'),
        highlight: {
          tokenRange: [fnNode.startTokenIndex, fnNode.endTokenIndex],
        },
      });
    }

    return diagnostics;
  },
};

export default IF_RULE;
