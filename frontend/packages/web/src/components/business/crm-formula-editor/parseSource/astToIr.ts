import { IRNode, ResolveContext } from '@/components/business/crm-formula/formula-runtime/types';

import { ASTNode, FunctionNode } from '../types';

function resolveNode(node: ASTNode, ctx: ResolveContext): IRNode {
  switch (node.type) {
    case 'number':
      return node as IRNode;

    case 'field': {
      const isColumn = node.fieldId.includes('.');
      if (isColumn && !ctx.allowColumn) {
        throw new Error(`field ${node.fieldId} can only be used in SUM function`);
      }
      return node;
    }

    case 'binary':
      return {
        type: 'binary',
        operator: node.operator,
        left: resolveNode(node.left, { allowColumn: false }),
        right: resolveNode(node.right, { allowColumn: false }),
      };

    case 'function':
      // eslint-disable-next-line no-use-before-define
      return resolveFunction(node, ctx);
    default:
      throw new Error('unknown node type');
  }
}
function resolveFunction(node: FunctionNode, _ctx: ResolveContext): IRNode {
  switch (node.name) {
    case 'SUM':
      return {
        type: 'function',
        name: 'SUM',
        args: node.args.map((arg) => resolveNode(arg, { allowColumn: true })),
      };

    case 'DAYS':
      if (node.args.length !== 2) {
        throw new Error('DAYS function must have 2 arguments');
      }
      return {
        type: 'function',
        name: 'DAYS',
        args: node.args.map((arg) => resolveNode(arg, { allowColumn: false })),
      };

    default:
      throw new Error(`unknown function ${node.name}`);
  }
}

// 解析 AST -> IR,用于运行执行器计算
export default function resolveASTToIR(ast: ASTNode): IRNode {
  return resolveNode(ast, { allowColumn: false });
}
