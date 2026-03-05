import { FormulaDiagnostic, FormulaFunctionRule } from '../../types';

const CONCATENATE_RULE: FormulaFunctionRule = {
  name: 'CONCATENATE',

  diagnose({ fnNode, args }) {
    const diagnostics: FormulaDiagnostic[] = [];
    // 后边CONCATENATE的其他规则可扩展在这里
    return diagnostics;
  },
};

export default CONCATENATE_RULE;
