// index.ts 诊断函数配置总入口
import { FormulaFunctionRule } from '../../types';
import AND from './and.rule';
import CONCATENATE from './concatenate.rule';
import DAYS from './days.rule';
import IF from './if.rule';
import IFS from './ifs.rule';
import NOW from './now.rule';
import SUM from './sum.rule';
import TEXT from './text.rule';
import TODAY from './today.rule';
import UNION from './union.rule';

const FUNCTION_RULES: Record<string, FormulaFunctionRule> = {
  SUM,
  DAYS,
  CONCATENATE,
  AND,
  IF,
  IFS,
  NOW,
  TEXT,
  TODAY,
  UNION,
};

export default FUNCTION_RULES;
