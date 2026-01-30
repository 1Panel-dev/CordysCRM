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
