// editorDom->token
import { NumberToken, NumberType, Token } from './types';

/**
 * 判断字符是否为可忽略的空白字符
 * @param char 字符
 * @returns 是否为可忽略的空白字符
 */
function isIgnorableWhitespace(char: string) {
  return (
    char === ' ' || // 普通空格
    char === '\n' ||
    char === '\t' ||
    char === '\u00A0' || // NBSP
    char === '\u200B' || // ZWSP
    char === '\uFEFF' // BOM
  );
}

/**
 * 将编辑器内容转换为令牌数组
 * @param editorEl 编辑器根元素
 * @param offset 偏移位置
 * @returns 令牌数组
 */
export default function tokenizeFromEditor(editorEl: HTMLElement, offset = 0): Token[] {
  const tokens: Token[] = [];
  let charIndex = offset;

  editorEl.childNodes.forEach((node) => {
    if (node.nodeType === Node.ELEMENT_NODE) {
      const el = node as HTMLElement;

      if (el.classList.contains('formula-fn-root')) {
        const fnNameEl = el.querySelector('.fn-name');
        const argsEl = el.querySelector('.fn-args');

        const fnName = fnNameEl?.textContent?.trim() || '';
        const start = charIndex;

        // function
        tokens.push({
          type: 'function',
          name: fnName,
          start,
          end: start + fnName.length,
        });
        charIndex += fnName.length;

        // (
        tokens.push({
          type: 'paren',
          value: '(',
          start: charIndex,
          end: charIndex + 1,
        });
        charIndex += 1;

        // args
        if (argsEl) {
          if (argsEl instanceof HTMLElement) {
            const innerTokens = tokenizeFromEditor(argsEl, charIndex);
            tokens.push(...innerTokens);
            if (innerTokens.length > 0) {
              charIndex = innerTokens[innerTokens.length - 1].end;
            }
          }
        }

        // )
        tokens.push({
          type: 'paren',
          value: ')',
          start: charIndex,
          end: charIndex + 1,
        });
        charIndex += 1;

        return;
      }

      if (el.classList.contains('formula-tag-wrapper') && el.dataset.nodeType === 'field') {
        const text = el.textContent || '';
        const start = charIndex;
        const end = start + text.length;

        tokens.push({
          type: 'field',
          fieldId: el.dataset.value || '',
          name: text,
          fieldType: el.dataset.fieldType,
          numberType: (el.dataset.numberType ?? 'number') as NumberType,
          start,
          end,
        });

        charIndex = end;
        return;
      }

      const innerTokens = tokenizeFromEditor(el, charIndex);
      tokens.push(...innerTokens);
      if (innerTokens.length > 0) {
        charIndex = innerTokens[innerTokens.length - 1].end;
      }
      return;
    }

    if (node.nodeType === Node.TEXT_NODE) {
      const raw = node.textContent || '';
      const text = raw.replace(/[\u200B-\u200D\uFEFF]/g, '');
      if (!text) return;

      Array.from(text).forEach((char) => {
        // 不校验空格
        if (isIgnorableWhitespace(char)) {
          charIndex++; // 遇到空格位置仍然前进
          return;
        }
        const start = charIndex;
        const end = start + 1;

        /** 数字 */
        if (/\d/.test(char)) {
          const last = tokens[tokens.length - 1];
          if (last && last.type === 'number') {
            (last as NumberToken).value = Number(`${(last as NumberToken).value}${char}`);
            last.end = end;
          } else {
            tokens.push({
              type: 'number',
              value: Number(char),
              numberType: 'number',
              start,
              end,
            });
          }
          charIndex = end;
        } else if (['+', '-', '*', '/'].includes(char)) {
          /** 操作符 */
          tokens.push({
            type: 'operator',
            value: char as any,
            start,
            end,
          });
          charIndex = end;
        } else if (char === '(' || char === ')') {
          /** 括号 */
          tokens.push({
            type: 'paren',
            value: char,
            start,
            end,
          });
          charIndex = end;
        } else if (char === ',' || char === '，') {
          /** 英文逗号 */
          tokens.push({
            type: 'comma',
            value: char,
            start,
            end,
          });
          charIndex = end;
        } else {
          /** 其他字符 */
          tokens.push({
            type: 'text',
            value: char,
            start,
            end,
          });
          charIndex = end;
        }
      });
    }
  });

  return tokens;
}
