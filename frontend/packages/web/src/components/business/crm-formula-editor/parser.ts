// token--> AST结构
import { ASTNode, FieldToken, FunctionNode, FunctionToken, NumberToken, OperatorToken, Token } from './types';

export default function parseTokensToAST(tokens: Token[]): ASTNode[] {
  const meaningfulTokens = tokens.filter((t) => t.type !== 'text');

  let index = 0;

  const peek = () => meaningfulTokens[index];
  const consume = () => meaningfulTokens[index++];

  function parsePrimary(): ASTNode {
    const token = peek();

    if (!token) {
      return {
        type: 'empty',
        startTokenIndex: index,
        endTokenIndex: index,
      };
    }

    if (token.type === 'number') {
      const t = consume() as NumberToken;
      return {
        type: 'number',
        value: t.value,
        startTokenIndex: index - 1,
        endTokenIndex: index - 1,
      };
    }

    if (token.type === 'field') {
      const t = consume() as FieldToken;
      return {
        type: 'field',
        fieldId: t.fieldId,
        name: t.name,
        fieldType: t.fieldType,
        startTokenIndex: index - 1,
        endTokenIndex: index - 1,
      };
    }

    if (token.type === 'function') {
      // eslint-disable-next-line no-use-before-define
      return parseFunctionCall();
    }

    if (token.type === 'paren' && token.value === '(') {
      const startIndex = index;
      consume(); // '('

      // eslint-disable-next-line no-use-before-define
      const expr = parseAdditive();

      const close = peek();
      if (close?.type === 'paren' && close.value === ')') {
        consume();
      }

      expr.startTokenIndex = startIndex;
      expr.endTokenIndex = index - 1;
      return expr;
    }

    consume();
    return {
      type: 'empty',
      startTokenIndex: index - 1,
      endTokenIndex: index - 1,
    };
  }

  function parseMultiplicative(): ASTNode {
    let node = parsePrimary();

    let next = peek();
    while (next?.type === 'operator' && (next.value === '*' || next.value === '/')) {
      const op = consume() as OperatorToken;
      const right = parsePrimary();

      node = {
        type: 'binary',
        operator: op.value,
        left: node,
        right,
        startTokenIndex: node.startTokenIndex,
        endTokenIndex: right.endTokenIndex,
      };

      next = peek();
    }

    return node;
  }

  function parseAdditive(): ASTNode {
    let node = parseMultiplicative();

    let next = peek();
    while (next?.type === 'operator' && (next.value === '+' || next.value === '-')) {
      const op = consume() as OperatorToken;
      const right = parseMultiplicative();

      node = {
        type: 'binary',
        operator: op.value,
        left: node,
        right,
        startTokenIndex: node.startTokenIndex,
        endTokenIndex: right.endTokenIndex,
      };

      next = peek();
    }

    return node;
  }

  function parseFunctionCall(): FunctionNode {
    const fnToken = consume() as FunctionToken;
    const startIndex = index - 1;

    const args: ASTNode[] = [];

    const next = peek();
    if (next?.type === 'paren' && next.value === '(') {
      consume(); // '('

      let current = peek();
      while (current && !(current.type === 'paren' && current.value === ')')) {
        if (current.type === 'comma') {
          args.push({
            type: 'empty',
            startTokenIndex: index,
            endTokenIndex: index,
          });
          consume();
        } else {
          const expr = parseAdditive();
          args.push(expr);

          const after = peek();
          if (after?.type === 'comma') {
            consume();
          }
        }

        current = peek();
      }

      if (peek()?.type === 'paren') {
        consume();
      }
    }

    return {
      type: 'function',
      name: fnToken.name,
      args,
      startTokenIndex: startIndex,
      endTokenIndex: index - 1,
    };
  }

  const astList: ASTNode[] = [];

  while (index < meaningfulTokens.length) {
    astList.push(parseAdditive());
  }

  return astList;
}
