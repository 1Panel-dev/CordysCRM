<template>
  <div class="crm-comment-editor">
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

    <MentionUserSelect
      v-model:show="showMentionUserSelect"
      :selected-users="mentionUsers"
      @confirm="confirmMentionUsers"
    />
  </div>
</template>

<script setup lang="ts">
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { filterMentionUsers, FOLLOW_COMMENT_MAX_LENGTH } from '@lib/shared/method/comment';
  import type { FollowCommentSubmitValue, FollowCommentUser } from '@lib/shared/models/follow';

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
    }>(),
    {
      mode: 'create',
      replyUserName: '',
      loading: false,
      disabled: false,
      submitText: '',
      maxlength: FOLLOW_COMMENT_MAX_LENGTH,
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
  const showMentionUserSelect = ref(false);
  const fieldRef = ref<{
    $el?: HTMLElement;
    focus?: () => void;
  }>();

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
    return props.disabled || props.loading || !content.value.trim();
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
    });
  }

  watch(
    () => props.initialMentionUsers,
    (users) => {
      mentionUsers.value = users ? [...users] : [];
    },
    { immediate: true }
  );

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
    display: flex;
    align-items: center;
    gap: 8px;
    overflow: hidden;
    padding: 12px 0;
    width: 100%;
    height: 64px;
    background: var(--text-n10);
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
