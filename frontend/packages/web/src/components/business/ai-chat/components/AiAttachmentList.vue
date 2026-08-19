<template>
  <div class="ai-chat-attachments flex flex-wrap gap-[8px]">
    <div
      v-for="attachment in props.attachments"
      :key="attachment.id"
      class="ai-chat-attachment"
      :class="{
        'ai-chat-attachment--image': isImageAttachment(attachment),
        'ai-chat-attachment--readonly': !props.removable,
        'ai-chat-attachment--error': attachment.status === 'error',
      }"
    >
      <template v-if="isImageAttachment(attachment)">
        <n-image
          v-if="getAttachmentUrl(attachment)"
          class="ai-chat-attachment__image"
          object-fit="cover"
          :src="getAttachmentUrl(attachment)"
          :preview-disabled="attachment.status !== 'done'"
        />
        <div v-else class="ai-chat-attachment__image-placeholder">
          <CrmIcon type="iconicon_image" :size="24" color="var(--text-n4)" />
        </div>
      </template>

      <template v-else>
        <div class="ai-chat-attachment__file-icon">
          <CrmIcon type="iconicon_file" :size="20" color="var(--primary-8)" />
        </div>
      </template>

      <div class="flex min-w-0 flex-1 flex-col gap-[2px]">
        <n-tooltip trigger="hover" placement="top-start">
          <template #trigger>
            <div class="one-line-text text-[12px] text-[var(--text-n1)]">
              {{ attachment.name }}
            </div>
          </template>
          {{ attachment.name }}
        </n-tooltip>
        <div class="flex items-center gap-[6px] text-[12px] text-[var(--text-n4)]">
          <span v-if="attachment.status === 'uploading'">{{ t('aiChat.attachmentUploading') }}</span>
          <span v-else-if="attachment.status === 'error'" class="text-[var(--error-red)]">
            {{ t('aiChat.attachmentUploadFailed') }}
          </span>
          <span v-else-if="attachment.size">{{ formatFileSize(attachment.size) }}</span>
        </div>
      </div>

      <n-spin v-if="attachment.status === 'uploading'" :size="14" />
      <n-button
        v-else-if="attachment.status === 'error' && props.retryable"
        text
        class="ai-chat-attachment__action"
        @click="emit('retry', attachment)"
      >
        {{ t('common.retry') }}
      </n-button>
      <n-button v-if="props.removable" text class="ai-chat-attachment__remove" @click="emit('remove', attachment.id)">
        <CrmIcon type="iconicon_close" :size="14" color="var(--text-n4)" />
      </n-button>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { NButton, NImage, NSpin, NTooltip } from 'naive-ui';

  import type { AiChatAttachment } from '@lib/shared/ai-chat';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { formatFileSize } from '@lib/shared/method';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  const props = withDefaults(
    defineProps<{
      attachments: AiChatAttachment[];
      removable?: boolean;
      retryable?: boolean;
    }>(),
    {
      removable: false,
      retryable: false,
    }
  );

  const emit = defineEmits<{
    (e: 'remove', attachmentId: string): void;
    (e: 'retry', attachment: AiChatAttachment): void;
  }>();

  const { t } = useI18n();

  function isImageAttachment(attachment: AiChatAttachment): boolean {
    return attachment.kind === 'image';
  }

  function getAttachmentUrl(attachment: AiChatAttachment): string {
    return attachment.url || (attachment.metadata?.previewUrl as string | undefined) || '';
  }
</script>

<style scoped lang="scss">
  .ai-chat-attachment {
    position: relative;
    display: flex;
    align-items: center;
    padding: 8px;
    width: 220px;
    min-width: 0;
    border: 1px solid var(--text-n8);
    border-radius: 4px;
    background: var(--text-n10);
    gap: 8px;
    &--error {
      border-color: var(--error-red);
      background: var(--text-n10);
    }
  }
  .ai-chat-attachment__image,
  .ai-chat-attachment__image-placeholder,
  .ai-chat-attachment__file-icon {
    display: inline-flex;
    justify-content: center;
    align-items: center;
    overflow: hidden;
    width: 40px;
    height: 40px;
    border-radius: 4px;
    background: var(--text-n9);
    flex: none;
  }
  .ai-chat-attachment__image {
    :deep(img) {
      width: 40px;
      height: 40px;
      object-fit: cover;
    }
  }
  .ai-chat-attachment__action {
    flex: none;
    color: var(--primary-8);
  }
  .ai-chat-attachment__remove {
    flex: none;
    width: 18px;
    height: 18px;
  }
</style>
