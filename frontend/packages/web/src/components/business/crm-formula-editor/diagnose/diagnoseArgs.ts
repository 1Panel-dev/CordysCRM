import { useI18n } from '@lib/shared/hooks/useI18n';

import { AllowEmptyArgsFunctionList, FormulaErrorCode } from '../config';
import { ASTNode, FormulaDiagnostic, FunctionNode, Token } from '../types';

const { t } = useI18n();

/**
 * 获取当前函数参数区内的“顶层逗号”
 *
 * innerTokens 是从 startTokenIndex + 1 开始切的，
 * 里面包含了当前函数自己的左括号 "("，
 * 所以当前函数“顶层参数”的深度应为 1，而不是 0
 */
function getTopLevelCommaTokens(tokens: Token[]): Token[] {
  const result: Token[] = [];
  let depth = 0;

  tokens.forEach((token) => {
    if (token.type === 'paren') {
      if (token.value === '(') {
        depth++;
      } else if (token.value === ')') {
        depth--;
      }
    } else if (token.type === 'comma' && depth === 1) {
      result.push(token);
    }
  });

  return result;
}

/**
 * 判断当前函数参数区内是否存在“顶层连续逗号”
 * 例如：SUM(a,,b)
 *
 * 同样地，当前函数顶层参数所在深度应为 1
 */
function findAdjacentTopLevelCommaRange(tokens: Token[]): [number, number] | null {
  let depth = 0;
  let lastTopLevelCommaToken: Token | null = null;

  for (let i = 0; i < tokens.length; i++) {
    const token = tokens[i];

    if (token.type === 'paren') {
      if (token.value === '(') {
        depth++;
      } else if (token.value === ')') {
        depth--;
      }

      // 一旦遇到括号，说明当前逗号连续性中断
      if (depth === 1) {
        lastTopLevelCommaToken = null;
      }
    } else if (token.type === 'comma' && depth === 1) {
      if (lastTopLevelCommaToken) {
        return [lastTopLevelCommaToken.start, token.end];
      }

      lastTopLevelCommaToken = token;
    } else if (depth === 1) {
      // 当前函数顶层遇到非逗号 token，逗号连续性中断
      lastTopLevelCommaToken = null;
    }
  }

  return null;
}

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

  // 取函数括号内的 token 区域（当前函数的参数列表）
  const innerTokens = tokens.slice(startTokenIndex + 1, endTokenIndex);

  // 只取当前函数顶层逗号
  const commaTokens = getTopLevelCommaTokens(innerTokens);

  if (args?.length === 0 && !AllowEmptyArgsFunctionList.includes(fnNode.name)) {
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
    const firstMeaningfulToken = innerTokens[1];
    const lastMeaningfulToken = innerTokens[innerTokens.length - 1];

    if (firstMeaningfulToken?.type === 'comma' || lastMeaningfulToken?.type === 'comma') {
      diagnostics.push({
        type: 'error',
        code: FormulaErrorCode.DUPLICATE_SEPARATOR,
        functionName: fnNode.name,
        message: t('formulaEditor.diagnostics.duplicateSeparator'),
        highlight: {
          tokenRange: [fnNode.startTokenIndex, fnNode.endTokenIndex],
        },
      });
      return diagnostics;
    }

    // 顶层连续逗号
    const adjacentCommaRange = findAdjacentTopLevelCommaRange(innerTokens);
    if (adjacentCommaRange) {
      diagnostics.push({
        type: 'error',
        code: FormulaErrorCode.DUPLICATE_SEPARATOR,
        functionName: fnNode.name,
        message: t('formulaEditor.diagnostics.duplicateSeparatorOfCenter'),
        highlight: {
          tokenRange: adjacentCommaRange,
        },
      });
      return diagnostics;
    }
  }

  /**
   * 缺少分隔符
   *
   * 正常情况下：
   * args.length = N
   * 顶层 comma 数 = N - 1
   */
  if (args?.length >= 2 && commaTokens.length !== args.length - 1) {
    diagnostics.push({
      type: 'error',
      code: FormulaErrorCode.MISSING_SEPARATOR,
      functionName: fnNode.name,
      message: t('formulaEditor.diagnostics.missingSeparator'),
      highlight: {
        tokenRange: [fnNode.startTokenIndex, fnNode.endTokenIndex],
      },
    });
  }

  return diagnostics;
}
