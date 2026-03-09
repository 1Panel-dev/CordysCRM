// /formula-runtime/functions/sum.ts
import { EvaluateContext } from '../types';

export default function SUM(ctx: EvaluateContext, ...args: any[]): number {
  let total = 0;

  args.forEach((v) => {
    if (Array.isArray(v)) {
      total += SUM(ctx, ...v);
    } else if (typeof v === 'number') {
      total += v;
    }
  });

  return total;
}
