<template>
  <article class="mb-[32px] flex gap-[16px]" :class="messageClass">
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
          <n-input
            v-model:value="editContent"
            class="w-full"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 8 }"
            :disabled="runtime.state.loading.value"
            @keydown.enter.exact.prevent="handleEditSubmit"
          />
          <div class="mt-[8px] flex justify-end gap-[8px]">
            <n-button size="small" :disabled="runtime.state.loading.value" @click="cancelEdit">
              {{ t('common.cancel') }}
            </n-button>
            <n-button
              size="small"
              type="primary"
              :disabled="!canSubmitEdit"
              :loading="runtime.state.loading.value"
              @click="handleEditSubmit"
            >
              {{ t('aiChat.send') }}
            </n-button>
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
            <component :is="item.renderer" v-if="item.renderer" :part="item.part" :index="item.index" />
            <div v-else class="ai-chat-block">{{ item.part.type }}</div>
          </template>
        </template>
      </div>

      <div v-if="showActions" class="mt-[6px] flex gap-[8px]" :class="isUser ? 'justify-end' : 'justify-start'">
        <slot name="actions" :message="props.message">
          <CrmButtonGroup :list="messageActions" not-show-divider class="gap-[8px]" @select="handleActionSelect" />
        </slot>
      </div>
    </div>
  </article>
</template>

<script setup lang="ts">
  import { computed, h, ref, watch } from 'vue';
  import { NAvatar, NButton, NInput } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmButtonGroup from '@/components/pure/crm-button-group/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmAvatar from '@/components/business/crm-avatar/index.vue';
  import AiErrorBlock from '../blocks/AiErrorBlock.vue';
  import AiMarkdownBlock from '../blocks/AiMarkdownBlock.vue';
  import AiTextBlock from '../blocks/AiTextBlock.vue';

  import useLegacyCopy from '@/hooks/useLegacyCopy';

  import { useAiChatRuntime } from '../runtime/useAiChatRuntime';
  import type { AiChatMessage, AiChatMessagePart } from '../types';
  import type { Component } from 'vue';

  const props = defineProps<{
    message: AiChatMessage;
  }>();

  const { t } = useI18n();
  const runtime = useAiChatRuntime();
  const { legacyCopy } = useLegacyCopy();

  const isUser = computed(() => props.message.role === 'user');

  const assistantPartRenderers: Partial<Record<AiChatMessagePart['type'], Component>> = {
    'text': AiMarkdownBlock,
    'reasoning': AiMarkdownBlock,
    'data-error': AiErrorBlock,
  };

  const userPartRenderers: Partial<Record<AiChatMessagePart['type'], Component>> = {
    text: AiTextBlock,
  };

  const isEditing = ref(false);
  const editContent = ref('');

  const canRetry = computed(() => props.message.role === 'assistant' && !runtime.state.loading.value);
  const canEdit = computed(() => isUser.value && !runtime.state.loading.value);
  const canSubmitEdit = computed(() => editContent.value.trim().length > 0 && !runtime.state.loading.value);
  const isGenerating = computed(() => runtime.state.loading.value);
  const copyableText = computed(() =>
    props.message.parts
      .filter((part) => ['text', 'reasoning'].includes(part.type))
      .map((part) => ('text' in part ? part.text : ''))
      .filter(Boolean)
      .join('\n\n')
  );
  const canCopy = computed(() => copyableText.value.length > 0);
  const canShowActionArea = computed(() => !isEditing.value && (isUser.value || !isGenerating.value));
  const showActions = computed(() => canShowActionArea.value);

  const messageMcps = computed(() => props.message.metadata?.mcps ?? []);
  const messageAttachments = computed(() => props.message.metadata?.attachments ?? []);

  const partRenderers = computed(() => (isUser.value ? userPartRenderers : assistantPartRenderers));

  const renderableParts = computed(() =>
    props.message.parts
      .filter((part) => ['text', 'reasoning', 'data-error'].includes(part.type))
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
    editContent.value = props.message.parts
      .filter((part) => part.type === 'text')
      .map((part) => part.text)
      .join('\n')
      .trim();
    isEditing.value = true;
  }

  const messageActions = computed(
    () =>
      [
        {
          key: 'copy',
          label: '',
          tooltipContent: t('common.copy'),
          renderIcon: () => h(CrmIcon, { type: 'iconicon_file_copy' }),
          disabled: !canCopy.value,
          visible: isUser.value || !isGenerating.value,
        },
        {
          key: 'retry',
          label: '',
          tooltipContent: t('common.retry'),
          renderIcon: () => h(CrmIcon, { type: 'iconicon_refresh' }),
          disabled: false,
          visible: canRetry.value,
        },
        {
          key: 'edit',
          label: '',
          tooltipContent: t('common.edit'),
          renderIcon: () => h(CrmIcon, { type: 'iconicon_edit' }),
          disabled: false,
          visible: canEdit.value,
        },
      ].filter((action) => action.visible) as ActionsItem[]
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
  .ai-chat-message--user {
    .ai-chat-message__bubble {
      padding: 8px 16px;
      border-radius: 4px;
      background: var(--text-n9);
    }
  }
</style>
