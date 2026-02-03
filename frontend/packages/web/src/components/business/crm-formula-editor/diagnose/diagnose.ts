import { ASTNode, FormulaDiagnostic, Token } from '../types';
import diagnoseArgs from './diagnoseArgs';
import diagnoseIncompleteExpression from './diagnoseIncompleteExpression';
import diagnoseTokens from './diagnoseTokens';
import FUNCTION_RULES from './rules';

function walkAST(node: ASTNode, visitor: (node: ASTNode) => void) {
  visitor(node);

  if (node.type === 'binary') {
    walkAST(node.left, visitor);
    walkAST(node.right, visitor);
    return;
  }

  if (node.type === 'function') {
    node.args.forEach((arg) => {
      walkAST(arg, visitor);
    });
  }
}

/**
 * 总诊断入口
 * @param tokens 公式token列表
 * @param ast ast节点
 * @returns  诊断列表
 */
export default function diagnoseFormula(tokens: Token[], ast: ASTNode[]): FormulaDiagnostic[] {
  const diagnostics: FormulaDiagnostic[] = [];

  /**  token 层诊断 */
  const tokenErrors = diagnoseTokens(tokens);
  diagnostics.push(...tokenErrors);

  if (tokenErrors.some((d) => d.type === 'error')) {
    return diagnostics;
  }

  /**  AST 层诊断 */
  ast.forEach((root) => {
    walkAST(root, (node) => {
      diagnostics.push(...diagnoseIncompleteExpression(node));
      if (node.type !== 'function') return;

      /**  通用参数结构诊断（逗号、空参） */
      diagnostics.push(...diagnoseArgs(node, node.args, tokens));

      /**  函数规则诊断 */
      const rule = FUNCTION_RULES[node.name];
      if (!rule) return;

      const ruleDiagnostics = rule.diagnose({
        fnNode: node,
        args: node.args,
      });

      ruleDiagnostics.forEach((d) => {
        diagnostics.push({
          ...d,
          functionName: node.name,
          highlight: d.highlight ?? {
            tokenRange: [node.startTokenIndex, node.endTokenIndex],
          },
        });
      });
    });
  });

  return diagnostics;
}
