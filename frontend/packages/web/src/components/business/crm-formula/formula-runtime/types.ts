// ---- IR 类型 ----
export type IRNode = IRNumberNode | IRFieldNode | IRBinaryNode | IRFunctionNode;

export interface ResolveContext {
  /** 是否允许出现列字段 */
  allowColumn: boolean;
}

export interface IRNumberNode {
  type: 'number';
  value: number;
  numberType?: 'number';
}

export interface IRFieldNode {
  type: 'field';
  fieldId: string;
  numberType?: 'number' | 'percent' | 'date';
}

export interface IRBinaryNode {
  type: 'binary';
  operator: '+' | '-' | '*' | '/';
  left: IRNode;
  right: IRNode;
}

export interface IRFunctionNode {
  type: 'function';
  name: string;
  args: IRNode[];
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
