<template>
  <div class="ai-chat-block ai-mobile-text-block">
    <template v-for="segment in textSegments" :key="segment.key">
      <span v-if="segment.type === 'text'">{{ segment.text }}</span>
      <span v-else class="ai-mobile-mcp-token">
        <CrmIcon name="iconicon_mcp" width="16px" height="16px" color="var(--primary-8)" />
        <span class="min-w-0 truncate">{{ segment.mcp.name }}</span>
      </span>
    </template>
  </div>
</template>

<script setup lang="ts">
  import { computed } from 'vue';

  import type { AiChatMcp } from '@lib/shared/ai-chat';
  import { getMatchedMcp } from '@lib/shared/ai-chat';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  import type { TextUIPart } from 'ai';

  type TextSegment =
    | {
        key: string;
        type: 'text';
        text: string;
      }
    | {
        key: string;
        type: 'mcp';
        mcp: AiChatMcp;
      };

  const props = defineProps<{
    part: TextUIPart;
    mcps?: AiChatMcp[];
  }>();

  const sortedMcps = computed(() => [...(props.mcps ?? [])].sort((a, b) => b.name.length - a.name.length));

  const textSegments = computed<TextSegment[]>(() => {
    const segments: TextSegment[] = [];
    let currentText = '';
    let index = 0;

    while (index < props.part.text.length) {
      const matchedMcp = getMatchedMcp(props.part.text, index, sortedMcps.value);

      if (matchedMcp) {
        if (currentText) {
          segments.push({
            key: `text_${segments.length}`,
            type: 'text',
            text: currentText,
          });
          currentText = '';
        }

        segments.push({
          key: `mcp_${matchedMcp.mcp.id}_${segments.length}`,
          type: 'mcp',
          mcp: matchedMcp.mcp,
        });
        index += matchedMcp.length;
        continue;
      }

      currentText += props.part.text[index];
      index += 1;
    }

    if (currentText) {
      segments.push({
        key: `text_${segments.length}`,
        type: 'text',
        text: currentText,
      });
    }

    return segments;
  });
</script>

<style scoped lang="less">
  .ai-mobile-text-block {
    width: 100%;
    font-size: 14px;
    white-space: pre-wrap;
    color: var(--text-n1);
    word-break: break-word;
  }
  .ai-mobile-mcp-token {
    display: inline-flex;
    align-items: center;
    margin: 0 2px;
    padding: 0 6px;
    max-width: 100%;
    border-radius: 4px;
    color: var(--primary-8);
    background: var(--primary-7);
    gap: 4px;
    vertical-align: text-bottom;
  }
</style>
