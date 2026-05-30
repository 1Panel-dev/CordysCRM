// /formula-runtime/functions/text.ts
import { formatTextValue } from '../runtime/text-format';
import { EvaluateContext } from '../types';

export default function TEXT(ctx: EvaluateContext, value: any, format: any): string {
  return formatTextValue(value, format);
}
