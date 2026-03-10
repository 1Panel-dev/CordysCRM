// formula-runtime/functions/today.ts
import { localDateToExcelSerial } from '../runtime/excel-date';
import { EvaluateContext } from '../types';

export default function TODAY(ctx: EvaluateContext) {
  if (!ctx.evaluationNow) return;

  const now = ctx.evaluationNow;

  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0, 0);

  return Math.floor(localDateToExcelSerial(today));
}
