<template>
  <div class="ai-chat-block ai-chat-block-text">
    {{ props.part.text }}
  </div>
</template>

<script setup lang="ts">
  import type { TextUIPart } from 'ai';

  const props = defineProps<{
    part: TextUIPart;
  }>();
</script>

<style scoped lang="scss">
  .ai-chat-block-text {
    white-space: pre-wrap;
    word-break: break-word;
  }
</style>
