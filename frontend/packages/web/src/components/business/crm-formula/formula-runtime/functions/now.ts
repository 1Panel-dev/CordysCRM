import { localDateToExcelSerial } from '../runtime/excel-date';
import { EvaluateContext } from '../types';

export default function NOW(ctx: EvaluateContext) {
  if (!ctx.evaluationNow) return;
  return localDateToExcelSerial(ctx.evaluationNow);
}
