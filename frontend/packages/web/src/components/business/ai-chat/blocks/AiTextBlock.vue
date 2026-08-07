<template>
  <div class="ai-chat-block ai-chat-block-text">
    <template v-for="segment in textSegments" :key="segment.key">
      <span v-if="segment.type === 'text'">{{ segment.text }}</span>
      <span v-else class="ai-chat-mcp-token">
        <CrmIcon type="iconicon_mcp" :size="16" />
        <span class="min-w-0 truncate">{{ segment.mcp.name }}</span>
      </span>
    </template>
  </div>
</template>

<script setup lang="ts">
  import { computed } from 'vue';

  import type { AiChatMcp } from '@lib/shared/ai-chat';

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

  function getMatchedMcp(text: string, index: number): AiChatMcp | undefined {
    return sortedMcps.value.find((mcp) => text.startsWith(mcp.name, index));
  }

  const textSegments = computed<TextSegment[]>(() => {
    const segments: TextSegment[] = [];
    let currentText = '';
    let index = 0;

    while (index < props.part.text.length) {
      const mcp = getMatchedMcp(props.part.text, index);

      if (mcp) {
        if (currentText) {
          segments.push({
            key: `text_${segments.length}`,
            type: 'text',
            text: currentText,
          });
          currentText = '';
        }

        segments.push({
          key: `mcp_${mcp.id}_${segments.length}`,
          type: 'mcp',
          mcp,
        });
        index += mcp.name.length;
      } else {
        currentText += props.part.text[index];
        index += 1;
      }
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

<style scoped lang="scss">
  .ai-chat-block-text {
    white-space: pre-wrap;
    word-break: break-word;
  }
</style>
