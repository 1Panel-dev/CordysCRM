import { useI18n } from '@lib/shared/hooks/useI18n';

import { FormulaErrorCode } from '../config';
import { ASTNode, FormulaDiagnostic, FunctionNode, Token } from '../types';

const { t } = useI18n();
/**
 *
 * @param fnNode 函数的ast节点
 * @param args 函数的ast参数列表
 * @param tokens 公式的token列表
 * @returns 诊断错误信息列表
 */
export default function diagnoseArgs(fnNode: FunctionNode, args: ASTNode[], tokens: Token[]): FormulaDiagnostic[] {
  const diagnostics: FormulaDiagnostic[] = [];

  const { startTokenIndex, endTokenIndex } = fnNode;

  // 取函数括号内的 token（不含函数名）
  const innerTokens = tokens.slice(startTokenIndex + 1, endTokenIndex);

  // 所有逗号 token
  const commaTokens = innerTokens.filter((item) => item.type === 'comma');

  if (args?.length === 0) {
    diagnostics.push({
      type: 'warning',
      functionName: fnNode.name,
      code: FormulaErrorCode.EMPTY_ARGS,
      message: t('formulaEditor.diagnostics.emptyParams'),
      highlight: {
        tokenRange: [fnNode.startTokenIndex, fnNode.endTokenIndex],
      },
    });
    return diagnostics;
  }

  /**
   * 1. 空参数 / 多余分隔符
   */
  if (commaTokens.length > 0) {
    // 开头 / 结尾是逗号
    if (innerTokens[0]?.type === 'comma' || innerTokens[innerTokens.length - 1]?.type === 'comma') {
      diagnostics.push({
        type: 'error',
        code: FormulaErrorCode.DUPLICATE_SEPARATOR,
        functionName: fnNode.name,
        message: t('formulaEditor.diagnostics.duplicateSeparator'),
        highlight: {
          tokenRange: [innerTokens[innerTokens.length - 1].start, innerTokens[innerTokens.length - 1].start],
        },
      });
      return diagnostics;
    }

    // 连续逗号
    for (let i = 0; i < innerTokens.length - 1; i++) {
      if (innerTokens[i].type === 'comma' && innerTokens[i + 1].type === 'comma') {
        diagnostics.push({
          type: 'error',
          code: FormulaErrorCode.DUPLICATE_SEPARATOR,
          functionName: fnNode.name,
          message: t('formulaEditor.diagnostics.duplicateSeparatorOfCenter'),
          highlight: {
            tokenRange: [innerTokens[i].start, innerTokens[i + 1].start + 1],
          },
        });
        return diagnostics;
      }
    }
  }

  if (args?.length >= 2 && commaTokens.length === 0) {
    diagnostics.push({
      type: 'error',
      code: FormulaErrorCode.MISSING_SEPARATOR,
      functionName: fnNode.name,
      message: t('formulaEditor.diagnostics.missingSeparator'),
      highlight: {
        tokenRange: [args[0].endTokenIndex, args[1].startTokenIndex],
      },
    });
  }

  return diagnostics;
}
