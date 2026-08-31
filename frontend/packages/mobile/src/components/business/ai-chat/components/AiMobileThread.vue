<template>
  <div class="relative h-full">
    <div ref="threadRef" class="h-full overflow-y-auto px-[12px] py-[14px]" @scroll="handleScroll">
      <div
        v-if="messages.length === 0"
        class="flex h-full flex-col items-center justify-center px-[24px] text-[var(--text-n1)]"
      >
        <div class="mb-[32px] text-[24px] font-[600]">
          {{ t('aiChat.emptyTitle') }}
        </div>
        <div class="grid w-full grid-cols-1 gap-[12px]">
          <div
            v-for="item in emptySuggestionList"
            :key="item.label"
            class="flex min-h-[58px] items-center gap-[8px] rounded-[4px] border border-solid border-[var(--text-n8)] bg-[var(--text-n10)] px-[16px] text-[16px] font-[600] active:bg-[var(--text-n9)]"
            @click="handleSuggestionClick(item.label)"
          >
            <CrmIcon :name="item.icon" width="24px" height="24px" class="shrink-0" />
            <span>{{ item.label }}</span>
          </div>
        </div>
      </div>

      <template v-else>
        <AiMobileMessage
          v-for="message in messages"
          :key="message.id"
          :message="message"
          :is-generating="runtime.state.loading.value && message.id === latestMessageId"
        />
        <div v-if="showThreadLoading" class="flex w-full items-start gap-[8px] [&+&]:mt-[16px]">
          <CrmIcon
            name="iconicon_crmbot"
            width="32px"
            height="32px"
            color="linear-gradient(180deg, #00A6AB 0%, #3370FF 70.19%)"
          />

          <div class="min-w-0 flex-1">
            <AiMobileLoadingBlock />
          </div>
        </div>
      </template>
    </div>

    <div
      v-if="showBackToBottom"
      class="ai-mobile-thread__back-to-bottom absolute bottom-[12px] right-[12px] z-[1]"
      @click="scrollToBottom"
    >
      <CrmIcon name="iconicon_arrow_down" width="16px" height="16px" color="var(--text-n2)" />
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, nextTick, onMounted, ref, watch } from 'vue';

  import { useAiChatRuntime } from '@lib/shared/ai-chat';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import AiMobileLoadingBlock from '../blocks/AiMobileLoadingBlock.vue';
  import AiMobileMessage from './AiMobileMessage.vue';

  const props = withDefaults(
    defineProps<{
      scrollToBottomKey?: string | number;
    }>(),
    {
      scrollToBottomKey: '',
    }
  );

  const runtime = useAiChatRuntime();
  const { t } = useI18n();
  const threadRef = ref<HTMLElement | null>(null);
  const shouldStickToBottom = ref(true);
  const messages = computed(() => runtime.state.messages.value);
  const latestMessageId = computed(() => messages.value.at(-1)?.id);
  const emptySuggestionList = [
    {
      icon: 'icon-ai',
      label: t('aiChat.emptyCustomerLookup'),
    },
    {
      icon: 'icon-ai3',
      label: t('aiChat.emptySalesBrief'),
    },
    {
      icon: 'icon-ai2',
      label: t('aiChat.emptyReceivablesSummary'),
    },
    {
      icon: 'icon-ai4',
      label: t('aiChat.emptyOpportunityStats'),
    },
  ];
  const showThreadLoading = computed(() => {
    const lastMessage = messages.value.at(-1);

    if (!runtime.state.loading.value || !lastMessage) {
      return false;
    }

    return lastMessage.role !== 'assistant';
  });

  const showBackToBottom = computed(() => messages.value.length > 0 && !shouldStickToBottom.value);

  function isNearBottom(): boolean {
    const el = threadRef.value;

    if (!el) {
      return true;
    }

    return el.scrollHeight - el.scrollTop - el.clientHeight <= 48;
  }

  function handleScroll(): void {
    shouldStickToBottom.value = isNearBottom();
  }

  function handleSuggestionClick(label: string): void {
    runtime.setInput(label);
  }

  async function scrollToBottom(): Promise<void> {
    await nextTick();

    const el = threadRef.value;

    if (!el) {
      return;
    }

    el.scrollTop = el.scrollHeight;
    shouldStickToBottom.value = true;
  }

  watch(
    () => JSON.stringify(messages.value.map((message) => [message.id, message.parts.length, message.parts.at(-1)])),
    () => {
      if (shouldStickToBottom.value) {
        scrollToBottom();
      }
    },
    { flush: 'post' }
  );

  watch(showThreadLoading, () => {
    if (shouldStickToBottom.value) {
      scrollToBottom();
    }
  });

  watch(
    () => props.scrollToBottomKey,
    async (key) => {
      if (!key) {
        return;
      }

      await scrollToBottom();
    },
    { flush: 'post' }
  );

  onMounted(async () => {
    await nextTick();

    if (props.scrollToBottomKey || messages.value.length > 0) {
      await scrollToBottom();
    }
  });
</script>

<style scoped lang="less">
  .ai-mobile-thread__back-to-bottom {
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 0;
    width: 32px;
    height: 32px;
    border: 0;
    border-radius: 50%;
    background: var(--text-n10);
    box-shadow: 0 4px 10px -1px #6467671a;
  }
</style>
