import { useI18n } from '@lib/shared/hooks/useI18n';

import { FormulaErrorCode } from '../../config';
import { FormulaDiagnostic, FormulaFunctionRule } from '../../types';

const { t } = useI18n();

const DAYS_RULE: FormulaFunctionRule = {
  name: 'DAYS',

  diagnose({ fnNode, args }) {
    const diagnostics: FormulaDiagnostic[] = [];

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
