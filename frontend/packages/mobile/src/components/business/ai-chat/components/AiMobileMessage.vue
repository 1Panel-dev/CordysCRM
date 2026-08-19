<template>
  <div class="flex w-full items-start gap-[8px] [&+&]:mt-[16px]" :class="{ 'flex-row-reverse': isUser }">
    <div
      v-if="!isUser"
      class="inline-flex h-[40px] w-[40px] flex-none items-center justify-center rounded-[16px] bg-[var(--primary-6)]"
    >
      <CrmIcon name="iconicon_crmbot" width="28px" height="28px" color="var(--primary-8)" />
    </div>
    <CrmAvatar v-if="isUser" :size="40" :is-word="false" class="rounded-[16px]" />

    <div class="flex min-w-0 flex-col" :class="isUser ? 'max-w-[calc(100%-48px)] items-end' : 'flex-1 items-start'">
      <AiMobileAttachmentList
        v-if="messageAttachments.length"
        class="mb-[8px] w-full"
        :attachments="messageAttachments"
      />

      <div
        v-if="renderableParts.length || showAssistantLoading"
        class="ai-mobile-message__bubble w-full min-w-0"
        :class="{ 'user-message': isUser }"
      >
        <template v-for="item in renderableParts" :key="item.key">
          <AiMobileTextBlock v-if="item.part.type === 'text' && isUser" :part="item.part" :mcps="messageMcps" />
          <AiMobileMarkdownBlock
            v-else-if="item.part.type === 'text' || item.part.type === 'reasoning'"
            :part="item.part"
            :index="item.index"
            :is-generating="isGenerating"
          />
          <AiMobileErrorBlock v-else-if="item.part.type === 'data-error'" :part="item.part" />
          <AiMobileProgressBlock
            v-else-if="item.part.type === 'data-progress'"
            :part="item.part"
            :index="item.index"
            :is-generating="isGenerating"
          />
        </template>
        <AiMobileLoadingBlock v-if="showAssistantLoading" />
      </div>

      <div
        v-if="showActions"
        class="ai-mobile-message__actions"
        :class="{
          'ai-mobile-message__actions--user': isUser,
          'ai-mobile-message__actions--assistant': props.message.role === 'assistant',
        }"
      >
        <div class="flex items-center gap-[16px]">
          <CrmIcon
            v-if="canCopy"
            name="iconicon_file_copy"
            width="16px"
            height="16px"
            color="var(--primary-8)"
            @click="handleCopyMessage"
          />
          <CrmIcon
            v-if="canRetry"
            name="iconicon_refresh"
            width="16px"
            height="16px"
            color="var(--primary-8)"
            @click="handleRetry"
          />
          <CrmIcon
            v-if="canFeedback"
            name="iconicon_good"
            width="16px"
            height="16px"
            color="var(--primary-8)"
            @click="handleFeedbackMessage('like')"
          />
          <CrmIcon
            v-if="canFeedback"
            name="iconicon_bad"
            width="16px"
            height="16px"
            color="var(--primary-8)"
            @click="handleFeedbackMessage('dislike')"
          />
          <CrmIcon
            v-if="canEdit"
            name="iconicon_edit"
            width="16px"
            height="16px"
            color="var(--primary-8)"
            @click="handleEdit"
          />
        </div>

        <div v-if="tokenUsageText" class="flex items-center gap-[8px] text-[var(--text-n2)]">
          <span>{{ t('aiChat.tokensUsed', { tokens: tokenUsageText }) }}</span>
          <CrmIcon name="iconicon_star1" width="16px" height="16px" color="var(--text-n4)" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed } from 'vue';
  import { showFailToast, showSuccessToast } from 'vant';

  import {
    type AiChatMessage,
    type AiChatMessagePart,
    getAiChatMessageText,
    hasRenderableAiChatContent,
    useAiChatRuntime,
  } from '@lib/shared/ai-chat';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { formatThousands } from '@lib/shared/method';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmAvatar from '@/components/business/crm-avatar/index.vue';
  import AiMobileErrorBlock from '../blocks/AiMobileErrorBlock.vue';
  import AiMobileLoadingBlock from '../blocks/AiMobileLoadingBlock.vue';
  import AiMobileMarkdownBlock from '../blocks/AiMobileMarkdownBlock.vue';
  import AiMobileProgressBlock from '../blocks/AiMobileProgressBlock.vue';
  import AiMobileTextBlock from '../blocks/AiMobileTextBlock.vue';
  import AiMobileAttachmentList from './AiMobileAttachmentList.vue';

  import { dislikeAgentChat, likeAgentChat } from '@/api/modules';

  const props = defineProps<{
    message: AiChatMessage;
    isGenerating?: boolean;
  }>();

  const { t } = useI18n();
  const runtime = useAiChatRuntime();

  const isUser = computed(() => props.message.role === 'user');
  const renderableParts = computed(() =>
    props.message.parts
      .filter((part) => ['text', 'reasoning', 'data-error', 'data-progress'].includes(part.type))
      .map((part, index) => {
        const messagePart = { ...part } as AiChatMessagePart;

        return {
          index,
          key: `${messagePart.type}_${index}`,
          part: messagePart,
        };
      })
  );
  const showAssistantLoading = computed(
    () => props.message.role === 'assistant' && props.isGenerating && !hasRenderableAiChatContent(props.message.parts)
  );
  const copyableText = computed(() => getAiChatMessageText(props.message));
  const canCopy = computed(() => copyableText.value.length > 0);
  const canRetry = computed(() => props.message.role === 'assistant' && !runtime.state.loading.value);
  const canEdit = computed(() => props.message.role === 'user' && !runtime.state.loading.value);
  const runId = computed(() => props.message.metadata?.runId);
  const canFeedback = computed(() => props.message.role === 'assistant' && !props.isGenerating && Boolean(runId.value));
  const tokenUsageText = computed(() =>
    typeof props.message.metadata?.tokens === 'number' ? formatThousands(props.message.metadata.tokens) : ''
  );
  const messageAttachments = computed(() => props.message.metadata?.attachments ?? []);
  const messageMcps = computed(() => props.message.metadata?.mcps ?? []);
  const showActions = computed(
    () =>
      !props.isGenerating &&
      (canCopy.value || canRetry.value || canFeedback.value || canEdit.value || tokenUsageText.value)
  );

  async function handleCopyMessage() {
    if (!copyableText.value) {
      return;
    }

    try {
      await navigator.clipboard?.writeText(copyableText.value);
      showSuccessToast(t('common.copySuccess'));
    } catch {
      showFailToast(t('common.copyNotSupport'));
    }
  }

  async function handleRetry() {
    await runtime.retry(props.message.id);
  }

  async function handleFeedbackMessage(type: 'like' | 'dislike') {
    if (!runId.value) {
      return;
    }

    try {
      if (type === 'like') {
        await likeAgentChat(runId.value);
      } else {
        await dislikeAgentChat(runId.value);
      }
      showSuccessToast(t('aiChat.feedbackThanks'));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  function handleEdit() {
    runtime.startEditMessage(props.message.id);
  }
</script>

<style scoped lang="less">
  .ai-mobile-message__bubble :deep(.ai-chat-block + .ai-chat-block) {
    margin-top: 8px;
  }
  .user-message {
    padding: 12px;
    background-color: var(--primary-7);
    border-top-left-radius: 12px;
    border-bottom-right-radius: 12px;
    border-bottom-left-radius: 12px;
  }
  .ai-mobile-message__actions {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 8px;
    padding-top: 8px;
    border-top: 1px solid var(--text-n8);
    &--user {
      justify-content: flex-end;
    }
    &--assistant {
      width: 100%;
    }
  }
</style>
