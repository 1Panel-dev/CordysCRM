// formula-runtime/functions/today.ts
import { EvaluateContext } from '../types';

const DAY_MS = 24 * 60 * 60 * 1000;
const EXCEL_EPOCH = new Date(1899, 11, 30).getTime();

export default function TODAY(ctx: EvaluateContext) {
  const now = new Date();

  // 取本地日期，不带时间
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0, 0);

  const serial = (today.getTime() - EXCEL_EPOCH) / DAY_MS;

  return Math.floor(serial);
}
