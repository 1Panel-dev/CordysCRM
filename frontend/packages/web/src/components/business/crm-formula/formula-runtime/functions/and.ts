import { toBoolean } from '../runtime/excel-runtime';
import { EvaluateContext } from '../types';

export default function AND(ctx: EvaluateContext, ...args: Array<() => any>) {
  return args.every((fn) => toBoolean(fn()));
}
