// editorDom->token
import { NumberType, Token } from './types';

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
    } else if (node.nodeType === Node.TEXT_NODE) {
      const raw = node.textContent || '';
      const text = raw.replace(/[\u200B-\u200D\uFEFF]/g, '');
      if (!text) {
        return;
      }

      let i = 0;

      while (i < text.length) {
        const char = text[i];

        // ===== 空白 =====
        if (isIgnorableWhitespace(char)) {
          charIndex++;
          i++;
        }

        // ===== 字符串 =====
        else if (char === '"') {
          const start = charIndex;
          let value = '';
          let j = i + 1;
          let closed = false;

          while (j < text.length) {
            const cur = text[j];
            if (cur === '"') {
              closed = true;
              break;
            }
            value += cur;
            j++;
          }

          if (closed) {
            tokens.push({
              type: 'string',
              value,
              start,
              end: start + (j - i) + 1,
            });

            charIndex += j - i + 1;
            i = j + 1;
          } else {
            // 未闭合字符串
            tokens.push({
              type: 'unknown',
              value: text.slice(i),
              start,
              end: start + (text.length - i),
            });

            charIndex += text.length - i;
            i = text.length;
          }
        }

        // ===== 数字（支持小数）=====
        else if (/\d/.test(char) || (char === '.' && /\d/.test(text[i + 1]))) {
          const start = charIndex;

          let numStr = '';
          let j = i;
          let hasDot = false;
          let done = false;

          while (j < text.length && !done) {
            const cur = text[j];

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
            numberType: 'number',
            start,
            end: start + numStr.length,
          });

          charIndex += numStr.length;
          i = j;
        }
        // ===== boolean / 标识符 =====
        else if (/[A-Za-z]/.test(char)) {
          const start = charIndex;
          let word = char;
          let j = i + 1;

          while (j < text.length && /[A-Za-z]/.test(text[j])) {
            word += text[j];
            j++;
          }

          const upper = word.toUpperCase();

          if (upper === 'TRUE' || upper === 'FALSE') {
            tokens.push({
              type: 'boolean',
              value: upper === 'TRUE',
              start,
              end: start + word.length,
            });
          } else {
            // 不是支持的关键字，就按 unknown 整段报（比逐字符 unknown 更好）
            tokens.push({
              type: 'unknown',
              value: word,
              start,
              end: start + word.length,
            });
          }

          charIndex += word.length;
          i = j;
        }

        // ===== 比较运算符 =====
        else if (char === '=' || char === '<' || char === '>') {
          const start = charIndex;
          const next = text[i + 1];

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
            start,
            end: start + length,
          });

          charIndex += length;
          i += length;
        }

        // ===== 四则运算符 =====
        else if (['+', '-', '*', '/'].includes(char)) {
          tokens.push({
            type: 'operator',
            value: char as any,
            start: charIndex,
            end: charIndex + 1,
          });

          charIndex++;
          i++;
        }

        // ===== 括号 =====
        else if (char === '(' || char === ')') {
          tokens.push({
            type: 'paren',
            value: char,
            start: charIndex,
            end: charIndex + 1,
          });

          charIndex++;
          i++;
        }

        // ===== 逗号 =====
        else if (char === ',' || char === '，') {
          tokens.push({
            type: 'comma',
            value: char,
            start: charIndex,
            end: charIndex + 1,
          });

          charIndex++;
          i++;
        }

        // ===== 其他非法字符 =====
        else {
          tokens.push({
            type: 'unknown',
            value: char,
            start: charIndex,
            end: charIndex + 1,
          });

          charIndex++;
          i++;
        }
      }
    }
  });
  // todo xinxinwu debugger
  // console.log(tokens, 'tokens:tokenizeFromEditor');

  return tokens;
}
