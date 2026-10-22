import { EvaluateContext } from '../types';

export default function UNION(_ctx: EvaluateContext, value: unknown): string {
  if (!Array.isArray(value)) {
    return '';
  }

  return value
    .filter((item) => item !== null && item !== undefined && item !== '')
    .map((item) => String(item))
    .join('');
}
