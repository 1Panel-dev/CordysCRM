import { FormulaFunctionRule } from '../../types';
import DAYS_RULE from './days.rule';
import SUM_RULE from './sum.rule';

const FUNCTION_RULES: Record<string, FormulaFunctionRule> = {
  SUM_RULE,
  DAYS_RULE,
};

export default FUNCTION_RULES;
