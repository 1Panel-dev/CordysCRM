import { useI18n } from '@lib/shared/hooks/useI18n';

import { FormulaErrorCode } from '../../config';
import { FormulaDiagnostic, FormulaFunctionRule } from '../../types';

const { t } = useI18n();

const UNION_RULE: FormulaFunctionRule = {
  name: 'UNION',

  diagnose({ fnNode, args }) {
    const diagnostics: FormulaDiagnostic[] = [];
    const argument = args[0];

    if (args.length !== 1 || argument?.type !== 'field' || !argument.fieldId.includes('.')) {
      diagnostics.push({
        type: 'error',
        code: FormulaErrorCode.INVALID_FUNCTION_CALL,
        functionName: fnNode.name,
        message: t('formulaEditor.diagnostics.unionRequiresSubtableField'),
        highlight: {
          tokenRange: [fnNode.startTokenIndex, fnNode.endTokenIndex],
        },
      });
    }

    return diagnostics;
  },
};

export default UNION_RULE;
