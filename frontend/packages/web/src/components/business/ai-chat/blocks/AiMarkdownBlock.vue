<template>
  <div
    v-if="props.part.text"
    ref="markdownRef"
    class="ai-chat-block ai-chat-block-markdown"
    :class="{ 'ai-chat-block-markdown--thinking': isThinkingBlock }"
    @click="handleMarkdownClick"
  >
    <template v-if="isThinkingBlock">
      <n-collapse v-model:expanded-names="expandedNames" arrow-placement="right">
        <n-collapse-item :title="t('aiChat.thinking')" :name="partId">
          <div v-html="html" />
        </n-collapse-item>
      </n-collapse>
    </template>

    <div v-else v-html="html" />
  </div>
</template>

<script setup lang="ts">
  import { computed, nextTick, onMounted, ref, watch } from 'vue';
  import { NCollapse, NCollapseItem } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import useLegacyCopy from '@/hooks/useLegacyCopy';

  import renderMarkdown from '../utils/markdown';
  import type { ReasoningUIPart, TextUIPart } from 'ai';
  import DOMPurify from 'dompurify';
  import mermaid from 'mermaid';

  // 避免每个 markdown block 都重复 initialize
  const mermaidState = mermaid as typeof mermaid & {
    __aiChatInitialized?: boolean;
  };

  function initializeMermaid(): void {
    if (mermaidState.__aiChatInitialized) {
      return;
    }

    mermaid.initialize({
      startOnLoad: false,
      securityLevel: 'strict',
      theme: 'default',
    });
    mermaidState.__aiChatInitialized = true;
  }

  const props = defineProps<{
    part: TextUIPart | ReasoningUIPart;
    index?: number;
  }>();

  const { t } = useI18n();
  const { legacyCopy } = useLegacyCopy();
  const partId = computed(() => `${props.part.type}_${props.index ?? 0}`);
  const markdownRef = ref<HTMLElement | null>(null);
  const expandedNames = ref<string[]>([partId.value]);
  const isThinkingBlock = computed(() => props.part.type === 'reasoning');
  const mermaidIdPrefix = `ai-mermaid-${Math.random().toString(36).slice(2)}`;

  initializeMermaid();

  watch(
    () => partId.value,
    (id) => {
      expandedNames.value = [id];
    }
  );

  const html = computed(() =>
    renderMarkdown(props.part.text, {
      copyText: t('common.copy'),
    })
  );

  async function renderMermaid(): Promise<void> {
    await nextTick();

    const markdownElement = markdownRef.value;
    if (!markdownElement) {
      return;
    }

    const mermaidBlocks = markdownElement.querySelectorAll<HTMLElement>('[data-ai-mermaid]');

    mermaidBlocks.forEach((block, index) => {
      const source = block.querySelector<HTMLElement>('.ai-mermaid__source')?.textContent?.trim();
      const renderTarget = block.querySelector<HTMLElement>('.ai-mermaid__render');

      if (!source || !renderTarget || block.dataset.rendered === source) {
        return;
      }

      mermaid
        .render(`${mermaidIdPrefix}-${index}`, source)
        .then(({ svg }) => {
          renderTarget.innerHTML = DOMPurify.sanitize(svg, {
            USE_PROFILES: { svg: true, svgFilters: true },
          });
          block.dataset.rendered = source;
          block.classList.remove('ai-mermaid--error');
        })
        .catch(() => {
          block.classList.add('ai-mermaid--error');
        });
    });
  }

  async function handleMarkdownClick(event: MouseEvent): Promise<void> {
    if (!(event.target instanceof HTMLElement)) {
      return;
    }

    const copyButton = event.target.closest('[data-ai-code-copy]');

    if (!(copyButton instanceof HTMLElement)) {
      return;
    }

    const code = copyButton.parentElement?.nextElementSibling?.textContent;

    if (code) {
      await legacyCopy(code);
    }
  }

  onMounted(renderMermaid);

  watch(
    () => html.value,
    () => {
      renderMermaid().catch(() => undefined);
    },
    { flush: 'post' }
  );
</script>

<style scoped lang="scss">
  .ai-chat-block-markdown {
    // 基础块级排版
    :deep(p),
    :deep(blockquote),
    :deep(hr),
    :deep(ol),
    :deep(pre),
    :deep(table),
    :deep(ul),
    :deep(.ai-code-block),
    :deep(.ai-mermaid) {
      margin: 0 0 12px;
    }

    // 标题
    :deep(h1),
    :deep(h2),
    :deep(h3),
    :deep(h4),
    :deep(h5),
    :deep(h6) {
      margin: 18px 0 10px;
      font-weight: 600;
      color: var(--text-n1);
      line-height: 1.35;
    }
    :deep(h1) {
      padding-bottom: 8px;
      font-size: 24px;
      border-bottom: 1px solid var(--text-n8);
    }
    :deep(h2) {
      padding-bottom: 6px;
      font-size: 20px;
      border-bottom: 1px solid var(--text-n8);
    }
    :deep(h3) {
      font-size: 18px;
    }
    :deep(h4) {
      font-size: 16px;
    }
    :deep(h5) {
      font-size: 14px;
    }
    :deep(h6) {
      font-size: 14px;
      color: var(--text-n3);
    }

    // 列表
    :deep(ul),
    :deep(ol) {
      padding-left: 24px;
    }
    :deep(ul) {
      list-style: disc;
    }
    :deep(ol) {
      list-style: decimal;
    }
    :deep(ul ul),
    :deep(ol ul) {
      list-style: circle;
    }
    :deep(ol ol),
    :deep(ul ol) {
      list-style: lower-alpha;
    }
    :deep(li + li) {
      margin-top: 4px;
    }
    :deep(li > p) {
      margin: 0;
    }

    // 行内元素
    :deep(a) {
      text-decoration: none;
      color: var(--primary-8);
    }
    :deep(a:hover) {
      text-decoration: underline;
    }
    :deep(strong) {
      font-weight: 600;
    }
    :deep(del) {
      color: var(--text-n3);
    }
    :deep(img) {
      max-width: 100%;
      border-radius: 4px;
    }

    // 代码
    :deep(code) {
      padding: 2px 4px;
      font-size: 12px;
      border-radius: 4px;
      background: var(--text-n8);
    }
    :deep(pre) {
      overflow: auto;
      border-radius: 6px;
      background: var(--text-n9);
    }
    :deep(pre code) {
      padding: 0;
      background: transparent;
    }
    :deep(.ai-code-block) {
      padding: 0;
      border-radius: 6px;
      background: var(--text-n9);
    }
    :deep(.ai-code-block__header) {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 6px 10px;
      font-size: 12px;
      border-bottom: 1px solid var(--text-n8);
      color: var(--text-n4);
      gap: 8px;
    }
    :deep(.ai-code-block__lang) {
      overflow: hidden;
      min-width: 0;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    :deep(.ai-code-block__lang:empty::before) {
      content: 'text';
    }
    :deep(.ai-code-block__copy) {
      padding: 0;
      font-size: 12px;
      border: 0;
      color: var(--primary-8);
      background: transparent;
      flex: none;
      cursor: pointer;
    }
    :deep(.ai-code-block__body) {
      display: block;
      overflow: auto;
      padding: 10px 12px;
      background: transparent;
    }

    // 公式和图表
    :deep(.katex-display) {
      overflow-x: auto;
      overflow-y: hidden;
      padding: 2px 0;
    }
    :deep(.ai-mermaid) {
      overflow: auto;
      padding: 12px;
      border: 1px solid var(--text-n8);
      border-radius: 6px;
      background: var(--text-n10);
    }
    :deep(.ai-mermaid__source) {
      display: none;
    }
    :deep(.ai-mermaid__render) {
      min-width: 240px;
      text-align: center;
    }
    :deep(.ai-mermaid__render svg) {
      max-width: 100%;
      height: auto;
    }
    :deep(.ai-mermaid--error .ai-mermaid__source) {
      display: block;
      margin: 0;
      padding: 0;
      white-space: pre-wrap;
      color: var(--error-red);
      background: transparent;
    }

    // 引用、分割线、表格
    :deep(blockquote) {
      padding: 0 0 0 12px;
      border-left: 4px solid var(--text-n8);
      color: var(--text-n3);
    }
    :deep(hr) {
      height: 1px;
      border: 0;
      background: var(--text-n8);
    }
    :deep(table) {
      width: 100%;
      border-collapse: collapse;
      font-size: 14px;
    }
    :deep(th),
    :deep(td) {
      padding: 8px 10px;
      border: 1px solid var(--text-n8);
      text-align: left;
      color: var(--text-n2);
    }
    :deep(th) {
      font-weight: 600;
      color: var(--text-n1);
      background: var(--text-n9);
    }
    :deep(tr:nth-child(even) td) {
      background: var(--text-n9);
    }
  }
  .ai-chat-block-markdown--thinking {
    width: 100%;
    color: var(--text-n2);
    :deep(.n-collapse) {
      width: 100%;
    }
    :deep(.n-collapse-item__header .n-collapse-item__header-main) {
      color: var(--text-n2);
    }
    :deep(.n-collapse-item__content-wrapper .n-collapse-item__content-inner) {
      padding-top: 8px;
      color: var(--text-n2);
    }
    :deep(.n-collapse-item__content-wrapper .n-collapse-item__content-inner *) {
      color: inherit;
    }
  }
</style>
