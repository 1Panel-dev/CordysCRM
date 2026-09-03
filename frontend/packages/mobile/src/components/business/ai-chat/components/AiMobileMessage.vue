<template>
  <div class="flex w-full items-start gap-[8px] [&+&]:mt-[16px]" :class="{ 'flex-row-reverse': isUser }">
    <CrmIcon
      v-if="!isUser"
      name="iconicon_crmbot"
      width="32px"
      height="32px"
      color="linear-gradient(180deg, #00A6AB 0%, #3370FF 70.19%)"
    />
    <CrmAvatar v-if="isUser" :size="40" :is-word="false" class="rounded-[16px]" />

    <div class="flex min-w-0 flex-col" :class="isUser ? 'max-w-[calc(100%-48px)] items-end' : 'flex-1 items-start'">
      <AiMobileAttachmentList
        v-if="messageAttachments.length"
        class="mb-[8px] max-w-full"
        :attachments="messageAttachments"
      />

      <div
        v-if="thoughtParts.length || renderableParts.length || showAssistantLoading"
        class="ai-mobile-message__bubble min-w-0 max-w-full"
        :class="{ 'user-message': isUser }"
      >
        <AiMobileThoughtBlock
          v-if="thoughtParts.length"
          :items="thoughtParts"
          :message-id="props.message.id"
          :is-generating="isGenerating"
          :status="thoughtStatus"
          :duration="props.message.metadata?.duration"
        />
        <template v-for="item in renderableParts" :key="item.key">
          <AiMobileTextBlock v-if="item.part.type === 'text' && isUser" :part="item.part" :mcps="messageMcps" />
          <AiMobileMarkdownBlock v-else-if="item.part.type === 'text'" :part="item.part" :index="item.index" />
          <AiMobileErrorBlock v-else-if="item.part.type === 'data-error'" :part="item.part" />
        </template>
        <AiMobileLoadingBlock v-if="showAssistantLoading" />
        <div v-if="showGeneratingStatus" class="mt-[8px] flex items-center gap-[4px] text-[var(--text-n4)]">
          <span>{{ t('aiChat.generating') }}</span>
          <CrmIcon name="iconicon_loading" width="16px" height="16px" color="var(--text-n4)" class="animate-spin" />
        </div>
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
            :color="feedback === true ? 'var(--primary-0)' : 'var(--primary-8)'"
            @click="handleFeedbackMessage('like')"
          />
          <CrmIcon
            v-if="canFeedback"
            name="iconicon_bad"
            width="16px"
            height="16px"
            :color="feedback === false ? 'var(--primary-0)' : 'var(--primary-8)'"
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
  import { computed, ref, watch } from 'vue';
  import { showFailToast, showSuccessToast } from 'vant';

  import {
    type AiChatMessage,
    type AiChatMessagePart,
    type AiChatThoughtStatus,
    getAiChatMessageCopyText,
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
  import AiMobileTextBlock from '../blocks/AiMobileTextBlock.vue';
  import AiMobileThoughtBlock from '../blocks/AiMobileThoughtBlock.vue';
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
      .map((part, index) => {
        const messagePart = { ...part } as AiChatMessagePart;

        return {
          index,
          key: `${messagePart.type}_${index}`,
          part: messagePart,
        };
      })
      .filter((item) => ['text', 'data-error'].includes(item.part.type))
  );
  const thoughtParts = computed(() =>
    props.message.parts
      .map((part, index) => {
        const messagePart = { ...part } as AiChatMessagePart;

        return {
          index,
          key: `${messagePart.type}_${index}`,
          part: messagePart,
        };
      })
      .filter((item) => !isUser.value && ['reasoning', 'data-progress'].includes(item.part.type))
  );
  const showAssistantLoading = computed(
    () => props.message.role === 'assistant' && props.isGenerating && !hasRenderableAiChatContent(props.message.parts)
  );
  const showGeneratingStatus = computed(() => props.message.role === 'assistant' && props.isGenerating);
  const thoughtStatus = computed<AiChatThoughtStatus>(() => {
    if (props.isGenerating) {
      return 'thinking';
    }

    if (props.message.metadata?.finishReason === 'stopped') {
      return 'stopped';
    }

    return 'completed';
  });
  const copyableText = computed(() => getAiChatMessageCopyText(props.message));
  const canCopy = computed(() => copyableText.value.length > 0);
  const canRetry = computed(() => props.message.role === 'assistant' && !runtime.state.loading.value);
  const canEdit = computed(() => props.message.role === 'user' && !runtime.state.loading.value);
  const runId = computed(() => props.message.metadata?.runId);
  const canFeedback = computed(() => props.message.role === 'assistant' && !props.isGenerating && Boolean(runId.value));
  const tokenUsageText = computed(() =>
    typeof props.message.metadata?.tokens === 'number' ? formatThousands(props.message.metadata.tokens) : ''
  );
  const feedback = ref<boolean | undefined>(props.message.metadata?.helpful);
  const messageAttachments = computed(() => props.message.metadata?.attachments ?? []);
  const messageMcps = computed(() => props.message.metadata?.mcps ?? []);
  const showActions = computed(
    () =>
      !props.isGenerating &&
      (canCopy.value || canRetry.value || canFeedback.value || canEdit.value || tokenUsageText.value)
  );

  watch(
    () => props.message.id,
    () => {
      feedback.value = props.message.metadata?.helpful;
    }
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

    const picked = type === 'like';
    if (feedback.value === picked) {
      return;
    }

    try {
      if (type === 'like') {
        await likeAgentChat(runId.value);
      } else {
        await dislikeAgentChat(runId.value);
      }
      feedback.value = picked;
      if (props.message.metadata) {
        props.message.metadata.helpful = picked;
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
    padding-top: 8px;
    &--user {
      justify-content: flex-end;
    }
    &--assistant {
      margin-top: 8px;
      width: 100%;
      border-top: 1px solid var(--text-n8);
    }
  }
</style>
