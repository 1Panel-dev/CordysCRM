import { IRNode } from '@/components/business/crm-formula/formula-runtime/types';

// ----token类型----
export type TokenType = 'function' | 'field' | 'number' | 'operator' | 'comma' | 'paren' | 'text';

export type NumberType = 'number' | 'percent' | 'date';

// todo viewStart&sourceStart
export interface BaseToken {
  type: TokenType;
  start: number;
  end: number;
}

export interface FunctionToken extends BaseToken {
  type: 'function';
  name: string; // SUM / DAYS
}

export interface FieldToken extends BaseToken {
  type: 'field';
  fieldId: string;
  name: string;
  fieldType?: string;
  numberType?: NumberType; // 字段的数值类型
}

export interface TextToken extends BaseToken {
  type: 'text';
  value: string;
}

export interface NumberToken extends BaseToken {
  type: 'number';
  value: number;
  numberType?: NumberType;
}

export type OperatorValue = '+' | '-' | '*' | '/';

export interface OperatorToken extends BaseToken {
  type: 'operator';
  value: OperatorValue;
}

export interface CommaToken extends BaseToken {
  type: 'comma';
  value: string;
}

export type ParenValue = '(' | ')';

export interface ParenToken extends BaseToken {
  type: 'paren';
  value: ParenValue;
}

export type Token = FunctionToken | FieldToken | NumberToken | OperatorToken | CommaToken | ParenToken | TextToken;

// ----AST类型----
export type ASTNode = FunctionNode | FieldNode | NumberNode | BinaryExpressionNode | EmptyNode;

export interface ASTNodeBase {
  startTokenIndex: number;
  endTokenIndex: number;
}

export interface EmptyNode extends ASTNodeBase {
  type: 'empty';
}

export interface FunctionNode extends ASTNodeBase {
  type: 'function';
  name: string;
  args: ASTNode[];
}

export interface FieldNode extends ASTNodeBase {
  type: 'field';
  fieldId: string;
  name: string;
  fieldType?: string;
  numberType?: NumberType;
}

export interface NumberNode extends ASTNodeBase {
  type: 'number';
  value: number;
  numberType?: NumberType;
}

export interface BinaryExpressionNode extends ASTNodeBase {
  type: 'binary';
  operator: '+' | '-' | '*' | '/';
  left: ASTNode;
  right: ASTNode;
}

// 诊断相关类型
export type FunctionDiagnoseContext = {
  fnNode: FunctionNode;
  args: ASTNode[];
  tokens: Token[];
};

export type FormulaDiagnostic = {
  type: 'error' | 'warning';
  code: string;
  message: string;
  functionName?: string;
  highlight?: {
    tokenRange: [number, number]; // token 的区间
    char?: string; // 高亮的字符
    range?: [number, number]; // 在 formula string 中的字符区间
    fieldId?: string;
  };
};

export type FormulaFunctionRule = {
  name: string;
  diagnose(ctx: { fnNode: FunctionNode; args: ASTNode[] }): FormulaDiagnostic[]; // 诊断函数
};

export interface FormulaFieldMeta {
  fieldId: string;
  fieldType?: string;
  numberType?: NumberType;
}

export interface FormulaSerializeResult {
  source: string; // SUM(${123}, ${456}) + DAYS(...)
  display: string; // SUM(报价产品.价格, 订阅表格.价格)
  fields: FormulaFieldMeta[];
  ir: IRNode; // 公式 IR
}
