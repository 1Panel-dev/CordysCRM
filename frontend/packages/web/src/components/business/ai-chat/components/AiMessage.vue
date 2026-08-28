<template>
  <article class="group mb-[32px] flex gap-[16px] overflow-hidden" :class="messageClass">
    <div>
      <slot name="avatar" :message="props.message">
        <CrmIcon
          v-if="props.message.role === 'assistant'"
          class="shrink-0"
          type="iconicon_crmbot"
          :size="32"
          color="linear-gradient(180deg, #00A6AB 0%, #3370FF 70.19%)"
        />
        <CrmAvatar v-else :size="32" class="flex-shrink-0 transition-all" />
      </slot>
    </div>

    <div
      class="flex min-w-0 max-w-[calc(100%-96px)] flex-col overflow-hidden"
      :class="{
        'w-full': !isUser || isEditing,
        'items-end': isUser && !isEditing,
        'items-start': !isUser,
      }"
    >
      <div v-if="roleText.length" class="mb-[8px] font-[600]">
        {{ roleText }}
      </div>

      <template v-if="isEditing">
        <div class="ai-chat-message__bubble">
          <div class="ai-chat-message__edit rounded-[4px] bg-[var(--text-n9)] p-[16px]">
            <AiComposer
              ref="editComposerRef"
              class="!bg-transparent !p-0 !shadow-none"
              :initial-content="editContent"
              :initial-mcps="messageMcps"
              :mcp-options="messageMcps"
              :show-attachments="false"
              :show-footer="false"
              :sync-runtime="false"
              submit-mode="emit"
              @change="handleEditChange"
              @submit="handleEditSubmit"
            />
            <div class="mt-[16px] flex items-center justify-between">
              <div class="flex min-w-0 items-center gap-[4px] text-[12px] text-[var(--text-n4)]">
                <CrmIcon type="iconicon_info_circle" :size="14" />
                <span>{{ t('aiChat.editRestartTip') }}</span>
              </div>
              <div class="flex gap-[12px]">
                <n-button :disabled="runtime.state.loading.value" @click="cancelEdit">
                  {{ t('common.cancel') }}
                </n-button>
                <n-button
                  type="primary"
                  ghost
                  :disabled="!canSubmitEdit"
                  :loading="runtime.state.loading.value"
                  @click="handleEditButtonClick"
                >
                  {{ t('aiChat.send') }}
                </n-button>
              </div>
            </div>
          </div>
        </div>
      </template>

      <template v-else>
        <AiAttachmentList
          v-if="messageAttachments.length"
          class="mb-[8px]"
          :class="{ 'justify-end': isUser }"
          :attachments="messageAttachments"
        />

        <div
          v-if="thoughtParts.length || renderableParts.length || showAssistantLoading"
          class="ai-chat-message__bubble max-w-full overflow-hidden"
          :class="{ 'w-full': !isUser }"
        >
          <AiThoughtBlock
            v-if="thoughtParts.length"
            :items="thoughtParts"
            :message-id="props.message.id"
            :is-generating="isGenerating"
            :duration="props.message.metadata?.duration"
          />
          <template v-for="item in renderableParts" :key="item.key">
            <AiTextBlock v-if="isUserTextPart(item.part)" :part="item.part" :mcps="messageMcps" />
            <component
              :is="item.renderer"
              v-else-if="item.renderer"
              :part="item.part"
              :index="item.index"
              :is-generating="isGenerating"
            />
            <div v-else class="ai-chat-block">{{ item.part.type }}</div>
          </template>
          <AiLoadingBlock v-if="showAssistantLoading" />
        </div>
      </template>

      <div
        v-if="showActions"
        class="mt-[8px] flex items-center gap-[12px] text-[var(--text-n4)] opacity-0 transition-opacity focus-within:opacity-100 group-hover:opacity-100"
        :class="isUser ? 'justify-end' : 'justify-start'"
      >
        <n-tooltip v-for="action in messageActions" :key="action.key" :delay="300">
          <template #trigger>
            <CrmIcon
              class="cursor-pointer"
              :type="action.iconType"
              :size="16"
              :color="actionColor(action.key)"
              :class="actionClass(action.key)"
              @click="handleActionSelect(action.key)"
            />
          </template>
          {{ action.tooltipContent }}
        </n-tooltip>

        <div v-if="tokenUsageText" class="flex items-center gap-[8px]">
          <CrmIcon type="iconicon_star1" :size="16" />
          <span>{{ t('aiChat.tokensUsed', { tokens: tokenUsageText }) }}</span>
        </div>
      </div>
    </div>
  </article>
</template>

<script setup lang="ts">
  import { computed, ref, watch } from 'vue';
  import { NButton, NTooltip, useMessage } from 'naive-ui';

  import type { AiChatMessage, AiChatMessagePart, AiComposerSubmitPayload } from '@lib/shared/ai-chat';
  import { getAiChatMessageText, hasRenderableAiChatContent, useAiChatRuntime } from '@lib/shared/ai-chat';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { formatThousands } from '@lib/shared/method';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmAvatar from '@/components/business/crm-avatar/index.vue';
  import AiErrorBlock from '../blocks/AiErrorBlock.vue';
  import AiLoadingBlock from '../blocks/AiLoadingBlock.vue';
  import AiMarkdownBlock from '../blocks/AiMarkdownBlock.vue';
  import AiTextBlock from '../blocks/AiTextBlock.vue';
  import AiThoughtBlock from '../blocks/AiThoughtBlock.vue';
  import AiAttachmentList from './AiAttachmentList.vue';
  import AiComposer from './AiComposer.vue';

  import { dislikeAgentChat, likeAgentChat } from '@/api/modules';
  import useLegacyCopy from '@/hooks/useLegacyCopy';

  import type { Component } from 'vue';

  const props = defineProps<{
    message: AiChatMessage;
    isGenerating?: boolean;
  }>();

  interface AiMessageAction {
    key: 'copy' | 'retry' | 'edit' | 'like' | 'dislike';
    iconType: string;
    tooltipContent: string;
    visible: boolean;
  }

  const { t } = useI18n();
  const Message = useMessage();
  const runtime = useAiChatRuntime();
  const { legacyCopy } = useLegacyCopy();

  const isUser = computed(() => props.message.role === 'user');

  const assistantPartRenderers: Partial<Record<AiChatMessagePart['type'], Component>> = {
    'text': AiMarkdownBlock,
    'data-error': AiErrorBlock,
  };

  const isEditing = ref(false);
  const editContent = ref('');
  const editComposerRef = ref<InstanceType<typeof AiComposer> | null>(null);

  const canRetry = computed(() => props.message.role === 'assistant' && !runtime.state.loading.value);
  const canSubmitEdit = computed(() => editContent.value.trim().length > 0 && !runtime.state.loading.value);
  const isGenerating = computed(() => Boolean(props.isGenerating));
  const copyableText = computed(() => getAiChatMessageText(props.message));
  const canCopy = computed(() => copyableText.value.length > 0);
  const canShowActionArea = computed(() => !isEditing.value && (isUser.value || !isGenerating.value));
  const runId = computed(() => props.message.metadata?.runId);
  const canFeedback = computed(() => !isUser.value && !isGenerating.value && Boolean(runId.value));

  const tokenUsageText = computed(() =>
    typeof props.message.metadata?.tokens === 'number' ? formatThousands(props.message.metadata.tokens) : ''
  );
  const feedback = ref<boolean | undefined>(props.message.metadata?.helpful);

  function actionClass(key: string): Record<string, boolean> {
    const isActiveFeedback = key === 'like' || key === 'dislike';

    if (!isActiveFeedback || !canFeedback.value) {
      return {};
    }

    const active = key === 'like' ? feedback.value === true : feedback.value === false;

    return {
      'ai-chat-message__feedback--active': active,
      'cursor-pointer': !active,
      'cursor-not-allowed': active,
    };
  }

  function actionColor(key: string): string | undefined {
    if ((key === 'like' && feedback.value === true) || (key === 'dislike' && feedback.value === false)) {
      return 'var(--primary-8)';
    }

    return undefined;
  }

  const messageAttachments = computed(() => props.message.metadata?.attachments ?? []);
  const messageMcps = computed(() => props.message.metadata?.mcps ?? []);

  const renderableParts = computed(() =>
    props.message.parts
      .map((part, index) => {
        const messagePart = { ...part } as AiChatMessagePart;

        return {
          index,
          key: `${messagePart.type}_${index}`,
          part: messagePart,
          renderer: isUser.value ? undefined : assistantPartRenderers[messagePart.type],
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
    () => !isUser.value && isGenerating.value && !hasRenderableAiChatContent(props.message.parts)
  );

  const messageClass = computed(() => ({
    'flex-row-reverse': isUser.value,
    'ai-chat-message--user': isUser.value && !isEditing.value,
  }));

  const roleText = computed(() => {
    if (props.message.role === 'assistant') {
      return 'CORDYS AI';
    }

    return '';
  });

  function isUserTextPart(part: AiChatMessagePart): part is AiChatMessagePart & { type: 'text'; text: string } {
    return isUser.value && part.type === 'text' && 'text' in part;
  }

  watch(
    () => props.message.id,
    () => {
      isEditing.value = false;
      editContent.value = '';
      feedback.value = props.message.metadata?.helpful;
    }
  );

  // 重试
  async function handleRetry(): Promise<void> {
    await runtime.retry(props.message.id);
  }

  async function handleCopyMessage(): Promise<void> {
    const text = copyableText.value;

    if (!text.trim()) {
      return;
    }

    await legacyCopy(text);
  }

  async function handleLikeMessage(): Promise<void> {
    if (!runId.value || feedback.value === true) {
      return;
    }

    try {
      await likeAgentChat(runId.value);
      feedback.value = true;
      if (props.message.metadata) {
        props.message.metadata.helpful = true;
      }
      Message.success(t('aiChat.feedbackThanks'));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  async function handleDislikeMessage(): Promise<void> {
    if (!runId.value || feedback.value === false) {
      return;
    }

    try {
      await dislikeAgentChat(runId.value);
      feedback.value = false;
      if (props.message.metadata) {
        props.message.metadata.helpful = false;
      }
      Message.success(t('aiChat.feedbackThanks'));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  function startEdit(): void {
    editContent.value = getAiChatMessageText(props.message, '\n').trim();
    isEditing.value = true;
  }

  const messageActions = computed(
    () =>
      [
        {
          key: 'copy',
          iconType: 'iconicon_file_copy',
          tooltipContent: t('common.copy'),
          visible: canCopy.value && (isUser.value || !isGenerating.value),
        },
        {
          key: 'retry',
          iconType: 'iconicon_refresh',
          tooltipContent: t('common.retry'),
          visible: canRetry.value,
        },
        {
          key: 'like',
          iconType: 'iconicon_good',
          tooltipContent: t('aiChat.like'),
          visible: canFeedback.value,
        },
        {
          key: 'dislike',
          iconType: 'iconicon_bad',
          tooltipContent: t('aiChat.dislike'),
          visible: canFeedback.value,
        },
        {
          key: 'edit',
          iconType: 'iconicon_edit',
          tooltipContent: t('common.edit'),
          visible: isUser.value && !runtime.state.loading.value,
        },
      ].filter((action) => action.visible) as AiMessageAction[]
  );

  const showActions = computed(
    () => canShowActionArea.value && (messageActions.value.length > 0 || tokenUsageText.value)
  );

  async function handleActionSelect(key: string) {
    switch (key) {
      case 'copy':
        await handleCopyMessage();
        break;
      case 'retry':
        await handleRetry();
        break;
      case 'edit':
        startEdit();
        break;
      case 'like':
        await handleLikeMessage();
        break;
      case 'dislike':
        await handleDislikeMessage();
        break;
      default:
        break;
    }
  }

  function cancelEdit(): void {
    isEditing.value = false;
    editContent.value = '';
  }

  function handleEditChange(payload: AiComposerSubmitPayload): void {
    editContent.value = payload.content;
  }

  async function handleEditSubmit(payload?: AiComposerSubmitPayload): Promise<void> {
    if (!canSubmitEdit.value) {
      return;
    }

    const editPayload = payload ?? editComposerRef.value?.getSubmitPayload();
    const content = (editPayload?.content ?? editContent.value).trim();

    if (!content) {
      return;
    }

    isEditing.value = false;
    editContent.value = '';
    await runtime.edit(props.message.id, content, {
      mcps: editPayload?.options?.mcps ?? [],
    });
  }

  async function handleEditButtonClick(): Promise<void> {
    await handleEditSubmit();
  }
</script>

<style scoped lang="scss">
  .ai-chat-message__bubble :deep(.ai-chat-block + .ai-chat-block) {
    margin-top: 8px;
  }
  .ai-chat-message__edit-input {
    background: transparent;
    &.n-input--focus {
      background: transparent;
    }
    :deep(.n-input-wrapper) {
      padding: 0;
    }
    :deep(.n-input__textarea-el) {
      padding: 0;
      background: transparent;
    }
  }
  .ai-chat-message--user {
    .ai-chat-message__bubble {
      padding: 8px 16px;
      border-radius: 4px;
      background: var(--text-n9);
    }
  }
  .ai-chat-message__feedback--active {
    color: var(--primary-8);
  }
</style>
