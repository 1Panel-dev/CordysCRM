import DAYS from './days';
import SUM from './sum';

const FUNCTION_IMPL: Record<string, (...args: any[]) => number> = {
  SUM,
  DAYS,
};

export default FUNCTION_IMPL;
