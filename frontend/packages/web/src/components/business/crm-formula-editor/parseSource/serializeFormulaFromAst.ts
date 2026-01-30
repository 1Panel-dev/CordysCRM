import { FormulaFormCreateField } from '../index.vue';

import { ASTNode, Token, TokenType } from '../types';

export interface FormulaSerializeResult {
  source: string; // SUM(${123}, ${456}) + DAYS(...)
  display: string; // SUM(报价产品.价格, 订阅表格.价格)
  fields: string[]; // ["fieldId", ...]
}

export function serializeNode(
  node: ASTNode,
  fieldNameMap: Record<string, string>,
  fields: Set<string>
): { source: string; display: string } {
  switch (node.type) {
    case 'number':
      return {
        source: String(node.value),
        display: String(node.value),
      };

    case 'field': {
      fields.add(node.fieldId);

      return {
        source: `\${${node.fieldId}}`,
        display: fieldNameMap[node.fieldId] ?? node.name,
      };
    }

    case 'function': {
      const args = node.args.map((arg) => serializeNode(arg, fieldNameMap, fields));

      return {
        source: `${node.name}(${args.map((a) => a.source).join(', ')})`,
        display: `${node.name}(${args.map((a) => a.display).join(', ')})`,
      };
    }

    case 'binary': {
      const left = serializeNode(node.left, fieldNameMap, fields);
      const right = serializeNode(node.right, fieldNameMap, fields);

      return {
        source: `${left.source} ${node.operator} ${right.source}`,
        display: `${left.display} ${node.operator} ${right.display}`,
      };
    }

    case 'empty':
      return {
        source: '',
        display: '',
      };

    default:
      return {
        source: '',
        display: '',
      };
  }
}

// 回显解析ast 收集保存入参
export function serializeFormulaFromAst(
  astList: ASTNode[],
  fieldNameMap: Record<string, string> // fieldId -> 中文名
): FormulaSerializeResult {
  const fields = new Set<string>();

  const sourceParts: string[] = [];
  const displayParts: string[] = [];

  astList.forEach((node) => {
    const { source, display } = serializeNode(node, fieldNameMap, fields);
    sourceParts.push(source);
    displayParts.push(display);
  });

  return {
    source: sourceParts.join(''),
    display: displayParts.join(''),
    fields: Array.from(fields),
  };
}

const CHAR_TOKEN_TYPE_MAP: Record<string, TokenType> = {
  ',': 'comma',
  '，': 'comma',
  '(': 'paren',
  ')': 'paren',
  '+': 'operator',
  '-': 'operator',
  '*': 'operator',
  '/': 'operator',
};

export function tokenizeFromSource(source: string, fieldMap: Record<string, FormulaFormCreateField>): Token[] {
  const tokens: Token[] = [];
  let i = 0;

  while (i < source.length) {
    let consumed = 0;
    const char = source[i];

    // ---------- field ----------
    if (char === '$' && source[i + 1] === '{') {
      const end = source.indexOf('}', i);
      if (end !== -1) {
        const fieldId = source.slice(i + 2, end).trim();
        const field = fieldMap[fieldId];

        tokens.push({
          type: 'field',
          fieldId,
          name: field?.name ?? `字段${fieldId}`,
          fieldType: field?.type,
          start: i,
          end: end + 1,
        });

        consumed = end + 1 - i;
      } else {
        consumed = 1;
      }
    }

    // ---------- function ----------
    else if (/[A-Z]/.test(char)) {
      let j = i;
      while (j < source.length && /[A-Z]/.test(source[j])) j++;

      tokens.push({
        type: 'function',
        name: source.slice(i, j),
        start: i,
        end: j,
      });

      consumed = j - i;
    }

    // ---------- number ----------
    else if (/\d/.test(char)) {
      let j = i;
      while (j < source.length && /\d/.test(source[j])) j++;

      tokens.push({
        type: 'number',
        value: Number(source.slice(i, j)),
        start: i,
        end: j,
      });

      consumed = j - i;
    }

    // ---------- operator / comma / paren ----------
    else {
      const tokenType = CHAR_TOKEN_TYPE_MAP[char];
      if (tokenType) {
        tokens.push({
          type: tokenType,
          value: char,
          start: i,
          end: i + 1,
        } as Token);
      }
      consumed = 1;
    }

    i += consumed || 1;
  }

  return tokens;
}
