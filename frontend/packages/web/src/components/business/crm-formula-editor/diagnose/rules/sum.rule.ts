import { FormulaDiagnostic, FormulaFunctionRule } from '../../types';

const SUM_RULE: FormulaFunctionRule = {
  name: 'SUM',

  diagnose({ fnNode, args }) {
    const diagnostics: FormulaDiagnostic[] = [];
    // 后边SUM的其他规则可扩展在这里
    return diagnostics;
  },
};

export default SUM_RULE;
