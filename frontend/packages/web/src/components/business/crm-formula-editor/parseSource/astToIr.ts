import { IRNode, ResolveContext } from '@/components/business/crm-formula/formula-runtime/types';

import { ASTNode, FunctionNode } from '../types';

function wrapColumnIfNeeded(node: IRNode): IRNode {
  // 已经是标量，直接返回
  if (node.type !== 'field') return node;

  // 不是子表字段
  if (!node.fieldId.includes('.')) return node;

  // SUM(column)
  return {
    type: 'function',
    name: 'SUM',
    args: [node],
  };
}

function resolveNode(node: ASTNode, ctx: ResolveContext): IRNode {
  switch (node.type) {
    case 'number':
      return node as IRNode;

    case 'field': {
      const ir: IRNode = node;
      return ctx.expectScalar ? wrapColumnIfNeeded(ir) : ir;
    }

    case 'binary':
      return {
        type: 'binary',
        operator: node.operator,
        left: resolveNode(node.left, { expectScalar: true }),
        right: resolveNode(node.right, { expectScalar: true }),
      };

    case 'function':
      // eslint-disable-next-line no-use-before-define
      return resolveFunction(node, ctx);
    default:
      return {
        type: 'invalid',
        reason: `unknown node type`,
      };
  }
}
function resolveFunction(node: FunctionNode, _ctx: ResolveContext): IRNode {
  switch (node.name) {
    case 'SUM':
      return {
        type: 'function',
        name: 'SUM',
        args: node.args.map((arg) => resolveNode(arg, { expectScalar: false })),
      };

    case 'DAYS':
      if (node.args.length !== 2) {
        return {
          type: 'invalid',
          reason: 'DAYS function must have 2 arguments',
        };
      }
      return {
        type: 'function',
        name: 'DAYS',
        args: node.args.map((arg) => resolveNode(arg, { expectScalar: true })),
      };

    default:
      return {
        type: 'invalid',
        reason: `unknown function ${node.name}`,
      };
  }
}

// 解析 AST -> IR,用于运行执行器计算
export default function resolveASTToIR(ast: ASTNode): IRNode | null {
  if (!ast) return null;

  return resolveNode(ast, { expectScalar: true });
}
