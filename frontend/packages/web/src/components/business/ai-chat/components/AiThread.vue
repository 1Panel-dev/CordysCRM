<template>
  <n-scrollbar ref="threadScrollbarRef" class="h-full" content-class="min-h-full flex" @scroll="updateStickToBottom">
    <div
      ref="threadContentRef"
      class="w-full p-[24px]"
      :class="{ 'flex items-center justify-center': messages.length === 0 }"
    >
      <div v-if="messages.length === 0">
        <slot name="empty">
          <n-empty :description="props.emptyText" />
        </slot>
      </div>

      <template v-else>
        <AiMessage v-for="message in messages" :key="message.id" :message="message" />
      </template>
    </div>
  </n-scrollbar>
</template>

<script setup lang="ts">
  import { computed, nextTick, onMounted, ref, watch } from 'vue';
  import { NEmpty, NScrollbar, ScrollbarInst } from 'naive-ui';

  import AiMessage from './AiMessage.vue';

  import { useAiChatRuntime } from '../runtime/useAiChatRuntime';

  const props = withDefaults(
    defineProps<{
      /**
       * 是否在用户处于底部附近时跟随新消息滚动。
       * 用户主动往上看历史时不会被强行拉回底部。
       */
      autoScroll?: boolean;
      /**
       * 是否在组件首次挂载后滚到底部。
       * 历史会话通常不建议默认滚动，新会话或客服场景可显式开启。
       */
      initialScrollToBottom?: boolean;
      emptyText?: string;
    }>(),
    {
      autoScroll: true,
      initialScrollToBottom: false,
      emptyText: '',
    }
  );

  const runtime = useAiChatRuntime();
  const threadScrollbarRef = ref<ScrollbarInst>();
  const threadContentRef = ref<HTMLElement | null>(null);
  const scrollContainerRef = ref<HTMLElement | null>(null);
  const shouldStickToBottom = ref(true);

  const messages = computed(() => runtime.state.messages.value);
  const latestMessageSnapshot = computed(() => {
    const latestMessage = messages.value[messages.value.length - 1];

    if (!latestMessage) {
      return '';
    }

    return [
      latestMessage.id,
      latestMessage.parts.length,
      latestMessage.parts.map((part, index) => `${index}:${part.type}:${JSON.stringify(part).length}`).join('|'),
    ].join(':');
  });

  /**
   * 判断当前滚动位置是否接近底部。
   * 只有接近底部时才自动跟随新消息，避免用户查看历史时被打断。
   */
  function isNearBottom(container = scrollContainerRef.value): boolean {
    if (!container) {
      return true;
    }

    const threshold = 48;
    const distance = container.scrollHeight - container.scrollTop - container.clientHeight;

    return distance <= threshold;
  }

  function updateStickToBottom(event?: Event): void {
    if (event?.target instanceof HTMLElement) {
      scrollContainerRef.value = event.target;
    }

    shouldStickToBottom.value = isNearBottom();
  }

  async function scrollToBottom(): Promise<void> {
    await nextTick();

    if (!threadContentRef.value) {
      return;
    }

    threadScrollbarRef.value?.scrollTo({
      top: threadContentRef.value.scrollHeight,
    });
    shouldStickToBottom.value = true;
  }

  onMounted(async () => {
    if (props.initialScrollToBottom) {
      await scrollToBottom();
      return;
    }

    await nextTick();
    updateStickToBottom();
  });

  watch(
    () => [messages.value.length, latestMessageSnapshot.value],
    () => {
      if (!props.autoScroll || !shouldStickToBottom.value) {
        return;
      }

      scrollToBottom();
    }
  );
</script>
