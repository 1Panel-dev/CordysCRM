import { defaultFormulaConfig } from './config';
import { FormulaDiagnostic } from './types';

export function createRangeFromStringIndex(root: HTMLElement, start: number, end: number): Range | null {
  const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
  let currentIndex = 0;

  let startNode: Text | null = null;
  let startOffset = 0;
  let endNode: Text | null = null;
  let endOffset = 0;

  while (walker.nextNode()) {
    const node = walker.currentNode as Text;
    const len = node.textContent?.length ?? 0;

    if (!startNode && currentIndex + len >= start) {
      startNode = node;
      startOffset = start - currentIndex;
    }

    if (startNode && currentIndex + len >= end) {
      endNode = node;
      endOffset = end - currentIndex;
      break;
    }

    currentIndex += len;
  }

  if (!startNode || !endNode) return null;

  const range = document.createRange();
  range.setStart(startNode, startOffset);
  range.setEnd(endNode, endOffset);
  return range;
}

export function clearErrorHighlight(editor: HTMLElement) {
  editor.querySelectorAll('.formula-error').forEach((el) => {
    el.replaceWith(document.createTextNode(el.textContent || ''));
  });
}

export function applyDiagnosticsHighlight(editor: HTMLElement, diagnostics: FormulaDiagnostic[]) {
  clearErrorHighlight(editor);

  diagnostics.forEach((diag) => {
    if (!diag.highlight?.range) return;

    const range = createRangeFromStringIndex(editor, diag.highlight.range[0], diag.highlight.range[1]);

    if (!range) return;

    const span = document.createElement('span');
    span.className = 'formula-error';
    span.style.backgroundColor = 'rgba(255,0,0,0.2)';
    span.style.borderBottom = '1px solid red';

    range.surroundContents(span);
  });
}

export function safeParseFormula(formulaString?: string) {
  try {
    return JSON.parse(formulaString ?? '');
  } catch (e) {
    return defaultFormulaConfig;
  }
}
/**
 * 创建一个不可编辑的文本节点（NBSP），用于占位和保持光标位置
 * @returns 不可编辑的文本节点
 */
export function createCaretText() {
  const text = document.createTextNode('\u00A0'); // NBSP
  return text;
}

/**
 * 确保 atomic 节点两侧有可编辑的文本节点（光标占位）
 * @param node atomic 节点
 */
export function ensureCaretAroundAtomic(node: HTMLElement) {
  const prev = node.previousSibling;
  const next = node.nextSibling;

  if (!prev || prev.nodeType !== Node.TEXT_NODE) {
    node.before(createCaretText());
  }
  if (!next || next.nodeType !== Node.TEXT_NODE) {
    node.after(createCaretText());
  }
}

/**
 *  规范化编辑器结构，确保所有 atomic 节点两侧都有可编辑的文本节点
 * @param root 编辑器根节点
 */
export function normalizeEditorStructure(root: HTMLElement) {
  const walker = document.createTreeWalker(root, NodeFilter.SHOW_ELEMENT, null);

  let node: Node | null;
  while (true) {
    node = walker.nextNode();
    if (!node) break;

    const el = node as HTMLElement;
    if (el.dataset?.nodeType === 'field' || el.dataset?.nodeType === 'function') {
      ensureCaretAroundAtomic(el);
    }
  }
}

/**
 * 将 atomic 节点插入到选区中，并确保其两侧有可编辑的文本节点
 * @param root 编辑器根节点
 * @param atomic atomic 节点
 * @returns void
 */
export function insertRangeAtomic(root: HTMLElement, atomic: HTMLElement) {
  const sel = window.getSelection();
  if (!sel || sel.rangeCount === 0) return;

  const range = sel.getRangeAt(0);

  // 保证 range 在 editor 内
  if (!root.contains(range.commonAncestorContainer)) {
    const tail = createCaretText();
    root.appendChild(tail);
    range.setStart(tail, 0);
    range.collapse(true);
  }

  // 删除选区
  range.deleteContents();

  // atomic 两侧必须是普通 text node
  const leftText = createCaretText();
  const rightText = createCaretText();

  const frag = document.createDocumentFragment();
  frag.append(leftText, atomic, rightText);

  range.insertNode(frag);

  // 光标放在 atomic 后
  range.setStart(rightText, 0);
  range.collapse(true);

  sel.removeAllRanges();
  sel.addRange(range);

  normalizeEditorStructure(root);
}

/**
 * 判断节点是否为 atomic 节点
 * @param node 节点
 * @returns 是否为 atomic 节点
 */
export function isAtomicNode(node: Node | null): node is HTMLElement {
  return node instanceof HTMLElement && node.contentEditable === 'false' && node.dataset.nodeType != null;
}

/**
 * 确保节点两侧有可编辑的文本节点（光标占位）
 * @param node 节点
 * @returns void
 */
export function ensureTextAround(node: Node) {
  const parent = node.parentNode;
  if (!parent) return;

  if (!node.previousSibling) {
    parent.insertBefore(createCaretText(), node);
  }

  if (!node.nextSibling) {
    parent.insertBefore(createCaretText(), node.nextSibling);
  }
}

/**
 * 删除 atomic 节点，并将光标放在其原位置
 * @param atomic atomic 节点
 * @returns void
 */
export function deleteAtomicNode(atomic: HTMLElement) {
  const parent = atomic.parentNode;
  if (!parent) return;

  ensureTextAround(atomic);

  const leftText = atomic.previousSibling as Text;
  atomic.remove();

  const range = document.createRange();
  range.setStart(leftText, leftText.length);
  range.setEnd(leftText, leftText.length);

  const sel = window.getSelection();
  sel?.removeAllRanges();
  sel?.addRange(range);
}

/**
 *  查找容器左侧最近的 atomic 节点
 * @param container 容器
 * @param offset 偏移位置
 * @returns atomic 节点或 null
 */
export function findLeftAtomicDeep(container: Node, offset: number): HTMLElement | null {
  let node: Node | null = null;

  //  在 text node 中
  if (container.nodeType === Node.TEXT_NODE) {
    if (offset === 0) {
      node = container.previousSibling;
    } else {
      return null;
    }
  }

  //  在 element 中
  if (container.nodeType === Node.ELEMENT_NODE) {
    node = (container as Element).childNodes[offset - 1] ?? null;
  }

  // 跳过空 text node
  while (node && node.nodeType === Node.TEXT_NODE && node.textContent === '') {
    node = node.previousSibling;
  }

  //  命中 atomic
  if (isAtomicNode(node)) {
    return node;
  }

  // 命中识别函数结构
  if (node?.nodeType === Node.TEXT_NODE && node.textContent === ')') {
    const maybeArgs = node.previousSibling;
    const maybeFnLeftParen = maybeArgs?.previousSibling;
    const maybeFn = maybeFnLeftParen?.previousSibling;

    if (
      maybeArgs instanceof HTMLElement &&
      maybeArgs.classList.contains('formula-args') &&
      maybeFn instanceof HTMLElement &&
      maybeFn.dataset.nodeType === 'function'
    ) {
      return maybeFn;
    }
  }

  return null;
}
