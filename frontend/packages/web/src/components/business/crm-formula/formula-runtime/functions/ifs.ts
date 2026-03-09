import { toBoolean } from '../runtime/excel-runtime';
import { EvaluateContext } from '../types';

export default function IFS(ctx: EvaluateContext, ...args: Array<() => any>) {
  for (let i = 0; i < args.length; i += 2) {
    const cond = args[i];
    const value = args[i + 1];

    if (!cond || !value) break;

    if (toBoolean(cond())) {
      return value();
    }
  }

  return null;
}
