// editorDom->token
import { NumberToken, Token } from './types';

export default function tokenizeFromEditor(editorEl: HTMLElement, offset = 0): Token[] {
  const tokens: Token[] = [];
  let charIndex = offset;

  editorEl.childNodes.forEach((node) => {
    if (node.nodeType === Node.ELEMENT_NODE) {
      const el = node as HTMLElement;
      const text = el.textContent || '';
      const start = charIndex;
      const end = start + text.length;

      if (el.classList.contains('formula-fn') && el.dataset.nodeType === 'function') {
        tokens.push({
          type: 'function',
          name: text.trim(),
          start,
          end,
        });
        charIndex = end;
        return;
      }

      if (el.classList.contains('formula-tag-wrapper') && el.dataset.nodeType === 'field') {
        tokens.push({
          type: 'field',
          fieldId: el.dataset.value || '',
          name: text.trim(),
          fieldType: el.dataset.fieldType,
          start,
          end,
        });
        charIndex = end;
        return;
      }

      if (el.classList.contains('formula-args')) {
        const innerTokens = tokenizeFromEditor(el, charIndex);
        tokens.push(...innerTokens);
        charIndex = innerTokens.length ? innerTokens[innerTokens.length - 1].end : charIndex;
        return;
      }

      charIndex = end;
      return;
    }

    if (node.nodeType === Node.TEXT_NODE) {
      const rawText = node.textContent || '';
      const text = rawText.replace(/[\u200B-\u200D\uFEFF]/g, '');
      if (!text) return;

      Array.from(text).forEach((char) => {
        const start = charIndex;
        const end = start + 1;

        /** 数字 */
        if (/\d/.test(char)) {
          const last = tokens[tokens.length - 1];
          if (last?.type === 'number') {
            (last as NumberToken).value = Number(`${(last as NumberToken).value}${char}`);
            last.end = end;
          } else {
            tokens.push({
              type: 'number',
              value: Number(char),
              start,
              end,
            });
          }
          charIndex++;
          return;
        }

        /** 操作符 */
        if (['+', '-', '*', '/'].includes(char)) {
          tokens.push({
            type: 'operator',
            value: char as any,
            start,
            end,
          });
          charIndex++;
          return;
        }

        /** 括号 */
        if (char === '(' || char === ')') {
          tokens.push({
            type: 'paren',
            value: char,
            start,
            end,
          });
          charIndex++;
          return;
        }

        /** 英文逗号 */
        if (char === ',') {
          tokens.push({
            type: 'comma',
            value: ',',
            start,
            end,
          });
          charIndex++;
          return;
        }

        /** 中文逗号（关键） */
        if (char === '，') {
          tokens.push({
            type: 'comma',
            value: '，',
            start,
            end,
          });
          charIndex++;
          return;
        }

        /** 其他字符 */
        tokens.push({
          type: 'text',
          value: char,
          start,
          end,
        });
        charIndex++;
      });
    }
  });

  return tokens;
}
