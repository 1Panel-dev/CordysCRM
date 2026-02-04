import { DiagnoseContext, FormulaDiagnostic, Token } from '../../types';

export function createDiagnoseContext(tokens: Token[]): DiagnoseContext {
  return {
    tokens,
    index: 0,
    diagnostics: [],
    coveredRanges: [],
    parenBalance: 0,
    hasUnexpectedRightParen: false,

    get cur() {
      return tokens[this.index];
    },
    get prev() {
      return tokens[this.index - 1];
    },

    push(diag: FormulaDiagnostic) {
      // 修复：添加类型检查，确保 tokenRange 存在且长度正确
      const tokenRange = diag?.highlight?.tokenRange;
      if (!tokenRange || tokenRange.length !== 2) return;

      const [start, end] = tokenRange;

      // 如果当前错误区间，已经被之前的错误覆盖 → 忽略
      const isCovered = this.coveredRanges.some(([s, e]) => start >= s && end <= e);

      if (isCovered) return;

      // 记录新区间
      this.coveredRanges.push([start, end]);
      this.diagnostics.push(diag);
    },
  };
}

export function isValueEnd(token?: Token): boolean {
  if (!token) return false;

  return token.type === 'number' || token.type === 'field' || (token.type === 'paren' && token.value === ')');
}

export function isValueStart(token?: Token): boolean {
  if (!token) return false;

  return (
    token.type === 'number' ||
    token.type === 'field' ||
    token.type === 'function' || // SUM / DAYS
    (token.type === 'paren' && token.value === '(')
  );
}

export function isIllegalFunctionCall(prev?: Token, cur?: Token): boolean {
  return prev?.type === 'paren' && prev.value === ')' && cur?.type === 'paren' && cur.value === '(';
}
