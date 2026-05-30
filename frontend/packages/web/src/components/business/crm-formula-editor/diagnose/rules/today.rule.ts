import { useI18n } from '@lib/shared/hooks/useI18n';

import { FormulaErrorCode } from '../../config';
import { FormulaDiagnostic, FormulaFunctionRule } from '../../types';

const { t } = useI18n();

export const TODAY_RULE: FormulaFunctionRule = {
  name: 'TODAY',

  diagnose({ fnNode, args }) {
    const diagnostics: FormulaDiagnostic[] = [];

    if (args?.length !== 0) {
      diagnostics.push({
        type: 'error',
        code: FormulaErrorCode.ARG_COUNT_ERROR,
        functionName: fnNode.name,
        message: t('formulaEditor.diagnostics.nowNotAcceptParams', {
          name: fnNode.name,
        }),
        highlight: {
          tokenRange: [fnNode.startTokenIndex, fnNode.endTokenIndex],
        },
      });
    }

    return diagnostics;
  },
};

export default TODAY_RULE;
