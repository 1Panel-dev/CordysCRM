import { tokenizeFromSource } from '@/components/business/crm-formula-editor/parseSource/serializeFormulaFromAst';
import { safeParseFormula } from '@/components/business/crm-formula-editor/utils';

import { flatAllFields } from '../../utils';
import type { FormCreateField } from '@cordys/web/src/components/business/crm-form-create/types';

export interface FormulaDisplayInfo {
  allFieldIds: string[];
  display: string;
  isInvalid: boolean;
  tooltip: string;
}

function buildFormulaDisplayFromSource(source: string, fields: FormCreateField[], isSubTableRender?: boolean) {
  const flatFields = flatAllFields(fields, {
    isSubTableRender,
  });

  const fieldMap = Object.fromEntries(flatFields.map((field) => [field.id, field]));
  const tokens = tokenizeFromSource(source, fieldMap);

  return tokens
    .map((token) => {
      switch (token.type) {
        case 'field':
          return token.name;
        case 'function':
          return token.name;
        case 'string':
          return `"${token.value}"`;
        case 'boolean':
          return token.value ? 'TRUE' : 'FALSE';
        case 'number':
          return String(token.value);
        case 'comma':
          return ',';
        case 'paren':
          return token.value;
        case 'operator':
          return ` ${token.value} `;
        case 'unknown':
          return token.value;
        default:
          return '';
      }
    })
    .join('');
}

/**
 * 一次性获取公式展示所需信息
 */
export function getFormulaDisplayInfo(options: {
  formula?: string;
  fields: FormCreateField[];
  invalidText: string;
  emptyText: string;
  isSubTableRender?: boolean;
}): FormulaDisplayInfo {
  const { formula, fields, invalidText, emptyText, isSubTableRender } = options;

  const allFieldIds = flatAllFields(fields, {
    isSubTableRender,
  }).map((e) => e.id);

  if (!formula) {
    return {
      allFieldIds,
      display: '',
      isInvalid: false,
      tooltip: emptyText,
    };
  }

  const parsed = safeParseFormula(formula);
  const display = parsed.source
    ? buildFormulaDisplayFromSource(parsed.source, fields, isSubTableRender) || parsed.display || ''
    : parsed.display || '';
  const savedFields = parsed.fields?.map((e: any) => e.fieldId) ?? [];

  const isInvalid = savedFields.some((fieldId: string) => !allFieldIds.includes(fieldId));

  let tooltip: string;
  if (!display) {
    tooltip = emptyText;
  } else if (isInvalid) {
    tooltip = invalidText;
  } else {
    tooltip = display;
  }

  return {
    allFieldIds,
    display,
    isInvalid,
    tooltip,
  };
}
