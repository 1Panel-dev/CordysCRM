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
  switch (node.type) {
    case 'number':
      return {
        source: String(node.value),
        display: String(node.value),
      };

    case 'field': {
      const { fieldId, name, fieldType, numberType } = node;

      /** 收集字段语义（只收一次，去重） */
      if (!fields.has(fieldId)) {
        fields.set(fieldId, {
          fieldId,
          fieldType,
          numberType,
        });
      }

      return {
        // source 永远用 fieldId，和 UI / name 完全解耦
        source: `\${${fieldId}}`,

        // display 仅用于展示，可被字段重命名覆盖
        display: fieldNameMap[fieldId] ?? name,
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
        /** source 中建议保留空格，利于可读 & diff */
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
      while (j < source?.length && /[A-Z]/.test(source[j])) j++;

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
      while (j < source?.length && /\d/.test(source[j])) j++;

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
