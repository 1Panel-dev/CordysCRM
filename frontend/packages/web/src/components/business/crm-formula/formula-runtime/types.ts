import { IRNodeType } from '@lib/shared/enums/formula';

import { FormCreateField } from '@/components/business/crm-form-create/types';

export interface ResolveContext {
  /** 是否允许出现列字段 */
  expectScalar: boolean;
}

// ---- IR 类型 ----
export type IRNode = IRLiteralNode | IRFieldNode | IRBinaryNode | IRCompareNode | IRFunctionNode | IRInvalidNode;

export interface IRLiteralNode {
  type: IRNodeType.Literal;
  value: unknown;
  valueType: 'number' | 'string' | 'boolean';
}

// 比较符
export type CompareOperator = '=' | '<>' | '>' | '>=' | '<' | '<=';

export interface IRCompareNode {
  type: IRNodeType.Compare;
  operator: CompareOperator;
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

export type ValueType = 'number' | 'string' | 'boolean' | 'date' | 'unknown';

export interface FieldMeta {
  valueType: ValueType;
  numberType?: 'number' | 'percent';
}

export type FieldTypeMap = Record<string, FieldMeta>;

export type FormulaDataSourceMap = Record<
  string,
  {
    parserName: boolean;
    options: Record<string, any>[];
  }
>;

// -------- Runtime Context --------
export interface EvaluateContext {
  evaluationNow: Date | null;
  /** 当前是否在子表 */
  context?: {
    tableKey?: string;
    rowIndex?: number;
  };

  /** 取单值字段 */
  getScalarFieldValue(fieldId: string, ctx?: EvaluateContext['context']): number;

  /** 取列字段 */
  getTableColumnValues(path: string): number[];
  getFieldMeta?(fieldId: string): FieldMeta | undefined;
  resolveFieldRuntimeValue?(fieldId: string, rawValue: any): any;

  warn?(msg: string): void;
}

export type FunctionSource = 'builtin' | 'external' | 'plugin';

export interface FnSpec {
  fn: (ctx: EvaluateContext, ...args: any[]) => any;
  source?: FunctionSource; // 函数来源
  lazy?: boolean;
}

export interface FormulaExecutorContext {
  formula?: string;
  path?: string;
  formDetail?: Record<string, any>;
  fields?: FormCreateField[];
  formulaDataSource: Record<string, any>;
  evaluationNow?: any;
  decimalPlaces?: number;
  expectedType?: ValueType;
  cloneIR?: boolean;
  warn?: (msg: string) => void;
}

export interface FormulaExecutorResult {
  rawResult: any;
  normalizedResult: any;
}
