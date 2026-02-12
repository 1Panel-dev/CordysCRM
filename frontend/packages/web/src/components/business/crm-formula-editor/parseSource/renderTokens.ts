import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';

import { ARRAY_COLOR, DATE_TIME_COLOR, FUN_COLOR, INPUT_NUMBER_COLOR } from '../config';
import { FieldToken, Token } from '../types';
import { createCaretText } from '../utils';

/**
 * 将一个 atomic node 插入到 container 中，保证其左右两侧都是 text node（可输入）
 * @param container 插入容器
 * @param atomicNode // 待插入的 atomic node
 */
export function insertAtomic(container: Node, atomicNode: HTMLElement) {
  const leftText = createCaretText();
  const rightText = createCaretText();

  container.appendChild(leftText);
  container.appendChild(atomicNode);
  container.appendChild(rightText);
}

/**
 * 创建一个函数节点
 * @param fnName 函数名称
 * @param options 可选参数
 * @returns 包含根节点和参数节点的对象
 */
export function createFunctionNode(
  fnName: string,
  options?: {
    argsFragment?: DocumentFragment;
    withCaret?: boolean; // 是否保证 args 内有 caret
  }
) {
  const { argsFragment, withCaret = true } = options ?? {};

  const root = document.createElement('span');
  root.className = 'formula-fn-root';
  root.dataset.nodeType = 'function';
  root.dataset.fnName = fnName;
  root.contentEditable = 'false';

  /** 函数名 */
  const name = document.createElement('span');
  name.className = 'fn-name';
  name.style.color = FUN_COLOR;
  name.textContent = fnName;

  /** ( */
  const lp = document.createElement('span');
  lp.className = 'fn-paren';
  lp.textContent = '(';
  lp.style.marginRight = '2px';

  /** 参数区（唯一可编辑区域） */
  const args = document.createElement('span');
  args.className = 'fn-args';
  args.contentEditable = 'true';
  args.style.padding = '2px 4px';

  if (argsFragment && argsFragment.childNodes.length > 0) {
    args.appendChild(argsFragment);
  } else if (withCaret) {
    args.appendChild(document.createTextNode(''));
  }

  /** ) */
  const rp = document.createElement('span');
  rp.className = 'fn-paren';
  rp.textContent = ')';
  rp.style.marginLeft = '2px';

  root.append(name, lp, args, rp);

  return { root, args };
}

/**
 *    创建一个函数根节点，适用于回显时使用
 * @param fnName 函数名称
 * @param argsFragment 参数片段
 * @returns 函数根节点
 */
export function createFunctionRootNode(fnName: string, argsFragment?: DocumentFragment) {
  const { root } = createFunctionNode(fnName, {
    argsFragment,
    withCaret: true, // 回显也要保证 args 非空
  });

  return root;
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
  node.contentEditable = 'false';
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
      // 跳过 function + '('
      const { fragment: argsFragment, endIndex } = renderTokens(tokens, i + 2);

      const fnRoot = createFunctionRootNode(token.name!, argsFragment);

      // 函数整体作为 atomic 插入
      insertAtomic(fragment, fnRoot);

      i = endIndex + 1;
    } else if (token.type === 'paren' && token.value === ')') {
      /** ---------- paren end ---------- */
      return { fragment, endIndex: i };
    } else if (token.type === 'field') {
      /** ---------- field ---------- */
      const fieldNode = createFieldNode(token);
      insertAtomic(fragment, fieldNode);
      i++;
    } else {
      /** ---------- other ---------- */
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

  // 保证结尾一定text node（可输入）
  const last = editor.lastChild;
  if (!last || last.nodeType !== Node.TEXT_NODE) {
    editor.appendChild(createCaretText());
  }
}
