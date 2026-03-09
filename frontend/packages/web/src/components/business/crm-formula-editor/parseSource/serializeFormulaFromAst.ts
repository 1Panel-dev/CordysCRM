import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';

import { FormulaFormCreateField } from '../index.vue';

import { ASTNode, FormulaSerializeResult, Token, TokenType } from '../types';
import resolveASTToIR from './astToIr';

const { t } = useI18n();

export function serializeNode(
  node: ASTNode,
  fieldNameMap: Record<string, string>,
  fields: Map<
    string,
    {
      fieldId: string;
      fieldType?: string;
      numberType?: 'number' | 'percent' | 'date';
    }
  >
): { source: string; display: string } {
  let result: { source: string; display: string };
  switch (node.type) {
    case 'literal': {
      if (node.valueType === 'string') {
        result = {
          source: `"${String(node.value ?? '')}"`,
          display: `"${String(node.value ?? '')}"`,
        };
      } else if (node.valueType === 'boolean') {
        const boolText = node.value ? 'TRUE' : 'FALSE';
        result = {
          source: boolText,
          display: boolText,
        };
      } else if (node.valueType === 'number') {
        result = {
          source: String(node.value),
          display: String(node.value),
        };
      } else {
        result = {
          source: String(node.value),
          display: String(node.value),
        };
      }
      break;
    }

    case 'field': {
      const { fieldId, name, fieldType } = node;

      /** 收集字段语义（只收一次，去重） */
      if (!fields.has(fieldId)) {
        fields.set(fieldId, {
          fieldId,
          fieldType,
        });
      }

      result = {
        // source 永远用 fieldId，和 UI / name 完全解耦
        source: `\${${fieldId}}`,

        // display 仅用于展示，可被字段重命名覆盖
        display: fieldNameMap[fieldId] ?? name,
      };
      break;
    }

    case 'function': {
      const args = node.args.map((arg) => serializeNode(arg, fieldNameMap, fields));

      result = {
        source: `${node.name}(${args.map((a) => a.source).join(',')})`,
        display: `${node.name}(${args.map((a) => a.display).join(',')})`,
      };
      break;
    }

    case 'binary': {
      const left = serializeNode(node.left, fieldNameMap, fields);
      const right = serializeNode(node.right, fieldNameMap, fields);

      result = {
        /** source 中建议保留空格，利于可读 & diff */
        source: `${left.source} ${node.operator} ${right.source}`,
        display: `${left.display} ${node.operator} ${right.display}`,
      };
      break;
    }

    case 'compare': {
      const left = serializeNode(node.left, fieldNameMap, fields);
      const right = serializeNode(node.right, fieldNameMap, fields);

      result = {
        source: `${left.source} ${node.operator} ${right.source}`,
        display: `${left.display} ${node.operator} ${right.display}`,
      };
      break;
    }

    case 'empty':
      result = {
        source: '',
        display: '',
      };
      break;

    default:
      result = {
        source: '',
        display: '',
      };
      break;
  }
  if (node.parenthesized) {
    return {
      source: `(${result.source})`,
      display: `(${result.display})`,
    };
  }
  return result;
}

// 回显解析ast 收集保存入参
export function serializeFormulaFromAst(
  astList: ASTNode[],
  fieldNameMap: Record<string, string> // fieldId -> 中文名
): FormulaSerializeResult {
  const fieldMetaMap = new Map<
    string,
    {
      fieldId: string;
      fieldType?: string;
      numberType?: 'number' | 'percent' | 'date';
    }
  >();

  const sourceParts: string[] = [];
  const displayParts: string[] = [];

  astList.forEach((node) => {
    const { source, display } = serializeNode(node, fieldNameMap, fieldMetaMap);
    sourceParts.push(source);
    displayParts.push(display);
  });

  return {
    source: sourceParts.join(''),
    display: displayParts.join(''),
    fields: Array.from(fieldMetaMap.values()),
    ir: resolveASTToIR(astList[0]),
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

/**
 * 用于回显解析公式
 * @param source 公式
 * @param fieldMap 字段值映射
 * @returns 公式的token列表
 */
export function tokenizeFromSource(source: string, fieldMap: Record<string, FormulaFormCreateField>): Token[] {
  const tokens: Token[] = [];
  let i = 0;

  while (i < source?.length) {
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
          name: field?.name ?? t('common.optionNotExist'),
          fieldType: field?.type,
          start: i,
          end: end + 1,
        });

        consumed = end + 1 - i;
      } else {
        consumed = 1;
      }
    }

    // ---------- string ----------
    else if (char === '"') {
      let j = i + 1;

      while (j < source.length && source[j] !== '"') {
        j++;
      }

      if (j < source.length) {
        const value = source.slice(i + 1, j);

        tokens.push({
          type: 'string',
          value,
          start: i,
          end: j + 1,
        });

        consumed = j + 1 - i;
      } else {
        // 未闭合字符串
        tokens.push({
          type: 'unknown',
          value: source.slice(i),
          start: i,
          end: source.length,
        });

        consumed = source.length - i;
      }
    }

    // ---------- function ----------
    else if (/[A-Z]/.test(char)) {
      let j = i;
      while (j < source?.length && /[A-Z]/.test(source[j])) j++;

      const word = source.slice(i, j);

      if (word === 'TRUE' || word === 'FALSE') {
        tokens.push({
          type: 'boolean',
          value: word === 'TRUE',
          start: i,
          end: j,
        });
      } else {
        tokens.push({
          type: 'function',
          name: word,
          start: i,
          end: j,
        });
      }

      consumed = j - i;
    }

    // ---------- number（支持小数） ----------
    else if (/\d/.test(char) || (char === '.' && /\d/.test(source[i + 1]))) {
      let j = i;
      let numStr = '';
      let hasDot = false;
      let done = false;

      while (j < source.length && !done) {
        const cur = source[j];

        if (/\d/.test(cur)) {
          numStr += cur;
          j++;
        } else if (cur === '.' && !hasDot) {
          hasDot = true;
          numStr += cur;
          j++;
        } else {
          done = true;
        }
      }

      tokens.push({
        type: 'number',
        value: Number(numStr),
        start: i,
        end: j,
      });

      consumed = j - i;
    }

    // ---------- operator / comma / paren ----------
    else if (char === '=' || char === '<' || char === '>') {
      const next = source[i + 1];

      let value = char;
      let length = 1;

      if (char === '>' && next === '=') {
        value = '>=';
        length = 2;
      } else if (char === '<' && next === '=') {
        value = '<=';
        length = 2;
      } else if (char === '<' && next === '>') {
        value = '<>';
        length = 2;
      }

      tokens.push({
        type: 'operator',
        value: value as any,
        start: i,
        end: i + length,
      });

      consumed = length;
    } else {
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
  // todo xinxinwu debugger
  // console.log(tokens, 'tokens:tokenizeFromSource');

  return tokens;
}
