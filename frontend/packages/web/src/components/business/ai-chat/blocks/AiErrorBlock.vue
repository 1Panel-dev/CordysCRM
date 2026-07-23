<template>
  <div class="ai-chat-block ai-chat-block-error">
    {{ errorMessage }}
  </div>
</template>

<script setup lang="ts">
  import { computed } from 'vue';

  import type { AiChatDataParts, AiChatError } from '../types';
  import type { DataUIPart } from 'ai';

  const props = defineProps<{
    part: DataUIPart<AiChatDataParts>;
  }>();

  function isAiChatError(data: unknown): data is AiChatError {
    return Boolean(data && typeof data === 'object' && 'message' in data);
  }

  const errorMessage = computed(() => (isAiChatError(props.part.data) ? props.part.data.message : ''));
</script>

<style scoped lang="scss">
  .ai-chat-block-error {
    white-space: pre-wrap;
    color: var(--error-red);
  }
</style>
