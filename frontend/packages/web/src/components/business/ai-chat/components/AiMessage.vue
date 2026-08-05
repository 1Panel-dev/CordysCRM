<template>
  <article class="group mb-[32px] flex gap-[16px]" :class="messageClass">
    <div>
      <slot name="avatar" :message="props.message">
        <n-avatar v-if="props.message.role === 'assistant'" round class="bg-[var(--primary-6)]" :size="32">
          <CrmIcon type="iconicon_bot" :size="20" color="var(--primary-8)" />
        </n-avatar>
        <CrmAvatar v-else :size="32" class="flex-shrink-0 transition-all" />
      </slot>
    </div>

    <div class="min-w-0 max-w-[calc(100%-96px)]" :class="{ 'w-full': !isUser || isEditing }">
      <div v-if="roleText.length" class="mb-[8px] font-[600]">
        {{ roleText }}
      </div>

      <div class="ai-chat-message__bubble">
        <template v-if="isEditing">
          <div class="ai-chat-message__edit rounded-[4px] bg-[var(--text-n9)] p-[16px]">
            <n-input
              v-model:value="editContent"
              class="ai-chat-message__edit-input w-full"
              type="textarea"
              :bordered="false"
              :autosize="{ minRows: 1, maxRows: 10 }"
              :disabled="runtime.state.loading.value"
              @keydown.enter.exact.prevent="handleEditSubmit"
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
                  @click="handleEditSubmit"
                >
                  {{ t('aiChat.send') }}
                </n-button>
              </div>
            </div>
          </div>
        </template>

        <template v-else>
          <!-- TODO lmy mcp的样式 -->
          <div v-if="messageMcps.length" class="mb-[6px] flex flex-wrap gap-[4px]">
            <span
              v-for="mcp in messageMcps"
              :key="mcp.id"
              class="inline-flex max-w-[180px] items-center truncate rounded-[4px] bg-[#eee7ff] px-[6px] py-[2px] text-[12px] text-[#3f2f73]"
            >
              {{ mcp.name }}
            </span>
          </div>

          <!-- TODO lmy 文件的样式 -->
          <div v-if="messageAttachments.length" class="mb-[8px] flex flex-wrap gap-[6px]">
            <div
              v-for="attachment in messageAttachments"
              :key="attachment.id"
              class="max-w-[220px] overflow-hidden truncate rounded-[4px] border border-[#edf0f2] bg-[#f7f8fa] px-[8px] py-[4px]"
            >
              {{ attachment.name }}
            </div>
          </div>

          <template v-for="item in renderableParts" :key="item.key">
            <component
              :is="item.renderer"
              v-if="item.renderer"
              :part="item.part"
              :index="item.index"
              :is-generating="isGenerating"
            />
            <div v-else class="ai-chat-block">{{ item.part.type }}</div>
          </template>
          <AiLoadingBlock v-if="showAssistantLoading" />
        </template>
      </div>

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
  import { NAvatar, NButton, NInput, NTooltip } from 'naive-ui';

  import { getAiChatMessageText, hasRenderableAiChatContent } from '@lib/shared/ai-chat';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { formatThousands } from '@lib/shared/method';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmAvatar from '@/components/business/crm-avatar/index.vue';
  import AiErrorBlock from '../blocks/AiErrorBlock.vue';
  import AiLoadingBlock from '../blocks/AiLoadingBlock.vue';
  import AiMarkdownBlock from '../blocks/AiMarkdownBlock.vue';
  import AiProgressBlock from '../blocks/AiProgressBlock.vue';
  import AiTextBlock from '../blocks/AiTextBlock.vue';

  import useLegacyCopy from '@/hooks/useLegacyCopy';

  import { useAiChatRuntime } from '../runtime/useAiChatRuntime';
  import type { AiChatMessage, AiChatMessagePart } from '../types';
  import type { Component } from 'vue';

  const props = defineProps<{
    message: AiChatMessage;
    isGenerating?: boolean;
  }>();

  interface AiMessageAction {
    key: 'copy' | 'retry' | 'edit';
    iconType: string;
    tooltipContent: string;
    visible: boolean;
  }

  const { t } = useI18n();
  const runtime = useAiChatRuntime();
  const { legacyCopy } = useLegacyCopy();

  const isUser = computed(() => props.message.role === 'user');

  const assistantPartRenderers: Partial<Record<AiChatMessagePart['type'], Component>> = {
    'text': AiMarkdownBlock,
    'reasoning': AiMarkdownBlock,
    'data-error': AiErrorBlock,
    'data-progress': AiProgressBlock,
  };

  const userPartRenderers: Partial<Record<AiChatMessagePart['type'], Component>> = {
    text: AiTextBlock,
  };

  const isEditing = ref(false);
  const editContent = ref('');

  const canRetry = computed(() => props.message.role === 'assistant' && !runtime.state.loading.value);
  const canSubmitEdit = computed(() => editContent.value.trim().length > 0 && !runtime.state.loading.value);
  const isGenerating = computed(() => Boolean(props.isGenerating));
  const copyableText = computed(() => getAiChatMessageText(props.message));
  const canCopy = computed(() => copyableText.value.length > 0);
  const canShowActionArea = computed(() => !isEditing.value && (isUser.value || !isGenerating.value));

  const tokenUsageText = computed(() =>
    typeof props.message.metadata?.tokens === 'number' ? formatThousands(props.message.metadata.tokens) : ''
  );

  const messageMcps = computed(() => props.message.metadata?.mcps ?? []);
  const messageAttachments = computed(() => props.message.metadata?.attachments ?? []);

  const partRenderers = computed(() => (isUser.value ? userPartRenderers : assistantPartRenderers));

  const renderableParts = computed(() =>
    props.message.parts
      .filter((part) => ['text', 'reasoning', 'data-error', 'data-progress'].includes(part.type))
      .map((part, index) => {
        const messagePart = { ...part } as AiChatMessagePart;

        return {
          index,
          key: `${messagePart.type}_${index}`,
          part: messagePart,
          renderer: partRenderers.value[messagePart.type],
        };
      })
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

  watch(
    () => props.message.id,
    () => {
      isEditing.value = false;
      editContent.value = '';
    }
  );

  // 重试
  async function handleRetry(): Promise<void> {
    await runtime.retry(props.message.id);
  }

  async function handleCopyMessage(): Promise<void> {
    if (!canCopy.value) {
      return;
    }

    await legacyCopy(copyableText.value);
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
      default:
        break;
    }
  }

  function cancelEdit(): void {
    isEditing.value = false;
    editContent.value = '';
  }

  async function handleEditSubmit(): Promise<void> {
    if (!canSubmitEdit.value) {
      return;
    }

    const content = editContent.value.trim();
    isEditing.value = false;
    editContent.value = '';
    await runtime.edit(props.message.id, content);
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
</style>
