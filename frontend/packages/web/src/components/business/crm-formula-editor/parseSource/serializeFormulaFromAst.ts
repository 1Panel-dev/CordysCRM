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
    case 'number':
      result = {
        source: String(node.value),
        display: String(node.value),
      };
      break;

    case 'field': {
      const { fieldId, name, fieldType, numberType } = node;

      if (!fields.has(fieldId)) {
        fields.set(fieldId, {
          fieldId,
          fieldType,
          numberType,
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
        source: `${node.name}(${args.map((a) => a.source).join(', ')})`,
        display: `${node.name}(${args.map((a) => a.display).join(', ')})`,
      };
      break;
    }

    case 'binary': {
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

  const isWhitespace = (ch: string) => /\s/.test(ch);
  const isDigit = (ch: string) => /\d/.test(ch);
  const isUpperLetter = (ch: string) => /[A-Z]/.test(ch);

  while (i < source.length) {
    const char = source[i];
    let consumed = 1;

    // ---------- whitespace ----------
    if (isWhitespace(char)) {
      consumed = 1;
    }

    // ---------- field: ${fieldId} ----------
    else if (char === '$' && source[i + 1] === '{') {
      let j = i + 2;
      while (j < source.length && source[j] !== '}') {
        j++;
      }

      if (j < source.length && source[j] === '}') {
        const fieldId = source.slice(i + 2, j).trim();
        const field = fieldMap[fieldId];

        let numberType: 'number' | 'percent' | 'date' = 'number';
        if ([FieldTypeEnum.INPUT_NUMBER].includes(field?.type as FieldTypeEnum)) {
          numberType = field?.numberFormat === 'percent' ? 'percent' : 'number';
        } else if ([FieldTypeEnum.DATE_TIME].includes(field?.type as FieldTypeEnum)) {
          numberType = 'date';
        }

        tokens.push({
          type: 'field',
          fieldId,
          name: field?.name ?? t('common.optionNotExist'),
          fieldType: field?.type,
          numberType,
          start: i,
          end: j + 1,
        });

        consumed = j + 1 - i;
      } else {
        // 未闭合 ${... ，按 text 保底
        tokens.push({
          type: 'text',
          value: source.slice(i),
          start: i,
          end: source.length,
        });
        consumed = source.length - i;
      }
    }

    // ---------- function ----------
    else if (isUpperLetter(char)) {
      let j = i;
      while (j < source.length && /[A-Z0-9_]/.test(source[j])) {
        j++;
      }

      tokens.push({
        type: 'function',
        name: source.slice(i, j),
        start: i,
        end: j,
      });

      consumed = j - i;
    }

    // ---------- number ----------
    else if (isDigit(char) || (char === '.' && isDigit(source[i + 1] ?? ''))) {
      let j = i;
      let hasDot = false;

      if (source[j] === '.') {
        hasDot = true;
        j++;
      }

      while (j < source.length) {
        const ch = source[j];

        if (isDigit(ch)) {
          j++;
        } else if (ch === '.' && !hasDot) {
          hasDot = true;
          j++;
        } else {
          break;
        }
      }

      const raw = source.slice(i, j);

      tokens.push({
        type: 'number',
        value: Number(raw),
        numberType: 'number',
        start: i,
        end: j,
      });

      consumed = j - i;
    }

    // ---------- comma / paren / operator ----------
    else if (Object.prototype.hasOwnProperty.call(CHAR_TOKEN_TYPE_MAP, char)) {
      const tokenType = CHAR_TOKEN_TYPE_MAP[char];

      tokens.push({
        type: tokenType,
        value: char as any,
        start: i,
        end: i + 1,
      } as Token);

      consumed = 1;
    }

    // ---------- text ----------
    else {
      let j = i;
      while (
        j < source.length &&
        !isWhitespace(source[j]) &&
        !(source[j] === '$' && source[j + 1] === '{') &&
        !isUpperLetter(source[j]) &&
        !isDigit(source[j]) &&
        !(source[j] === '.' && isDigit(source[j + 1] ?? '')) &&
        !Object.prototype.hasOwnProperty.call(CHAR_TOKEN_TYPE_MAP, source[j])
      ) {
        j++;
      }

      tokens.push({
        type: 'text',
        value: source.slice(i, j),
        start: i,
        end: j,
      });

      consumed = j - i;
    }

    i += consumed;
  }

  return tokens;
}
