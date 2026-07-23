import 'highlight.js/styles/github.css';
import 'katex/dist/katex.min.css';
import MdKatex from '@vscode/markdown-it-katex';
import DOMPurify from 'dompurify';
import hljs from 'highlight.js';
import MarkdownIt from 'markdown-it';
import type { RenderRule } from 'markdown-it/lib/renderer.mjs';

interface RenderMarkdownOptions {
  copyText?: string;
}

const MERMAID_LANGUAGE = 'mermaid';
const { escapeHtml } = MarkdownIt().utils;

// Markdown fence 的 info 可能是 `ts {1,3}` 这种形式，这里只取真正的语言名。
function normalizeLanguage(language = ''): string {
  return language.trim().split(/\s+/)[0].toLowerCase();
}

// 兼容 AI 常见的 LaTeX 写法：把 \(...\) / \[...\] 转成 KaTeX 插件能识别的 $...$ / $$...$$。
// 代码块和行内代码里的内容不处理，避免误改示例代码。
function normalizeMathDelimiters(content: string): string {
  const pattern = /(```[\s\S]*?```|`[^`]*`)|\\\[([\s\S]*?[^\\])\\\]|\\\(([\s\S]*?)\\\)/g;

  return content.replace(pattern, (match, codeBlock, squareBracket, roundBracket) => {
    if (codeBlock) {
      return codeBlock;
    }

    if (squareBracket) {
      return `$$${squareBracket}$$`;
    }

    if (roundBracket) {
      return `$${roundBracket}$`;
    }

    return match;
  });
}

// highlight.js 只负责高亮片段；外层 pre/header/copy 按当前聊天 UI 的结构拼出来。
function renderCodeBlock(code: string, language?: string): string {
  const normalizedLanguage = normalizeLanguage(language);
  const lang = escapeHtml(normalizedLanguage);
  const highlightedCode =
    normalizedLanguage && hljs.getLanguage(normalizedLanguage)
      ? hljs.highlight(code, { language: normalizedLanguage }).value
      : hljs.highlightAuto(code).value;

  return [
    '<pre class="ai-code-block">',
    '<div class="ai-code-block__header">',
    `<span class="ai-code-block__lang">${lang}</span>`,
    '<button class="ai-code-block__copy" type="button" data-ai-code-copy></button>',
    '</div>',
    `<code class="hljs ai-code-block__body${lang ? ` language-${lang}` : ''}">${highlightedCode}</code>`,
    '</pre>',
  ].join('');
}

/**
 * AI 回复默认按 Markdown 渲染。
 * html 关闭，避免模型或用户输入直接注入原始 HTML。
 */
const markdown: MarkdownIt = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
  highlight(code, language): string {
    return renderCodeBlock(code, language);
  },
});

// 支持 $...$ / $$...$$ 公式渲染。
markdown.use(MdKatex);

const defaultLinkOpen =
  markdown.renderer.rules.link_open ??
  (((tokens, idx, options, _env, self) => self.renderToken(tokens, idx, options)) as RenderRule);

// 所有 Markdown 链接都在新窗口打开，并加 rel，避免新页面拿到 window.opener。
markdown.renderer.rules.link_open = (tokens, idx, options, env, self) => {
  const token = tokens[idx];

  token.attrSet('target', '_blank');
  token.attrSet('rel', 'noopener noreferrer');

  return defaultLinkOpen(tokens, idx, options, env, self);
};

const defaultFence =
  markdown.renderer.rules.fence ??
  (((tokens, idx, options, _env, self) => self.renderToken(tokens, idx, options)) as RenderRule);

// Mermaid 不能只靠 v-html 生效：这里先把 mermaid 代码块输出成带源码的占位 DOM，
// 后续由 AiMarkdownBlock.vue 在 mounted/updated 后调用 mermaid.render 转成 SVG。
markdown.renderer.rules.fence = (tokens, idx, options, env, self) => {
  const token = tokens[idx];
  const language = normalizeLanguage(token.info);

  if (language !== MERMAID_LANGUAGE) {
    return defaultFence(tokens, idx, options, env, self);
  }

  return [
    '<div class="ai-mermaid" data-ai-mermaid>',
    `<pre class="ai-mermaid__source">${escapeHtml(token.content)}</pre>`,
    '<div class="ai-mermaid__render"></div>',
    '</div>',
  ].join('');
};

/**
 * 把 Markdown 文本转换成安全 HTML。
 * MarkdownIt 负责语法解析，DOMPurify 负责最终 XSS 清洗。
 */
export default function renderMarkdown(content: string, options: RenderMarkdownOptions = {}): string {
  // KaTeX 会尝试识别 `$...$`，金额场景里的 `$100` 需要先转义掉。
  const normalizedContent = normalizeMathDelimiters(content.replace(/\$(?=\d)/g, '\\$'));

  // 复制按钮文字来自 i18n，但按钮 HTML 是 Markdown 渲染出来的，所以这里用占位按钮再统一替换文案。
  const html = markdown
    .render(normalizedContent)
    .replaceAll(
      '<button class="ai-code-block__copy" type="button" data-ai-code-copy></button>',
      `<button class="ai-code-block__copy" type="button" data-ai-code-copy>${escapeHtml(
        options.copyText || ''
      )}</button>`
    );

  return DOMPurify.sanitize(html, {
    // KaTeX 会输出部分 MathML 标签；DOMPurify 默认 HTML 白名单不一定完整覆盖，所以显式放行。
    ADD_TAGS: [
      'annotation',
      'math',
      'menclose',
      'mfenced',
      'mfrac',
      'mi',
      'mn',
      'mo',
      'mover',
      'mpadded',
      'mrow',
      'mspace',
      'msqrt',
      'msub',
      'msubsup',
      'msup',
      'mtable',
      'mtd',
      'mtext',
      'mtr',
      'munder',
      'munderover',
      'semantics',
    ],
    // data-* 给复制按钮和 Mermaid 占位用；target/rel 给链接用；其余几个给 KaTeX/SVG 兼容用。
    ADD_ATTR: [
      'aria-hidden',
      'checked',
      'data-ai-code-copy',
      'data-ai-mermaid',
      'disabled',
      'encoding',
      'focusable',
      'rel',
      'target',
      'type',
      'xmlns',
    ],
  });
}
