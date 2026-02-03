import { FormulaFunctionRule } from '../../types';
import DAYS from './days.rule';
import SUM from './sum.rule';

const FUNCTION_RULES: Record<string, FormulaFunctionRule> = {
  SUM,
  DAYS,
};

export default FUNCTION_RULES;
