<template>
  <div class="ai-mobile-attachments flex max-w-full gap-[12px] overflow-x-auto">
    <div
      v-for="attachment in props.attachments"
      :key="attachment.id"
      class="ai-mobile-attachment"
      :class="{ 'ai-mobile-attachment--file': !isImageAttachment(attachment) }"
    >
      <template v-if="isImageAttachment(attachment)">
        <van-image
          class="h-full w-full overflow-hidden rounded-[6px]"
          fit="cover"
          :src="getAttachmentUrl(attachment)"
          @click="previewImage(attachment)"
        />
      </template>
      <template v-else>
        <div class="one-line-text mt-[8px] w-full text-center text-[12px] text-[var(--text-n1)]">
          {{ attachment.name }}
        </div>
        <div v-if="attachment.status === 'uploading'" class="mt-[2px] text-[12px] text-[var(--text-n4)]">
          {{ t('aiChat.attachmentUploading') }}
        </div>
        <div v-else-if="attachment.status === 'error'" class="mt-[2px] text-[12px] text-[var(--error-red)]">
          {{ t('aiChat.attachmentUploadFailed') }}
        </div>
        <div v-else-if="attachment.size" class="mt-[2px] text-[12px] text-[var(--text-n4)]">
          {{ formatFileSize(attachment.size) }}
        </div>
      </template>

      <div v-if="isImageAttachment(attachment) && attachment.status !== 'done'" class="ai-mobile-attachment__status">
        {{ attachment.status === 'error' ? t('aiChat.attachmentUploadFailed') : t('aiChat.attachmentUploading') }}
      </div>

      <div v-if="props.removable" class="ai-mobile-attachment__remove" @click="emit('remove', attachment.id)">
        <CrmIcon name="iconicon_close" width="16px" height="16px" color="var(--text-n10)" />
      </div>
    </div>

    <div v-if="props.showAdd" class="ai-mobile-attachment ai-mobile-attachment--add" @click="emit('add')">
      <CrmIcon name="iconicon_add" width="28px" height="28px" color="var(--text-n4)" />
    </div>
  </div>
</template>

<script setup lang="ts">
  import { showImagePreview } from 'vant';

  import type { AiChatAttachment } from '@lib/shared/ai-chat';
  import { PreviewPictureUrl } from '@lib/shared/api/requrls/system/module';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { formatFileSize } from '@lib/shared/method';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  import useUserStore from '@/store/modules/user';

  const props = withDefaults(
    defineProps<{
      attachments: AiChatAttachment[];
      removable?: boolean;
      showAdd?: boolean;
    }>(),
    {
      removable: false,
      showAdd: false,
    }
  );

  const emit = defineEmits<{
    (e: 'remove', attachmentId: string): void;
    (e: 'add'): void;
  }>();
  const { t } = useI18n();
  const userStore = useUserStore();

  function isImageAttachment(attachment: AiChatAttachment): boolean {
    return attachment.kind === 'image';
  }

  function getAttachmentId(attachment: AiChatAttachment): string {
    const fileId = attachment.metadata?.fileId;

    return typeof fileId === 'string' ? fileId : attachment.id;
  }

  function getAttachmentUrl(attachment: AiChatAttachment): string {
    const localPreviewUrl = attachment.metadata?.previewUrl;

    if (typeof localPreviewUrl === 'string') {
      return localPreviewUrl;
    }

    const attachmentId = getAttachmentId(attachment);

    return attachmentId ? `${PreviewPictureUrl}/${attachmentId}?userId=${userStore.userInfo.id}` : '';
  }

  function previewImage(attachment: AiChatAttachment): void {
    if (attachment.status !== 'done') {
      return;
    }

    const localPreviewUrl = attachment.metadata?.previewUrl;
    if (typeof localPreviewUrl === 'string') {
      showImagePreview([localPreviewUrl]);
      return;
    }

    const attachmentId = getAttachmentId(attachment);

    if (!attachmentId) {
      return;
    }

    showImagePreview([`${PreviewPictureUrl}/${attachmentId}?userId=${userStore.userInfo.id}`]);
  }
</script>

<style scoped lang="less">
  .ai-mobile-attachments {
    min-width: 0;
    -webkit-overflow-scrolling: touch;
    scrollbar-width: none;
    &::-webkit-scrollbar {
      display: none;
    }
  }
  .ai-mobile-attachment {
    position: relative;
    flex: none;
    width: 80px;
    height: 80px;
    border-radius: 6px;
    background: var(--text-n9);
    &--file {
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;
      padding: 8px;
    }
    &--add {
      display: flex;
      justify-content: center;
      align-items: center;
    }
  }
  .ai-mobile-attachment__remove {
    position: absolute;
    top: 0;
    right: 0;
    display: flex;
    justify-content: center;
    align-items: center;
    width: 20px;
    height: 20px;
    border-top-right-radius: 6px;
    border-bottom-left-radius: 6px;
    background: var(--text-n4);
  }
  .ai-mobile-attachment__status {
    position: absolute;
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 8px;
    font-size: 12px;
    border-radius: 6px;
    text-align: center;
    color: var(--text-n10);
    background: rgb(0 0 0 / 45%);
    inset: 0;
  }
</style>
