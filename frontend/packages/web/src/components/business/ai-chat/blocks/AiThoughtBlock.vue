<template>
  <div class="ai-chat-block ai-chat-thought text-[var(--text-n2)]">
    <n-collapse v-model:expanded-names="expandedNames" arrow-placement="right">
      <n-collapse-item :name="blockId">
        <template #header>
          <div class="inline-flex min-w-0 max-w-full items-center">
            <span class="min-w-0 flex-1">{{ titleText }}</span>
          </div>
        </template>

        <div>
          <template v-for="item in items" :key="item.key">
            <AiMarkdownBlock
              v-if="item.part.type === 'reasoning'"
              :part="item.part"
              :index="item.index"
              :is-generating="isGenerating"
            />
            <AiProgressBlock
              v-else-if="item.part.type === 'data-progress'"
              :part="item.part"
              :index="item.index"
              :is-generating="isGenerating"
            />
          </template>
        </div>
      </n-collapse-item>
    </n-collapse>
  </div>
</template>

<script setup lang="ts">
  import { computed, ref, watch } from 'vue';
  import { NCollapse, NCollapseItem } from 'naive-ui';

  import type { AiChatMessagePart } from '@lib/shared/ai-chat';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import AiMarkdownBlock from './AiMarkdownBlock.vue';
  import AiProgressBlock from './AiProgressBlock.vue';

  interface AiThoughtItem {
    key: string;
    part: AiChatMessagePart;
    index: number;
  }

  const props = defineProps<{
    items: AiThoughtItem[];
    messageId: string;
    isGenerating?: boolean;
    duration?: number;
  }>();

  const { t } = useI18n();
  const blockId = computed(() => `thought_${props.messageId}`);
  const expandedNames = ref<string[]>(props.isGenerating ? [blockId.value] : []);

  function formatDuration(duration?: number): string {
    if (typeof duration !== 'number' || Number.isNaN(duration) || duration < 0) {
      return '';
    }

    const totalSeconds = duration / 1000;

    if (totalSeconds < 60) {
      return `${Number(totalSeconds.toFixed(totalSeconds < 10 ? 1 : 0))}s`;
    }

    const minutes = Math.floor(totalSeconds / 60);
    const seconds = Math.round(totalSeconds % 60)
      .toString()
      .padStart(2, '0');

    return `${minutes}:${seconds}`;
  }

  const durationText = computed(() => formatDuration(props.duration));
  const titleText = computed(() => {
    if (props.isGenerating) {
      return t('aiChat.thinkingInProgress');
    }

    return [t('common.completed'), durationText.value].filter(Boolean).join(' ');
  });

  watch(
    () => blockId.value,
    (id) => {
      expandedNames.value = props.isGenerating ? [id] : [];
    }
  );

  watch(
    () => props.isGenerating,
    (isGenerating) => {
      expandedNames.value = isGenerating ? [blockId.value] : [];
    }
  );
</script>

<style scoped lang="less">
  .ai-chat-thought {
    width: 100%;
    :deep(.n-collapse) {
      width: 100%;
    }
    :deep(.n-collapse-item__header) {
      min-width: 0;
    }
    :deep(.n-collapse-item__header .n-collapse-item__header-main) {
      overflow: hidden;
      min-width: 0;
      max-width: 100%;
      color: var(--text-n2);
    }
    :deep(.n-collapse-item__content-wrapper .n-collapse-item__content-inner) {
      padding-top: 8px;
      padding-left: 0;
      color: var(--text-n2);
    }
  }
</style>
