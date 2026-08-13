<template>
  <div class="crm-comment-item" :class="{ 'crm-comment-item--reply': isReply }">
    <CrmAvatar
      :avatar="props.comment.createUserAvatar"
      :is-user="false"
      :size="36"
      :word="props.comment.createUserName"
      class="shrink-0"
    />

    <div class="min-w-0 flex-1">
      <div class="mb-[4px] flex items-center gap-[8px]">
        <div class="max-w-[160px] truncate font-semibold text-[var(--text-n1)]">
          {{ props.comment.createUserName || '-' }}
        </div>
        <div class="shrink-0 text-[var(--text-n4)]">{{ formatCommentTime(props.comment.createTime) }}</div>
      </div>

      <MentionInput
        v-if="isEditing"
        v-model:value="editContent"
        :initial-mention-users="props.comment.mentionUsers"
        :loading="props.submitLoading"
        :submit-text="t('crmComment.save')"
        @submit="handleEditSubmit"
        @cancel="emit('cancelEditor')"
      />

      <div v-else class="crm-comment-item__content">
        <div class="flex items-center">
          <template v-if="props.comment.replyToUserName">
            <div class="text-[var(--text-n4)]">{{ t('crmComment.replyTo') }} {{ props.comment.replyToUserName }}：</div>
          </template>
          <div class="text-[var(--text-n1)]">
            {{ props.comment.content }}
          </div>
        </div>
      </div>

      <div v-if="!isEditing" class="mt-[8px] flex justify-end gap-[8px]">
        <n-button v-if="props.canReply" size="small" class="p-[8px]" quaternary @click="emit('reply', props.comment)">
          <template #icon>
            <CrmIcon type="iconicon_chat" class="text-[var(--text-n4)]" :size="14" />
          </template>
          <div class="text-[var(--text-n4)]">{{ t('crmComment.reply') }}</div>
        </n-button>

        <n-button v-if="props.canEdit" size="small" class="p-[8px]" quaternary @click="emit('edit', props.comment)">
          <template #icon>
            <CrmIcon type="iconicon_edit1" class="text-[var(--text-n4)]" :size="14" />
          </template>
          <div class="text-[var(--text-n4)]">{{ t('crmComment.edit') }}</div>
        </n-button>

        <n-button v-if="props.canDelete" size="small" class="p-[8px]" quaternary @click="emit('delete', props.comment)">
          <template #icon>
            <CrmIcon type="iconicon_delete" class="text-[var(--text-n4)]" :size="14" />
          </template>
          <div class="text-[var(--text-n4)]">{{ t('crmComment.delete') }}</div>
        </n-button>
      </div>

      <MentionInput
        v-if="showInlineReplyEditor"
        class="mt-[12px]"
        :loading="props.submitLoading"
        :reply-user-name="props.comment.createUserName"
        @submit="handleReplySubmit"
        @cancel="emit('cancelEditor')"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
  import { NButton } from 'naive-ui';
  import dayjs from 'dayjs';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type {
    FollowCommentActionValue,
    FollowCommentActiveEditor,
    FollowCommentItem,
    FollowCommentSubmitValue,
  } from '@lib/shared/models/follow';

  import CrmAvatar from '@/components/business/crm-avatar/index.vue';
  import MentionInput from './mentionInput.vue';

  const { t } = useI18n();

  const props = withDefaults(
    defineProps<{
      comment: FollowCommentItem;
      level?: 1 | 2;
      activeEditor?: FollowCommentActiveEditor | null;
      canReply?: boolean;
      canEdit?: boolean;
      canDelete?: boolean;
      submitLoading?: boolean;
    }>(),
    {
      level: 1,
      activeEditor: null,
      canReply: true,
      canEdit: true,
      canDelete: true,
    }
  );

  const emit = defineEmits<{
    (event: 'reply', comment: FollowCommentItem): void;
    (event: 'edit', comment: FollowCommentItem): void;
    (event: 'delete', comment: FollowCommentItem): void;
    (event: 'replySubmit', value: FollowCommentActionValue): void;
    (event: 'editSubmit', value: FollowCommentActionValue): void;
    (event: 'cancelEditor'): void;
  }>();

  const editContent = ref('');

  const isReply = computed(() => props.level === 2);
  const isEditing = computed(
    () => props.activeEditor?.action === 'edit' && props.activeEditor.commentId === props.comment.id
  );
  const showInlineReplyEditor = computed(
    () => props.activeEditor?.action === 'reply' && props.activeEditor.commentId === props.comment.id
  );

  function formatCommentTime(time?: number) {
    if (!time) {
      return '-';
    }
    return dayjs(time).format('YYYY-MM-DD HH:mm');
  }

  function handleReplySubmit(value: FollowCommentSubmitValue) {
    emit('replySubmit', {
      comment: props.comment,
      ...value,
    });
  }

  function handleEditSubmit(value: FollowCommentSubmitValue) {
    emit('editSubmit', {
      comment: props.comment,
      ...value,
    });
  }

  watch(
    () => isEditing.value,
    (editing) => {
      if (editing) {
        editContent.value = props.comment.content;
      }
    },
    { immediate: true }
  );
</script>

<style scoped lang="less">
  .crm-comment-item {
    padding: 12px 24px;
    gap: 10px;
    @apply flex;
  }
  .crm-comment-item--reply {
    margin-left: 40px;
    padding: 12px 16px;
  }
  .crm-comment-item__content {
    white-space: pre-wrap;
    color: var(--text-n1);
    line-height: 20px;
    word-break: break-word;
  }
</style>
