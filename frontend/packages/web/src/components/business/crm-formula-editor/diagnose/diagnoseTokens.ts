import { FormulaDiagnostic, Token } from '../types';
import RULES from './diagnose-rules';
import { createDiagnoseContext } from './diagnose-rules/utils';

/**
 *
 * @param tokens 公式 Token 列表
 * @returns 诊断错误信息列表
 */

export default function diagnoseTokens(tokens: Token[]): FormulaDiagnostic[] {
  const ctx = createDiagnoseContext(tokens);

  for (let i = 0; i < tokens.length; i++) {
    ctx.index = i;
    // 逐个 token 检查
    RULES.forEach((rule) => {
      rule.check(ctx);
    });
  }

  // after-scan 整体检查 规则
  RULES.forEach((rule) => {
    rule.afterAll?.(ctx);
  });

  return ctx.diagnostics;
}
