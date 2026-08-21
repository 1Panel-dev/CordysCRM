<template>
  <section class="flex h-full w-full flex-col bg-[var(--text-n10)]">
    <div class="flex-1 overflow-hidden">
      <!-- 消息列表区域 -->
      <slot name="thread">
        <AiThread :scroll-to-bottom-key="props.scrollToBottomKey" />
      </slot>
    </div>

    <AiConfirmModal v-if="pendingConfirm" :confirm="pendingConfirm" />

    <!-- 输入区 -->
    <div>
      <slot name="composer">
        <AiComposer />
      </slot>
    </div>
  </section>
</template>

<script setup lang="ts">
  import { computed } from 'vue';

  import { useAiChatRuntime } from '@lib/shared/ai-chat';

  import AiComposer from './AiComposer.vue';
  import AiConfirmModal from './AiConfirmModal.vue';
  import AiThread from './AiThread.vue';

  const props = withDefaults(
    defineProps<{
      scrollToBottomKey?: string | number;
    }>(),
    {
      scrollToBottomKey: '',
    }
  );

  const runtime = useAiChatRuntime();
  const pendingConfirm = computed(() => runtime.state.pendingConfirm.value);
</script>
