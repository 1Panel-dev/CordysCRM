import { FormulaErrorCode } from '../config';
import { FormulaDiagnostic, Token } from '../types';
// todo 国际化没加
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
        message: '连续的操作符',
        highlight: {
          tokenRange: [i - 1, i],
        },
      });
    }

    /** text token 直接报错（AST 层已忽略） */
    if (cur.type === 'text') {
      diagnostics.push({
        type: 'error',
        code: FormulaErrorCode.INVALID_CHAR,
        message: `存在非法字符 "${cur.value}"`,
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
          message: '请勿连续使用分隔符',
          highlight: {
            tokenRange: [i - 1, i],
          },
        });
      }
      if (cur.value === '，') {
        diagnostics.push({
          type: 'error',
          code: FormulaErrorCode.INVALID_CHAR,
          message: `存在非法字符 "${cur.value}"`,
          highlight: {
            tokenRange: [i, i],
          },
        });
      }
    }
  }

  return diagnostics;
}
