<template>
  <div class="crm-comment-editor">
    <van-field
      ref="fieldRef"
      v-model="content"
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
    }>(),
    {
      mode: 'create',
      replyUserName: '',
      loading: false,
      disabled: false,
      submitText: '',
      maxlength: 1000,
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
  const fieldRef = ref<{ focus?: () => void }>();

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

  watch(content, (value, oldValue) => {
    if (props.disabled || showMentionUserSelect.value) {
      return;
    }

    const isInputAt = value.length > oldValue.length && value.endsWith('@');
    if (isInputAt) {
      showMentionUserSelect.value = true;
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
      content.value = `${contentWithoutTrigger}${prefix}${mentionText} `;
    }
  }

  function handleSubmit() {
    if (submitDisabled.value) {
      return;
    }
    emit('submit', {
      content: content.value.trim(),
      mentionUsers: mentionUsers.value,
      mentionUserIds: mentionUsers.value.map((user) => user.id),
    });
  }

  onMounted(() => {
    nextTick(() => {
      fieldRef.value?.focus?.();
    });
  });
</script>

<style scoped lang="less">
  .crm-comment-editor {
    display: flex;
    align-items: center;
    padding: 12px;
    width: 100%;
    height: 64px;
    gap: 8px;
  }
  .crm-comment-editor-field {
    padding: 3px 0;
    min-width: 0;
    border: 1px solid var(--text-n8);
    border-radius: 999px;
    background: var(--text-n10);
    flex: 1;
  }
  .crm-comment-editor-field :deep(.van-field__control) {
    height: 34px;
    font-size: 13px;
    color: var(--text-n1);
    line-height: 34px;
  }
  .crm-comment-editor-field :deep(.van-field__body) {
    padding: 0 12px;
  }
  .crm-comment-editor-submit {
    padding: 8px 16px;
    min-width: 56px;
    height: 40px;
    border: 0;
    background: var(--primary-8);
    flex: none;
  }
</style>
