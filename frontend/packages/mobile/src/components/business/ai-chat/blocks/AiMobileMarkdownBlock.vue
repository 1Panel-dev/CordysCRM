<template>
  <div
    v-if="part.text"
    class="ai-chat-block ai-mobile-markdown"
    :class="{ 'ai-mobile-markdown--thinking': isThinkingBlock }"
  >
    <van-collapse v-if="isThinkingBlock" v-model="activeNames" :border="false">
      <van-collapse-item :name="partId" :title="t('aiChat.thinking')">
        <div @click="handleMarkdownClick" v-html="html" />
      </van-collapse-item>
    </van-collapse>
    <div v-else @click="handleMarkdownClick" v-html="html" />
  </div>
</template>

<script setup lang="ts">
  import { computed, ref, watch } from 'vue';
  import { showToast } from 'vant';

  import { renderMarkdown } from '@lib/shared/ai-chat';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import type { ReasoningUIPart, TextUIPart } from 'ai';

  const props = defineProps<{
    part: TextUIPart | ReasoningUIPart;
    index?: number;
    isGenerating?: boolean;
  }>();

  const { t } = useI18n();
  const partId = computed(() => `${props.part.type}_${props.index ?? 0}`);
  const isThinkingBlock = computed(() => props.part.type === 'reasoning');
  const activeNames = ref<string[]>(props.isGenerating ? [partId.value] : []);
  const html = computed(() =>
    renderMarkdown(props.part.text, {
      copyText: t('common.copy'),
    })
  );

  async function handleMarkdownClick(event: MouseEvent): Promise<void> {
    if (!(event.target instanceof HTMLElement)) {
      return;
    }

    const copyButton = event.target.closest('[data-ai-code-copy]');

    if (!(copyButton instanceof HTMLElement)) {
      return;
    }

    const code = copyButton.parentElement?.nextElementSibling?.textContent;

    if (!code) {
      return;
    }

    await navigator.clipboard?.writeText(code);
    showToast(t('common.copySuccess'));
  }

  watch(
    () => partId.value,
    (id) => {
      activeNames.value = props.isGenerating ? [id] : [];
    }
  );

  watch(
    () => props.isGenerating,
    (isGenerating) => {
      activeNames.value = isGenerating ? [partId.value] : [];
    }
  );
</script>

<style scoped lang="less">
  .ai-mobile-markdown {
    overflow: hidden;
    width: 100%;
    font-size: 14px;
    color: var(--text-n1);
    line-height: 1.65;
    word-break: break-word;
    :deep(p),
    :deep(blockquote),
    :deep(hr),
    :deep(ol),
    :deep(pre),
    :deep(table),
    :deep(ul),
    :deep(.ai-code-block) {
      margin: 0 0 10px;
    }
    :deep(p:last-child),
    :deep(ol:last-child),
    :deep(ul:last-child),
    :deep(pre:last-child),
    :deep(table:last-child) {
      margin-bottom: 0;
    }
    :deep(h1),
    :deep(h2),
    :deep(h3),
    :deep(h4),
    :deep(h5),
    :deep(h6) {
      margin: 14px 0 8px;
      font-weight: 600;
      color: var(--text-n1);
      line-height: 1.35;
    }
    :deep(h1) {
      font-size: 20px;
    }
    :deep(h2) {
      font-size: 18px;
    }
    :deep(h3) {
      font-size: 16px;
    }
    :deep(h4),
    :deep(h5),
    :deep(h6) {
      font-size: 14px;
    }
    :deep(ul),
    :deep(ol) {
      padding-left: 20px;
    }
    :deep(ul) {
      list-style: disc;
    }
    :deep(ol) {
      list-style: decimal;
    }
    :deep(blockquote) {
      padding: 8px 10px;
      border-left: 3px solid var(--primary-8);
      color: var(--text-n3);
      background: var(--text-n9);
    }
    :deep(a) {
      text-decoration: none;
      color: var(--primary-8);
      word-break: break-all;
    }
    :deep(table) {
      display: block;
      overflow-x: auto;
      width: 100%;
      white-space: nowrap;
      border-spacing: 0;
      border-collapse: collapse;
    }
    :deep(th),
    :deep(td) {
      padding: 6px 8px;
      border: 1px solid var(--text-n8);
    }
    :deep(th) {
      font-weight: 600;
      background: var(--text-n9);
    }
    :deep(code:not(.hljs)) {
      padding: 1px 4px;
      border-radius: 3px;
      color: var(--primary-8);
      background: var(--primary-7);
    }
    :deep(.ai-code-block) {
      overflow: hidden;
      border: 1px solid var(--text-n8);
      border-radius: 6px;
      background: #f6f8fa;
    }
    :deep(.ai-code-block__header) {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 6px 8px;
      font-size: 12px;
      color: var(--text-n4);
      background: #eef1f4;
    }
    :deep(.ai-code-block__copy) {
      padding: 0;
      border: 0;
      color: var(--primary-8);
      background: transparent;
    }
    :deep(.ai-code-block__body) {
      display: block;
      overflow-x: auto;
      padding: 10px;
      max-width: 100%;
      font-size: 12px;
      white-space: pre;
      line-height: 1.55;
    }
    :deep(.katex-display) {
      overflow-x: auto;
      overflow-y: hidden;
    }
  }
  .ai-mobile-markdown--thinking {
    color: var(--text-n2);
    :deep(.van-collapse) {
      width: 100%;
    }
    :deep(.van-cell) {
      padding: 0;
      color: var(--text-n2);
      background: transparent;
    }
    :deep(.van-cell::after) {
      display: none;
    }
    :deep(.van-cell__title) {
      margin-right: 4px;
      max-width: calc(100% - 20px);
      flex: none;
    }
    :deep(.van-collapse-item__content) {
      padding-right: 0;
      padding-left: 0;
      color: var(--text-n2);
    }
  }
</style>
