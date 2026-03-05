import { IRNodeType } from '@lib/shared/enums/formula';

export interface ResolveContext {
  /** 是否允许出现列字段 */
  expectScalar: boolean;
}

// ---- IR 类型 ----
export type IRNode = IRLiteralNode | IRFieldNode | IRBinaryNode | IRCompareNode | IRFunctionNode | IRInvalidNode;

export interface IRLiteralNode {
  type: IRNodeType.Literal;
  value: unknown;
  valueType: 'number' | 'string' | boolean;
}

export interface IRCompareNode {
  type: IRNodeType.Compare;
  operator: '=' | '<>' | '>' | '>=' | '<' | '<=';
  left: IRNode;
  right: IRNode;
}

export interface IRFieldNode {
  type: IRNodeType.Field;
  fieldId: string;
  numberType?: 'number' | 'percent' | 'date'; // todo
}

export interface IRBinaryNode {
  type: IRNodeType.Binary;
  operator: '+' | '-' | '*' | '/';
  left: IRNode;
  right: IRNode;
}

export interface IRFunctionNode {
  type: IRNodeType.Function;
  name: string;
  args: IRNode[];
}

interface IRInvalidNode {
  type: IRNodeType.Invalid;
  reason: string;
}

// -------- Runtime Context --------
export interface EvaluateContext {
  /** 当前是否在子表 */
  context?: {
    tableKey?: string;
    rowIndex?: number;
  };

  /** 取单值字段 */
  getScalarFieldValue(fieldId: string, ctx?: EvaluateContext['context']): number;

  /** 取列字段 */
  getTableColumnValues(path: string): number[];

  warn?(msg: string): void;
}

export type FormulaResultType = 'number' | 'string' | 'boolean' | 'date' | 'unknown';
