import { EvaluateContext } from '../types';

const DAY_MS = 24 * 60 * 60 * 1000;
const EXCEL_EPOCH = new Date(1899, 11, 30).getTime();

export default function NOW(ctx: EvaluateContext) {
  const now = new Date();

  return (now.getTime() - EXCEL_EPOCH) / DAY_MS;
}
