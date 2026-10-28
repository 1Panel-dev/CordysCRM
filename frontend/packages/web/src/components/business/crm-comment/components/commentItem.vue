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
      <div class="mb-[4px] flex items-center justify-between gap-[8px]">
        <div class="flex min-w-0 items-center gap-[8px]">
          <n-tooltip :delay="300">
            <template #trigger>
              <div class="max-w-[160px] truncate font-semibold text-[var(--text-n1)]">
                {{ props.comment.createUserName || '-' }}
              </div>
            </template>
            {{ props.comment.createUserName || '-' }}
          </n-tooltip>
          <div class="shrink-0 text-[var(--text-n4)]">{{ formatCommentTime(props.comment.createTime) }}</div>
        </div>
        <CrmMoreAction
          v-if="props.actionsInMore && moreActions.length"
          placement="bottom-end"
          :options="moreActions"
          trigger="click"
          @select="handleMoreActionSelect"
        >
          <n-button size="small" class="p-[8px]" quaternary>
            <template #icon>
              <CrmIcon type="iconicon_ellipsis" class="text-[var(--text-n4)]" :size="16" />
            </template>
          </n-button>
        </CrmMoreAction>
      </div>

      <MentionInput
        v-if="isEditing"
        v-model:value="editContent"
        :initial-mention-users="props.comment.mentionUsers"
        :initial-attachments="props.comment.attachments"
        :loading="props.submitLoading"
        :enable-attachment="props.enableAttachment"
        :submit-text="t('crmComment.save')"
        @submit="handleEditSubmit"
        @cancel="emit('cancelEditor')"
      />

      <div v-else class="crm-comment-item__content">
        <span v-if="props.comment.replyToUserName" class="text-[var(--text-n4)]">
          {{ t('crmComment.replyTo') }} {{ props.comment.replyToUserName }}：
        </span>
        <span
          v-for="(segment, index) in contentSegments"
          :key="`${segment.text}-${index}`"
          :class="segment.mention ? 'crm-comment-item__mention' : 'text-[var(--text-n1)]'"
        >
          {{ segment.text }}
        </span>
      </div>

      <CrmFileList
        v-if="!isEditing && props.comment.attachments?.length"
        :files="props.comment.attachments"
        class="mt-[8px]"
        readonly
      />

      <div v-if="!isEditing && !props.actionsInMore" class="mt-[8px] flex justify-end gap-[8px]">
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
  import { NButton, NTooltip } from 'naive-ui';
  import dayjs from 'dayjs';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { splitCommentMentions } from '@lib/shared/method/comment';
  import type {
    FollowCommentActionValue,
    FollowCommentActiveEditor,
    FollowCommentItem,
    FollowCommentSubmitValue,
  } from '@lib/shared/models/follow';

  import CrmMoreAction from '@/components/pure/crm-more-action/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmAvatar from '@/components/business/crm-avatar/index.vue';
  import CrmFileList from '@/components/business/crm-file-list/index.vue';
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
      enableAttachment?: boolean;
      actionsInMore?: boolean;
    }>(),
    {
      level: 1,
      activeEditor: null,
      canReply: true,
      canEdit: true,
      canDelete: true,
      enableAttachment: false,
      actionsInMore: false,
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
  const contentSegments = computed(() => splitCommentMentions(props.comment.content, props.comment.mentionUsers));

  const moreActions = computed<ActionsItem[]>(() => [
    ...(props.canReply ? [{ key: 'reply', label: t('crmComment.reply') }] : []),
    ...(props.canEdit ? [{ key: 'edit', label: t('crmComment.edit') }] : []),
    ...(props.canDelete ? [{ key: 'delete', label: t('crmComment.delete'), danger: true }] : []),
  ]);

  function handleMoreActionSelect(action: ActionsItem) {
    if (action.key === 'reply') {
      emit('reply', props.comment);
    } else if (action.key === 'edit') {
      emit('edit', props.comment);
    } else if (action.key === 'delete') {
      emit('delete', props.comment);
    }
  }

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
    overflow-wrap: anywhere;
  }
  .crm-comment-item__mention {
    display: inline;
    padding: 0 4px;
    border-radius: 2px;
    color: var(--text-n1);
    background: var(--text-n8);
    box-decoration-break: clone;
    box-decoration-break: clone;
    word-break: break-all;
  }
</style>
