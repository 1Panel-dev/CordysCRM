<template>
  <div class="crm-comment border-1 border-b-[var(--text-n8)]">
    <div class="crm-comment__header">
      <div class="flex cursor-pointer items-center gap-[8px]">
        <n-button
          quaternary
          class="flex !px-[4px] !text-[14px]"
          size="small"
          :disabled="!comments.length"
          @click="toggleExpanded"
        >
          <template #icon>
            <CrmIcon
              :type="expanded ? 'iconicon_chevron_down' : 'iconicon_chevron_right'"
              :size="16"
              class="mr-[8px] text-[var(--text-n4)]"
            />
          </template>
          <div class="flex items-center gap-[8px] text-[var(--text-n1)]">
            <CrmIcon type="iconicon_comment" :size="16" class="text-[var(--text-n1)]" />
            {{ t('crmComment.title') }}
          </div>
        </n-button>

        <CrmTag>
          {{ displayCount }}
        </CrmTag>
      </div>

      <n-button v-if="props.canCreate" type="default" class="outline--secondary" @click="handleCreate">
        <CrmIcon class="mr-[8px] text-[var(--text-n1)]" type="iconicon_add" :size="16" />
        {{ t('crmComment.addComment') }}
      </n-button>
    </div>

    <n-divider v-if="showHeaderDivider" class="crm-comment__header-divider" />

    <div v-if="expanded" class="crm-comment__body">
      <MentionInput
        v-if="activeEditor?.action === 'create'"
        class="mb-[12px]"
        :loading="props.submitLoading"
        @submit="handleCreateSubmit"
        @cancel="closeEditor"
      />

      <CommentList
        :active-editor="listActiveEditor"
        :can-delete="props.canDelete"
        :can-edit="props.canEdit"
        :can-reply="props.canReply"
        :comments="props.comments"
        :submit-loading="props.submitLoading"
        @reply="handleReply"
        @edit="handleEdit"
        @delete="emit('delete', $event)"
        @reply-submit="handleReplySubmit"
        @edit-submit="handleEditSubmit"
        @cancel-editor="closeEditor"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
  import { NButton, NDivider } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { formatBadgeCount } from '@lib/shared/method';
  import type {
    FollowCommentActionValue,
    FollowCommentActiveEditor,
    FollowCommentItem,
    FollowCommentSubmitValue,
  } from '@lib/shared/models/follow';

  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import CommentList from './components/commentList.vue';
  import MentionInput from './components/mentionInput.vue';

  const { t } = useI18n();

  type FollowCommentListActiveEditor = {
    action: 'reply' | 'edit';
    commentId: string;
  };

  const props = withDefaults(
    defineProps<{
      comments?: FollowCommentItem[];
      commentCount?: number;
      canCreate?: boolean;
      submitLoading?: boolean;
      canReply?: (comment: FollowCommentItem) => boolean;
      canEdit?: (comment: FollowCommentItem) => boolean;
      canDelete?: (comment: FollowCommentItem) => boolean;
    }>(),
    {
      comments: () => [],
      canCreate: true,
    }
  );

  const emit = defineEmits<{
    (event: 'createSubmit', value: FollowCommentSubmitValue): void;
    (event: 'replySubmit', value: FollowCommentActionValue): void;
    (event: 'editSubmit', value: FollowCommentActionValue): void;
    (event: 'delete', comment: FollowCommentItem): void;
  }>();

  const expanded = defineModel<boolean>('expanded', {
    default: false,
  });

  const activeEditor = ref<FollowCommentActiveEditor | null>(null);

  function closeEditor() {
    activeEditor.value = null;
  }

  function getLocalCommentCount() {
    return props.comments.reduce((total, comment) => total + 1 + (comment.replies?.length || 0), 0);
  }

  const listActiveEditor = computed<FollowCommentListActiveEditor | null>(() => {
    if (activeEditor.value?.action === 'reply' || activeEditor.value?.action === 'edit') {
      return activeEditor.value as FollowCommentListActiveEditor;
    }
    return null;
  });

  const displayCount = computed(() => {
    const count = props.commentCount ?? getLocalCommentCount();
    return formatBadgeCount(count);
  });

  const showHeaderDivider = computed(() => !expanded.value);

  function toggleExpanded() {
    expanded.value = !expanded.value;
    if (!expanded.value) {
      closeEditor();
    }
  }

  function handleCreate() {
    expanded.value = true;
    activeEditor.value = activeEditor.value?.action === 'create' ? null : { action: 'create' };
  }

  function handleReply(comment: FollowCommentItem) {
    expanded.value = true;
    activeEditor.value = {
      action: 'reply',
      commentId: comment.id,
    };
  }

  function handleEdit(comment: FollowCommentItem) {
    expanded.value = true;
    activeEditor.value = {
      action: 'edit',
      commentId: comment.id,
    };
  }

  function handleCreateSubmit(value: FollowCommentSubmitValue) {
    emit('createSubmit', value);
    closeEditor();
  }

  function handleReplySubmit(value: FollowCommentActionValue) {
    emit('replySubmit', value);
    closeEditor();
  }

  function handleEditSubmit(value: FollowCommentActionValue) {
    emit('editSubmit', value);
    closeEditor();
  }

  defineExpose({
    closeEditor,
  });
</script>

<style scoped lang="less">
  .crm-comment {
    width: 100%;
  }
  .crm-comment__header {
    gap: 12px;
    @apply flex items-center justify-between;
  }
  .crm-comment__body {
    margin-top: 12px;
  }
  .crm-comment__header-divider {
    margin: 12px 0 0;
    :deep(.n-divider__line) {
      background-color: var(--text-n8);
    }
  }
</style>
