<template>
  <div class="relative h-full">
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
          <AiMessage
            v-for="message in messages"
            :key="message.id"
            :message="message"
            :is-generating="runtime.state.loading.value && message.id === latestMessageId"
          />
          <article v-if="showThreadLoading" class="mb-[32px] flex gap-[16px]">
            <n-avatar round class="bg-[var(--primary-6)]" :size="32">
              <CrmIcon type="iconicon_bot" :size="20" color="var(--primary-8)" />
            </n-avatar>

            <div class="w-full min-w-0">
              <div class="mb-[8px] font-[600]"> CORDYS AI </div>
              <AiLoadingBlock />
            </div>
          </article>
        </template>
      </div>
    </n-scrollbar>

    <n-button
      v-if="showBackToBottom"
      circle
      class="base-box-shadow absolute bottom-[16px] right-[24px] z-[1]"
      @click="scrollToBottom"
    >
      <template #icon>
        <CrmIcon type="iconicon_arrow_down" :size="16" />
      </template>
    </n-button>
  </div>
</template>

<script setup lang="ts">
  import { computed, nextTick, onMounted, ref, watch } from 'vue';
  import { NAvatar, NButton, NEmpty, NScrollbar, ScrollbarInst } from 'naive-ui';

  import { useAiChatRuntime } from '@lib/shared/ai-chat';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import AiLoadingBlock from '../blocks/AiLoadingBlock.vue';
  import AiMessage from './AiMessage.vue';

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
  const latestMessageId = computed(() => messages.value.at(-1)?.id);
  const showThreadLoading = computed(() => {
    const lastMessage = messages.value.at(-1);

    return runtime.state.loading.value && Boolean(lastMessage) && lastMessage?.role !== 'assistant';
  });
  const showBackToBottom = computed(() => messages.value.length > 0 && !shouldStickToBottom.value);

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

  watch(showThreadLoading, () => {
    if (!props.autoScroll || !shouldStickToBottom.value) {
      return;
    }

    scrollToBottom();
  });
</script>
