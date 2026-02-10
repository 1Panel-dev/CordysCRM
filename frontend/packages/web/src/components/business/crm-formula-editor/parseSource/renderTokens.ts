import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';

import { ARRAY_COLOR, DATE_TIME_COLOR, FUN_COLOR, INPUT_NUMBER_COLOR } from '../config';
import { FieldToken, Token } from '../types';

export function createFunctionNode(fnName: string) {
  const node = document.createElement('span');
  node.className = 'formula-fn';
  node.style.color = FUN_COLOR;
  node.contentEditable = 'true';
  node.dataset.nodeType = 'function';
  node.dataset.fnName = fnName;
  node.textContent = fnName;
  return node;
}

function getFormulaNodeColor(token: Token) {
  if (token.type === 'function') {
    return FUN_COLOR;
  }

  // —— 数组字段（二级路径）
  if ((token as FieldToken).fieldId?.includes('.')) {
    return ARRAY_COLOR;
  }

  // —— 普通字段
  switch ((token as FieldToken).fieldType) {
    case FieldTypeEnum.INPUT_NUMBER:
      return INPUT_NUMBER_COLOR;

    case FieldTypeEnum.DATE_TIME:
      return DATE_TIME_COLOR;

    default:
      return '';
  }
}

export function createFieldNode(token: Token) {
  const parsedToken = token as FieldToken;
  const node = document.createElement('span');
  node.className = 'formula-tag-wrapper';
  node.contentEditable = 'true';
  node.dataset.nodeType = 'field';
  if ((token as FieldToken)?.fieldType) {
    node.dataset.fieldType = (token as FieldToken)?.fieldType;
  }

  if ((token as FieldToken)?.numberType) {
    node.dataset.numberType = (token as FieldToken)?.numberType;
  }

  node.style.color = getFormulaNodeColor(token);

  if (parsedToken.fieldId) {
    node.dataset.value = parsedToken.fieldId;
  }
  if (parsedToken.fieldType) {
    node.dataset.fieldType = parsedToken.fieldType;
  }
  if (parsedToken.name) {
    node.textContent = parsedToken.name;
    node.dataset.originText = parsedToken.name;
  }
  return node;
}

/**
 * 回显渲染 tokens
 */
export function renderTokens(tokens: Token[], startIndex = 0): { fragment: DocumentFragment; endIndex: number } {
  const fragment = document.createDocumentFragment();
  let i = startIndex;

  while (i < tokens.length) {
    const token = tokens[i];

    // function
    if (token.type === 'function') {
      const fnNode = createFunctionNode(token.name!);
      fragment.appendChild(fnNode);

      // '('
      fragment.appendChild(document.createTextNode('('));

      // args container
      const argsNode = document.createElement('span');
      argsNode.className = 'formula-args';

      // 跳过 '('，递归渲染参数
      const { fragment: argsFragment, endIndex } = renderTokens(tokens, i + 2);
      argsNode.appendChild(argsFragment);

      // 空参数兜底
      if (!argsNode.firstChild) {
        argsNode.appendChild(document.createTextNode('\u200B'));
      }

      fragment.appendChild(argsNode);
      fragment.appendChild(document.createTextNode(')'));

      i = endIndex + 1;
    }

    // paren end
    else if (token.type === 'paren' && token.value === ')') {
      return { fragment, endIndex: i };
    }

    // field
    else if (token.type === 'field') {
      fragment.appendChild(createFieldNode(token));
      i++;
    }

    // others
    else {
      fragment.appendChild(document.createTextNode(token.type === 'number' ? String(token.value) : token.value!));
      i++;
    }
  }

  return { fragment, endIndex: i };
}
/**
 * 将解析后的 tokens 渲染到编辑器中回显
 * @param editor 公式编辑器实例
 * @param tokens 公式节点tokens列表
 */
export function renderTokensToEditor(editor: HTMLElement, tokens: Token[]) {
  editor.innerHTML = '';

  const { fragment } = renderTokens(tokens);
  editor.appendChild(fragment);

  // 结尾放一个空格，保证可继续输入
  editor.appendChild(document.createTextNode('\u200B'));
}
