// index.ts 诊断函数配置总入口
import { FormulaFunctionRule } from '../../types';
import CONCATENATE from './concatenate.rule';
import DAYS from './days.rule';
import SUM from './sum.rule';

const FUNCTION_RULES: Record<string, FormulaFunctionRule> = {
  SUM,
  DAYS,
  CONCATENATE,
};

export default FUNCTION_RULES;
