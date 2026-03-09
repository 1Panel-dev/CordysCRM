// /formula-runtime/functions/days.ts
import { EvaluateContext } from '../types';

export default function DAYS(ctx: EvaluateContext, end: number, start: number): number {
  if (!Number.isFinite(end) || !Number.isFinite(start)) return 0;

  return Math.floor(end) - Math.floor(start);
}
