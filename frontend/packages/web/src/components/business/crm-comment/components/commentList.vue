<template>
  <div class="crm-comment-list">
    <template v-for="comment in props.comments" :key="comment.id">
      <div>
        <CommentItem
          :active-editor="getParentCommentEditor(comment)"
          :can-delete="canDelete(comment)"
          :can-edit="canEdit(comment)"
          :can-reply="canOperateComment"
          :comment="comment"
          :submit-loading="props.submitLoading"
          @reply="handleReply"
          @edit="handleEdit"
          @delete="handleDelete"
          @reply-submit="handleReplySubmit"
          @edit-submit="handleEditSubmit"
          @cancel-editor="emit('cancelEditor')"
        />

        <div v-if="hasReplyContent(comment)" class="crm-comment-list__replies">
          <n-divider v-if="!isReplyingParentComment(comment)" class="crm-comment-list__divider" />

          <MentionInput
            v-if="isReplyingParentComment(comment)"
            class="crm-comment-list__parent-reply-input mb-[8px]"
            :loading="props.submitLoading"
            :reply-user-name="comment.createUserName"
            @submit="(value) => handleParentReplySubmit(comment, value)"
            @cancel="emit('cancelEditor')"
          />

          <template v-for="reply in getVisibleReplies(comment)" :key="reply.id">
            <CommentItem
              :active-editor="props.activeEditor"
              :can-delete="canDelete(reply)"
              :can-edit="canEdit(reply)"
              :can-reply="canOperateComment"
              :comment="reply"
              :submit-loading="props.submitLoading"
              :level="2"
              @reply="handleReply"
              @edit="handleEdit"
              @delete="handleDelete"
              @reply-submit="handleReplySubmit"
              @edit-submit="handleEditSubmit"
              @cancel-editor="emit('cancelEditor')"
            />
            <n-divider
              v-if="shouldShowReplyDivider(comment)"
              class="crm-comment-list__divider crm-comment-list__reply-divider"
            />
          </template>
          <div v-if="shouldShowMoreReplies(comment) || shouldShowCollapseReplies(comment)" class="ml-[40px] p-[16px]">
            <n-button
              v-if="shouldShowMoreReplies(comment)"
              size="small"
              quaternary
              class="!px-[4px]"
              @click="expandReplies(comment.id)"
            >
              <template #icon>
                <CrmIcon type="iconicon_chevron_right" :size="16" class="cursor-pointer text-[var(--text-n4)]" />
              </template>
              <div class="!text-[14px] text-[var(--text-n1)]">
                {{ t('crmComment.moreReplies', { count: getHiddenReplyCount(comment) }) }}
              </div>
            </n-button>

            <n-button
              v-if="shouldShowCollapseReplies(comment)"
              size="small"
              quaternary
              class="!px-[4px]"
              @click="collapseReplies(comment.id)"
            >
              <template #icon>
                <CrmIcon type="iconicon_chevron_up" :size="16" class="cursor-pointer text-[var(--text-n4)]" />
              </template>
              <div class="!text-[14px] text-[var(--text-n1)]"> {{ t('crmComment.collapseReplies') }}</div>
            </n-button>
          </div>
        </div>
      </div>

      <n-divider class="crm-comment-list__divider" />
    </template>
  </div>
</template>

<script setup lang="ts">
  import { NButton, NDivider } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { FOLLOW_COMMENT_OPERATE_PERMISSIONS } from '@lib/shared/method/comment';
  import type {
    FollowCommentActionValue,
    FollowCommentActiveEditor,
    FollowCommentItem,
    FollowCommentSubmitValue,
  } from '@lib/shared/models/follow';

  import CommentItem from './commentItem.vue';
  import MentionInput from './mentionInput.vue';

  import useUserStore from '@/store/modules/user';
  import { hasAnyPermission } from '@/utils/permission';

  const { t } = useI18n();

  const props = withDefaults(
    defineProps<{
      comments?: FollowCommentItem[];
      activeEditor?: FollowCommentActiveEditor | null;
      submitLoading?: boolean;
    }>(),
    {
      comments: () => [],
      activeEditor: null,
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

  const expandedCommentIds = ref<string[]>([]);
  const userStore = useUserStore();
  const canOperateComment = computed(() => hasAnyPermission(FOLLOW_COMMENT_OPERATE_PERMISSIONS));

  function getReplies(comment: FollowCommentItem) {
    return comment.replies || [];
  }

  function isExpanded(commentId: string) {
    return expandedCommentIds.value.includes(commentId);
  }

  function isReplyingParentComment(comment: FollowCommentItem) {
    return props.activeEditor?.action === 'reply' && props.activeEditor.commentId === comment.id;
  }

  function hasReplyContent(comment: FollowCommentItem) {
    return getReplies(comment).length > 0 || isReplyingParentComment(comment);
  }

  function getVisibleReplies(comment: FollowCommentItem) {
    const replies = getReplies(comment);
    if (isExpanded(comment.id)) {
      return replies;
    }
    return replies.slice(0, 1);
  }

  function getHiddenReplyCount(comment: FollowCommentItem) {
    return Math.max(getReplies(comment).length - 1, 0);
  }

  function shouldShowMoreReplies(comment: FollowCommentItem) {
    return getHiddenReplyCount(comment) > 0 && !isExpanded(comment.id);
  }

  function shouldShowCollapseReplies(comment: FollowCommentItem) {
    return getReplies(comment).length > 1 && isExpanded(comment.id);
  }

  function shouldShowReplyDivider(comment: FollowCommentItem) {
    return getReplies(comment).length > 1;
  }

  function expandReplies(commentId: string) {
    if (!isExpanded(commentId)) {
      expandedCommentIds.value.push(commentId);
    }
  }

  function collapseReplies(commentId: string) {
    expandedCommentIds.value = expandedCommentIds.value.filter((id) => id !== commentId);
  }

  function getParentCommentEditor(comment: FollowCommentItem) {
    if (isReplyingParentComment(comment)) {
      return null;
    }
    return props.activeEditor;
  }

  const isCommentOwner = (comment: FollowCommentItem) => comment.createUser === userStore.userInfo.id;

  const canEdit = (comment: FollowCommentItem) => isCommentOwner(comment);

  const canDelete = (comment: FollowCommentItem) => isCommentOwner(comment);

  function handleReply(comment: FollowCommentItem) {
    if (!canOperateComment.value) {
      return;
    }
    emit('reply', comment);
  }

  function handleEdit(comment: FollowCommentItem) {
    if (!canEdit(comment)) {
      return;
    }
    emit('edit', comment);
  }

  function handleDelete(comment: FollowCommentItem) {
    if (!canDelete(comment)) {
      return;
    }
    emit('delete', comment);
  }

  function handleParentReplySubmit(comment: FollowCommentItem, value: FollowCommentSubmitValue) {
    if (!canOperateComment.value) {
      return;
    }

    emit('replySubmit', {
      comment,
      ...value,
    });
  }

  function handleReplySubmit(value: FollowCommentActionValue) {
    if (!canOperateComment.value) {
      return;
    }
    emit('replySubmit', value);
  }

  function handleEditSubmit(value: FollowCommentActionValue) {
    if (!canEdit(value.comment)) {
      return;
    }
    emit('editSubmit', value);
  }
</script>

<style scoped lang="less">
  .crm-comment-list {
    width: 100%;
  }
  .crm-comment-list__divider {
    margin: 0;
    :deep(.n-divider__line) {
      background-color: var(--text-n8);
    }
  }
  .crm-comment-list__reply-divider {
    margin-left: 56px;
    width: calc(100% - 56px);
  }
  .crm-comment-list__parent-reply-input {
    margin-left: 70px;
    width: calc(100% - 70px);
  }
</style>
