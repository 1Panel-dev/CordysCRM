<template>
  <!-- Provider 只建立上下文边界，不渲染额外 DOM，避免影响业务页面布局。 -->
  <slot />
</template>

<script setup lang="ts">
  import { provide } from 'vue';

  import type { AiChatRuntime } from '../runtime/types';
  import { AI_CHAT_RUNTIME_KEY } from '../runtime/useAiChatRuntime';

  const props = defineProps<{
    runtime: AiChatRuntime;
  }>();

  /**
   * 所有下层 AI Chat 组件都通过 useAiChatRuntime 获取同一个 Runtime。
   * 这样可以避免 Thread、Message、Composer 之间层层透传 props。
   */
  provide(AI_CHAT_RUNTIME_KEY, props.runtime);
</script>
