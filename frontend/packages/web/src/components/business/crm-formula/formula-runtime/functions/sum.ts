// /formula-runtime/functions/sum.ts
import { normalizeFormulaNumber } from '@/components/business/crm-formula/utils';

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

  return normalizeFormulaNumber(total);
}
