import { toBoolean } from '../runtime/excel-runtime';
import { EvaluateContext } from '../types';

export default function IF(ctx: EvaluateContext, cond: () => any, trueValue: () => any, falseValue?: () => any) {
  const condition = toBoolean(cond());

  if (condition) {
    return trueValue();
  }

  if (falseValue) {
    return falseValue();
  }

  return null;
}
