// ----token类型----
export type TokenType = 'function' | 'field' | 'number' | 'operator' | 'comma' | 'paren' | 'text';

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
}

export interface TextToken extends BaseToken {
  type: 'text';
  value: string;
}

export interface NumberToken extends BaseToken {
  type: 'number';
  value: number;
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
}

export interface NumberNode extends ASTNodeBase {
  type: 'number';
  value: number;
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
