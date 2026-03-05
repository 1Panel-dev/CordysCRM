import CONCATENATE from './concatenate';
import DAYS from './days';
import SUM from './sum';

const FUNCTION_IMPL: Record<string, (...args: any[]) => number | string> = {
  SUM,
  DAYS,
  CONCATENATE,
};

export default FUNCTION_IMPL;
