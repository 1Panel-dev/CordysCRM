import { FormCreateField } from '@/components/business/crm-form-create/types';
import { flatAllFields } from '@/components/business/crm-formula/utils';

import parseTokensToAST from '../parser';
import { FormulaSerializeResult } from '../types';
import { safeParseFormula } from '../utils';
import { serializeFormulaFromAst, tokenizeFromSource } from './serializeFormulaFromAst';

export function remapFormulaSourceFieldIds(source: string, fieldIdMap: Record<string, string>) {
  return source.replace(/\$\{([^}]+)\}/g, (match, rawFieldId: string) => {
    const fieldId = rawFieldId.trim();
    return fieldIdMap[fieldId] ? `\${${fieldIdMap[fieldId]}}` : match;
  });
}

export function remapFormulaFieldIds(
  formulaString: string | undefined,
  fieldIdMap: Record<string, string>,
  fields: FormCreateField[],
  options?: {
    isSubTableRender?: boolean;
  }
) {
  if (!formulaString) {
    return formulaString;
  }

  const parsed = safeParseFormula(formulaString) as FormulaSerializeResult;
  if (!parsed?.source) {
    return formulaString;
  }

  const nextSource = remapFormulaSourceFieldIds(parsed.source, fieldIdMap);
  if (nextSource === parsed.source) {
    return formulaString;
  }

  const flatFields = flatAllFields(fields, {
    isSubTableRender: options?.isSubTableRender,
  });

  const fieldMap = Object.fromEntries(flatFields.map((field) => [field.id, field]));
  const fieldNameMap = Object.fromEntries(flatFields.map((field) => [field.id, field.name]));

  const tokens = tokenizeFromSource(nextSource, fieldMap);
  const ast = parseTokensToAST(tokens);
  const nextFormula = serializeFormulaFromAst(ast, fieldNameMap);

  return JSON.stringify(nextFormula);
}
