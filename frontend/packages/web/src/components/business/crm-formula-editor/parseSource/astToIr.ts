// astToIr.ts Ast--->IR
import { IRNodeType } from '@lib/shared/enums/formula';

import { IRNode, ResolveContext } from '@/components/business/crm-formula/formula-runtime/types';

import { ASTNode, FunctionNode } from '../types';

function wrapColumnIfNeeded(node: IRNode): IRNode {
  // 已经是标量，直接返回
  if (node.type !== IRNodeType.Field) return node;

  // 不是子表字段
  if (!node.fieldId.includes('.')) return node;

  // SUM(column)
  return {
    type: IRNodeType.Function,
    name: 'SUM',
    args: [node],
  };
}

function resolveNode(node: ASTNode, ctx: ResolveContext): IRNode {
  switch (node.type) {
    case IRNodeType.Literal:
      return {
        type: IRNodeType.Literal,
        value: node.value,
        valueType: node.valueType as any,
      };

    case IRNodeType.Field: {
      const ir: IRNode = { ...node } as IRNode;
      return ctx.expectScalar ? wrapColumnIfNeeded(ir) : ir;
    }

    case IRNodeType.Binary:
      return {
        type: IRNodeType.Binary,
        operator: node.operator,
        left: resolveNode(node.left, { expectScalar: true }),
        right: resolveNode(node.right, { expectScalar: true }),
      };

    case IRNodeType.Compare:
      return {
        type: IRNodeType.Compare,
        operator: (node as any).operator, // '=' | '!=' | '>' | '>=' | '<' | '<='
        left: resolveNode((node as any).left, { expectScalar: true }),
        right: resolveNode((node as any).right, { expectScalar: true }),
      };

    case IRNodeType.Function:
      // eslint-disable-next-line no-use-before-define
      return resolveFunction(node, ctx);
    default:
      return {
        type: IRNodeType.Invalid,
        reason: `unknown node type`,
      };
  }
}

function resolveFunction(node: FunctionNode, _ctx: ResolveContext): IRNode {
  const name = node.name?.toUpperCase();
  if (!name) {
    return {
      type: IRNodeType.Invalid,
      reason: 'function name missing',
    };
  }

  /**
   * 特殊函数规则
   */
  switch (name) {
    case 'SUM':
      return {
        type: IRNodeType.Function,
        name,
        args: node.args.map((arg) => resolveNode(arg, { expectScalar: false })),
      };

    case 'DAYS':
      if (node.args.length !== 2) {
        return {
          type: IRNodeType.Invalid,
          reason: 'DAYS requires 2 arguments',
        };
      }

      return {
        type: IRNodeType.Function,
        name,
        args: node.args.map((arg) => resolveNode(arg, { expectScalar: true })),
      };

    default:
      /**
       * 默认函数行为
       *
       * IF / IFS / AND / CONCATENATE / TODAY / NOW / TEXT 等
       */
      return {
        type: IRNodeType.Function,
        name,
        args: node.args.map((arg) => resolveNode(arg, { expectScalar: true })),
      };
  }
}

// 解析 AST -> IR,用于运行执行器计算
export default function resolveASTToIR(ast: ASTNode): IRNode | null {
  if (!ast) return null;

  return resolveNode(ast, { expectScalar: true });
}
