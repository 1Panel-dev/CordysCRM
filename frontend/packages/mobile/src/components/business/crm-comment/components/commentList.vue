<template>
  <div class="crm-comment-list">
    <div v-for="(comment, index) in props.comments" :key="comment.id" class="crm-comment-list-item">
      <CommentItem
        :comment="comment"
        :can-reply="props.canReply"
        :can-edit="props.canEdit"
        :can-delete="props.canDelete"
        :show-divider="hasReplies(comment) || index < props.comments.length - 1"
        @reply="emit('reply', $event)"
        @edit="emit('edit', $event)"
        @delete="emit('delete', $event)"
      >
        <template #editor>
          <slot name="editor" :comment="comment" :level="1" />
        </template>
      </CommentItem>

      <div v-if="hasReplies(comment)" class="crm-comment-list-replies">
        <CommentItem
          v-for="(reply, replyIndex) in getVisibleReplies(comment)"
          :key="reply.id"
          :comment="reply"
          :level="2"
          :can-reply="props.canReply"
          :can-edit="props.canEdit"
          :can-delete="props.canDelete"
          :show-divider="shouldShowReplyDivider(comment, replyIndex)"
          @reply="emit('reply', $event)"
          @edit="emit('edit', $event)"
          @delete="emit('delete', $event)"
        >
          <template #editor>
            <slot name="editor" :comment="reply" :parent-comment="comment" :level="2" />
          </template>
        </CommentItem>

        <button
          v-if="shouldShowMoreReplies(comment)"
          class="crm-comment-list-toggle"
          type="button"
          @click.stop="expandReplies(comment.id)"
        >
          <CrmIcon name="iconicon_chevron_right" width="16px" height="16px" color="var(--text-n4)" />
          <div>{{ t('crmComment.moreReplies', { count: getMoreReplyCount(comment) }) }}</div>
        </button>

        <button
          v-if="shouldShowCollapseReplies(comment)"
          class="crm-comment-list-toggle"
          type="button"
          @click.stop="collapseReplies(comment.id)"
        >
          <CrmIcon name="iconicon_chevron_up" width="16px" height="16px" color="var(--text-n4)" />
          <span>{{ t('crmComment.collapseReplies') }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { FollowCommentItem } from '@lib/shared/models/follow';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CommentItem from './commentItem.vue';

  const props = withDefaults(
    defineProps<{
      comments?: FollowCommentItem[];
      defaultReplyCount?: number;
      canReply?: boolean;
      canEdit?: boolean;
      canDelete?: boolean;
    }>(),
    {
      comments: () => [],
      defaultReplyCount: 1,
      canReply: true,
      canEdit: true,
      canDelete: true,
    }
  );

  const emit = defineEmits<{
    (e: 'reply', comment: FollowCommentItem): void;
    (e: 'edit', comment: FollowCommentItem): void;
    (e: 'delete', comment: FollowCommentItem): void;
  }>();

  const { t } = useI18n();

  const expandedCommentIds = ref<string[]>([]);

  function getReplies(comment: FollowCommentItem) {
    return comment.replies || [];
  }

  function hasReplies(comment: FollowCommentItem) {
    return getReplies(comment).length > 0;
  }

  function isExpanded(commentId: string) {
    return expandedCommentIds.value.includes(commentId);
  }

  function getVisibleReplies(comment: FollowCommentItem) {
    const replies = getReplies(comment);
    if (isExpanded(comment.id)) {
      return replies;
    }
    return replies.slice(0, props.defaultReplyCount);
  }

  function getMoreReplyCount(comment: FollowCommentItem) {
    return Math.max(getReplies(comment).length - props.defaultReplyCount, 0);
  }

  function shouldShowMoreReplies(comment: FollowCommentItem) {
    return !isExpanded(comment.id) && getMoreReplyCount(comment) > 0;
  }

  function shouldShowCollapseReplies(comment: FollowCommentItem) {
    return isExpanded(comment.id) && getReplies(comment).length > props.defaultReplyCount;
  }

  function shouldShowReplyDivider(comment: FollowCommentItem, replyIndex: number) {
    const isLastVisibleReply = replyIndex === getVisibleReplies(comment).length - 1;
    const hasToggleAfterReply = shouldShowMoreReplies(comment) || shouldShowCollapseReplies(comment);
    return !(hasToggleAfterReply && isLastVisibleReply);
  }

  function expandReplies(commentId: string) {
    if (!isExpanded(commentId)) {
      expandedCommentIds.value = [...expandedCommentIds.value, commentId];
    }
  }

  function collapseReplies(commentId: string) {
    expandedCommentIds.value = expandedCommentIds.value.filter((id) => id !== commentId);
  }
</script>

<style scoped lang="less">
  .crm-comment-list {
    width: 100%;
  }
  .crm-comment-list-item {
    width: 100%;
  }
  .crm-comment-list-replies {
    width: 100%;
  }
  .crm-comment-list-toggle {
    position: relative;
    display: flex;
    align-items: center;
    margin: 8px 0 0;
    padding: 0 0 12px 56px;
    width: 100%;
    border: 0;
    color: var(--text-n4);
    background: transparent;
    gap: 4px;
  }
  .crm-comment-list-toggle::after {
    position: absolute;
    right: 0;
    bottom: 0;
    left: 0;
    height: 1px;
    background: var(--text-n8);
    content: '';
    transform: scaleY(0.5);
    transform-origin: 0 0;
  }
</style>
