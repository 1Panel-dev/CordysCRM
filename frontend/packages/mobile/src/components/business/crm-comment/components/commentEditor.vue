<template>
  <div class="crm-comment-editor" :class="{ 'crm-comment-editor--attachment': props.enableAttachment }">
    <div v-if="props.enableAttachment && attachments.length" class="crm-comment-editor-attachments">
      <AiMobileAttachmentList
        :attachments="attachments"
        :removable="!props.disabled && !props.loading"
        :show-add="!props.disabled && !props.loading && attachments.length < 10"
        @add="openAttachmentPicker"
        @remove="removeAttachment"
      />
    </div>
    <div class="crm-comment-editor-input">
      <input ref="fileInputRef" type="file" accept="*/*" multiple class="hidden" @change="handleFileInputChange" />
      <CrmTextButton
        v-if="props.enableAttachment"
        icon="iconicon_link1"
        icon-size="18px"
        color="var(--text-n4)"
        @click="openAttachmentPicker"
      />
      <van-field
        ref="fieldRef"
        v-model="content"
        :border="false"
        class="crm-comment-editor-field"
        :maxlength="props.maxlength"
        :placeholder="placeholder"
        :disabled="props.disabled"
        @keyup.enter="handleSubmit"
      />

      <van-button
        class="crm-comment-editor-submit"
        size="small"
        type="primary"
        round
        :loading="props.loading"
        :disabled="submitDisabled"
        @click="handleSubmit"
      >
        {{ submitButtonText }}
      </van-button>
    </div>

    <MentionUserSelect
      v-model:show="showMentionUserSelect"
      :selected-users="mentionUsers"
      @confirm="confirmMentionUsers"
    />
  </div>
</template>

<script setup lang="ts">
  import { showToast } from 'vant';
  import type { AiChatAttachment } from '@lib/shared/ai-chat';
  import AiMobileAttachmentList from '@/components/business/ai-chat/components/AiMobileAttachmentList.vue';
  import CrmTextButton from '@/components/pure/crm-text-button/index.vue';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { getGenerateId } from '@lib/shared/method';
  import { filterMentionUsers, FOLLOW_COMMENT_MAX_LENGTH } from '@lib/shared/method/comment';
  import type { FollowCommentItem, FollowCommentSubmitValue, FollowCommentUser } from '@lib/shared/models/follow';
  import { uploadTempAttachment } from '@/api/modules';

  import MentionUserSelect from './mentionUserSelect.vue';

  const props = withDefaults(
    defineProps<{
      mode?: 'create' | 'reply' | 'edit';
      replyUserName?: string;
      loading?: boolean;
      disabled?: boolean;
      submitText?: string;
      maxlength?: number;
      initialMentionUsers?: FollowCommentUser[];
      initialAttachments?: FollowCommentItem['attachments'];
      enableAttachment?: boolean;
    }>(),
    {
      mode: 'create',
      replyUserName: '',
      loading: false,
      disabled: false,
      submitText: '',
      maxlength: FOLLOW_COMMENT_MAX_LENGTH,
      enableAttachment: false,
    }
  );

  const emit = defineEmits<{
    (e: 'submit', value: FollowCommentSubmitValue): void;
    (e: 'cancel'): void;
  }>();

  const { t } = useI18n();

  const content = defineModel<string>('value', {
    default: '',
  });

  const mentionUsers = ref<FollowCommentUser[]>([]);
  const attachments = ref<AiChatAttachment[]>([]);
  const uploadingAttachmentIds = ref(new Set<string>());
  const fileInputRef = ref<HTMLInputElement>();
  const showMentionUserSelect = ref(false);
  const fieldRef = ref<{
    $el?: HTMLElement;
    focus?: () => void;
  }>();
  function openAttachmentPicker() {
    if (!props.disabled && !props.loading && attachments.value.length < 10) {
      fileInputRef.value?.click();
    }
  }

  function removeAttachment(id: string) {
    if (props.disabled || props.loading) return;
    const attachment = attachments.value.find((item) => item.id === id);
    const previewUrl = attachment?.metadata?.previewUrl;
    if (typeof previewUrl === 'string') URL.revokeObjectURL(previewUrl);
    attachments.value = attachments.value.filter((item) => item.id !== id);
  }

  function clearAttachments() {
    attachments.value.forEach((item) => {
      const previewUrl = item.metadata?.previewUrl;
      if (typeof previewUrl === 'string') URL.revokeObjectURL(previewUrl);
    });
    attachments.value = [];
    uploadingAttachmentIds.value.clear();
  }

  function resetAttachments() {
    clearAttachments();
    attachments.value = (props.initialAttachments || []).map((attachment) => ({
      id: attachment.id,
      name: attachment.name,
      size: attachment.size,
      mimeType: attachment.type,
      kind: /^(jpg|jpeg|png|gif|bmp|webp|svg)$/i.test(attachment.type) ? 'image' : 'file',
      status: 'done',
      metadata: { fileId: attachment.id },
    }));
  }

  async function handleFileInputChange(event: Event) {
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files || []);
    input.value = '';
    if (props.disabled || props.loading) return;
    if (attachments.value.length + files.length > 10) {
      showToast(t('formCreate.advanced.limitCount', { count: 10 }));
      return;
    }
    if (files.some((file) => file.size > 50 * 1024 * 1024)) {
      showToast(t('formCreate.advanced.overSize', { size: '50MB' }));
      return;
    }
    const names = [...attachments.value.map((item) => item.name), ...files.map((file) => file.name)];
    if (new Set(names).size !== names.length) {
      showToast(t('formCreate.upload.repeatFileTip'));
      return;
    }
    const selected: AiChatAttachment[] = files.map((file) => ({
      id: getGenerateId(),
      name: file.name,
      size: file.size,
      mimeType: file.type,
      kind: file.type.startsWith('image/') ? 'image' : 'file',
      status: 'uploading',
      metadata: { previewUrl: file.type.startsWith('image/') ? URL.createObjectURL(file) : undefined },
    }));
    attachments.value.push(...selected);
    await Promise.all(
      selected.map(async (item, index) => {
        uploadingAttachmentIds.value.add(item.id);
        try {
          const result = await uploadTempAttachment(files[index]);
          const current = attachments.value.find((attachment) => attachment.id === item.id);
          if (current) {
            current.metadata = { ...current.metadata, fileId: result.data[0] };
            current.status = 'done';
          }
        } catch {
          const current = attachments.value.find((attachment) => attachment.id === item.id);
          if (current) current.status = 'error';
        } finally {
          uploadingAttachmentIds.value.delete(item.id);
        }
      })
    );
  }

  onBeforeUnmount(clearAttachments);

  const placeholder = computed(() => {
    if (props.mode === 'reply' && props.replyUserName) {
      return t('crmComment.replyPlaceholder', { name: props.replyUserName });
    }
    return `${t('crmComment.commentPlaceholder')}@${t('crmComment.commentPlaceholderOthers')}`;
  });

  const submitButtonText = computed(() => {
    if (props.submitText) {
      return props.submitText;
    }
    return props.mode === 'edit' ? t('crmComment.save') : t('crmComment.submit');
  });

  const submitDisabled = computed(() => {
    return (
      props.disabled ||
      props.loading ||
      !content.value.trim() ||
      uploadingAttachmentIds.value.size > 0 ||
      attachments.value.some((item) => item.status === 'error')
    );
  });

  function openMentionUserSelect() {
    const input = fieldRef.value?.$el?.querySelector<HTMLInputElement | HTMLTextAreaElement>('input, textarea');
    mentionUsers.value = filterMentionUsers(content.value, mentionUsers.value);
    input?.blur();
    showMentionUserSelect.value = true;
  }

  watch(content, (value, oldValue) => {
    if (props.disabled || showMentionUserSelect.value) {
      return;
    }

    const isInputAt = value.length > oldValue.length && value.endsWith('@');
    if (isInputAt) {
      openMentionUserSelect();
    }
  });

  function confirmMentionUsers(users: FollowCommentUser[]) {
    const selectedUserIds = new Set(mentionUsers.value.map((user) => user.id));
    const newUsers = users.filter((user) => !selectedUserIds.has(user.id));
    mentionUsers.value = users;

    if (newUsers.length) {
      const contentWithoutTrigger = content.value.endsWith('@') ? content.value.slice(0, -1) : content.value;
      const prefix = contentWithoutTrigger.endsWith(' ') || !contentWithoutTrigger ? '' : ' ';
      const mentionText = newUsers.map((user) => `@${user.name}`).join(' ');
      content.value = `${contentWithoutTrigger}${prefix}${mentionText} `.slice(0, props.maxlength);
    }
  }

  function handleSubmit() {
    if (submitDisabled.value) {
      return;
    }
    const selectedMentionUsers = filterMentionUsers(content.value, mentionUsers.value);
    emit('submit', {
      content: content.value.trim(),
      mentionUsers: selectedMentionUsers,
      mentionUserIds: selectedMentionUsers.map((user) => user.id),
      attachmentIds: attachments.value.map((item) => item.metadata?.fileId as string),
    });
  }

  watch(
    () => props.initialMentionUsers,
    (users) => {
      mentionUsers.value = users ? [...users] : [];
    },
    { immediate: true }
  );

  watch([() => props.mode, () => props.initialAttachments], resetAttachments, { immediate: true });

  function focus() {
    fieldRef.value?.focus?.();
  }

  defineExpose({
    focus,
  });
</script>

<style scoped lang="less">
  .crm-comment-editor {
    box-sizing: border-box;
    overflow: hidden;
    width: 100%;
    background: var(--text-n10);
  }
  .crm-comment-editor-input {
    display: flex;
    align-items: center;
    gap: 8px;
    overflow: hidden;
    padding: 12px 0;
    width: 100%;
    height: 64px;
    background: var(--text-n10);
  }
  .crm-comment-editor-attachments {
    padding: 12px;
    border-bottom: 1px solid var(--text-n8);
  }
  .crm-comment-editor-field {
    box-sizing: border-box;
    padding: 0;
    min-width: 0;
    height: 40px;
    border: 1px solid var(--text-n8);
    border-radius: 100px;
    background: var(--text-n10);
    flex: 1;
  }
  .crm-comment-editor-field :deep(.van-field__control) {
    height: 38px;
    font-size: 13px;
    color: var(--text-n1);
    line-height: 38px;
  }
  .crm-comment-editor-field :deep(.van-field__body) {
    padding: 0 12px;
    height: 38px;
  }
  .crm-comment-editor-submit {
    box-sizing: border-box;
    padding: 0 16px;
    min-width: 64px;
    max-width: 64px;
    height: 40px;
    border: 0;
    background: var(--primary-8);
    flex: none;
  }
</style>
