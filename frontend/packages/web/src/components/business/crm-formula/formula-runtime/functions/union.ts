import type { EvaluateContext } from '../types';

export default function UNION(_ctx: EvaluateContext, ...values: unknown[]): string {
  return values
    .flatMap((value) => (Array.isArray(value) ? value : [value]))
    .map((item) =>
      item === null || item === undefined || (typeof item === 'string' && item.trim() === '') ? '' : String(item)
    )
    .join('');
}
