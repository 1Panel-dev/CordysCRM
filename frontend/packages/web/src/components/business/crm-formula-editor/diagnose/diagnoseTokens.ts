import { useI18n } from '@lib/shared/hooks/useI18n';

import { FormulaErrorCode } from '../config';
import { FormulaDiagnostic, Token } from '../types';

const { t } = useI18n();
/**
 *
 * @param tokens 公式 Token 列表
 * @returns 诊断错误信息列表
 */
export default function diagnoseTokens(tokens: Token[]): FormulaDiagnostic[] {
  const diagnostics: FormulaDiagnostic[] = [];

  for (let i = 0; i < tokens.length; i++) {
    const cur = tokens[i];
    const prev = tokens[i - 1];

    /** 连续操作符 */
    if (cur.type === 'operator' && prev?.type === 'operator') {
      diagnostics.push({
        type: 'error',
        code: FormulaErrorCode.SYNTAX_ERROR,
        message: t('formulaEditor.diagnostics.duplicateOperator'),
        highlight: {
          tokenRange: [i - 1, i],
        },
      });
    }

    /** text token 直接报错（AST 层已忽略） */
    if (cur.type === 'text' && prev?.type !== 'text') {
      diagnostics.push({
        type: 'error',
        code: FormulaErrorCode.INVALID_CHAR,
        message: `${t('formulaEditor.diagnostics.illegalCharacter')} "${cur.value}"`,
        highlight: {
          tokenRange: [i, i],
        },
      });
    }

    if (cur.type === 'comma') {
      if (prev && prev.type === 'comma') {
        diagnostics.push({
          type: 'error',
          code: FormulaErrorCode.DUPLICATE_SEPARATOR,
          message: t('formulaEditor.diagnostics.duplicateSeparatorOfBeginning'),
          highlight: {
            tokenRange: [i - 1, i],
          },
        });
      }
      if (cur.value === '，') {
        diagnostics.push({
          type: 'error',
          code: FormulaErrorCode.INVALID_CHAR,
          message: `${t('formulaEditor.diagnostics.illegalCharacter')} "${cur.value}"`,
          highlight: {
            tokenRange: [i, i],
          },
        });
      }
    }
  }

  return diagnostics;
}
