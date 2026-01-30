import { FormulaErrorCode } from '../../config';
import { FormulaDiagnostic, FormulaFunctionRule } from '../../types';
// todo 国际化没加
const DAYS_RULE: FormulaFunctionRule = {
  name: 'DAYS',

  diagnose({ fnNode, args }) {
    const diagnostics: FormulaDiagnostic[] = [];

    if (args.length !== 2) {
      diagnostics.push({
        type: 'error',
        code: FormulaErrorCode.ARG_COUNT_ERROR,
        functionName: fnNode.name,
        message: '参数个数错误，必须为两个参数',
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
