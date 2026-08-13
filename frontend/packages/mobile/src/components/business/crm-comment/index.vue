<template>
  <div class="crm-comment h-full">
    <CommentHeader :count="commentCount" :title="props.title" :show-add="canOperateComment" @add="openCreateEditor" />
    <div :class="['crm-comment-body', { 'crm-comment-body--editing': activeEditor }]">
      <CrmList
        :key="`${props.type}-${props.sourceId}`"
        ref="commentListRef"
        :model-value="comments"
        class="crm-comment-list min-h-0 flex-1"
        :item-gap="0"
        :load-list-api="handleLoadCommentList"
        :close-init-load="!props.sourceId"
        @update:model-value="handleCommentsUpdate"
      >
        <template #item="{ item }">
          <div class="crm-comment-list-item">
            <CommentItem
              :comment="item"
              :can-reply="canOperateComment"
              :can-edit="canEditComment(item)"
              :can-delete="canDeleteComment(item)"
              :show-divider="hasReplies(item) || !isLastComment(item)"
              @reply="openReplyEditor"
              @edit="openEditEditor"
              @delete="handleDelete"
            />

            <div v-if="hasReplies(item)" class="crm-comment-list-replies">
              <CommentItem
                v-for="(reply, replyIndex) in getVisibleReplies(item)"
                :key="reply.id"
                :comment="reply"
                :level="2"
                :can-reply="canOperateComment"
                :can-edit="isCommentOwner(reply)"
                :can-delete="isCommentOwner(reply)"
                :show-divider="shouldShowReplyDivider(item, replyIndex)"
                @reply="openReplyEditor"
                @edit="openEditEditor"
                @delete="handleDelete"
              />

              <button
                v-if="shouldShowMoreReplies(item)"
                class="crm-comment-list-toggle"
                type="button"
                @click.stop="expandReplies(item.id)"
              >
                <CrmIcon name="iconicon_chevron_right" width="16px" height="16px" color="var(--text-n4)" />
                <div>{{ t('crmComment.moreReplies', { count: getMoreReplyCount(item) }) }}</div>
              </button>

              <button
                v-if="shouldShowCollapseReplies(item)"
                class="crm-comment-list-toggle"
                type="button"
                @click.stop="collapseReplies(item.id)"
              >
                <CrmIcon name="iconicon_chevron_up" width="16px" height="16px" color="var(--text-n4)" />
                <span>{{ t('crmComment.collapseReplies') }}</span>
              </button>
            </div>
          </div>
        </template>
      </CrmList>
    </div>
    <div v-if="activeEditor" class="crm-comment-editor-wrapper">
      <CommentEditor
        ref="commentEditorRef"
        v-model:value="editorContent"
        :mode="activeEditor.action"
        :initial-mention-users="initialEditorMentionUsers"
        :reply-user-name="fixedEditorReplyUserName"
        :loading="submitLoading"
        @submit="handleSubmit"
        @cancel="closeEditor"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { FOLLOW_COMMENT_OPERATE_PERMISSIONS } from '@lib/shared/method/comment';
  import type { TableQueryParams } from '@lib/shared/models/common';
  import type {
    FollowCommentActiveEditor,
    FollowCommentItem,
    FollowCommentPageResult,
    FollowCommentSubmitValue,
  } from '@lib/shared/models/follow';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmList from '@/components/pure/crm-list/index.vue';
  import CommentEditor from './components/commentEditor.vue';
  import CommentHeader from './components/commentHeader.vue';
  import CommentItem from './components/commentItem.vue';

  import useUserStore from '@/store/modules/user';
  import { hasAnyPermission } from '@/utils/permission';

  import useCommentResource, { type MobileCommentResourceType } from './useCommentResource';

  const props = withDefaults(
    defineProps<{
      type: MobileCommentResourceType;
      sourceId: string;
      title?: string;
      defaultReplyCount?: number;
    }>(),
    {
      title: '',
      defaultReplyCount: 1,
    }
  );

  const emit = defineEmits<{
    (e: 'changeEditor', editor: FollowCommentActiveEditor | null): void;
  }>();

  const { t } = useI18n();
  const userStore = useUserStore();

  const activeEditor = ref<FollowCommentActiveEditor | null>(null);
  const editorContent = ref('');
  const comments = ref<FollowCommentItem[]>([]);
  const commentListRef = ref<InstanceType<typeof CrmList>>();
  const commentEditorRef = ref<InstanceType<typeof CommentEditor>>();
  const count = defineModel<number>('count', {
    default: 0,
  });
  const expandedCommentIds = ref<string[]>([]);
  const acceptEmptyCommentList = ref(false);

  const { submitLoading, loadCommentList, createComment, replyComment, editComment, deleteComment } =
    useCommentResource({
      type: toRef(props, 'type'),
      sourceId: toRef(props, 'sourceId'),
    });

  const commentCount = computed(() => count.value);
  const canOperateComment = computed(() => hasAnyPermission(FOLLOW_COMMENT_OPERATE_PERMISSIONS));

  const isCommentOwner = (comment: FollowCommentItem) => comment.createUser === userStore.userInfo.id;

  const canEditComment = (comment: FollowCommentItem) => isCommentOwner(comment);

  const canDeleteComment = (comment: FollowCommentItem) => isCommentOwner(comment);

  async function handleLoadCommentList(params: TableQueryParams): Promise<FollowCommentPageResult> {
    const result = await loadCommentList(params);
    count.value = result.commentCount;
    acceptEmptyCommentList.value = (params.current || 1) === 1 && result.list.length === 0;
    return result;
  }

  function handleCommentsUpdate(items: FollowCommentItem[]) {
    if (items.length || comments.value.length === 0 || acceptEmptyCommentList.value) {
      comments.value = items;
      acceptEmptyCommentList.value = false;
    }
  }

  function findCommentById(commentId?: string) {
    if (!commentId) {
      return undefined;
    }
    return (
      comments.value.find((comment) => comment.id === commentId) ||
      comments.value.flatMap((comment) => comment.replies || []).find((comment) => comment.id === commentId)
    );
  }

  const fixedEditorReplyUserName = computed(() => {
    if (activeEditor.value?.action !== 'reply') {
      return '';
    }
    return findCommentById(activeEditor.value.commentId)?.createUserName || '';
  });

  const initialEditorMentionUsers = computed(() => {
    if (activeEditor.value?.action !== 'edit') {
      return [];
    }
    return findCommentById(activeEditor.value.commentId)?.mentionUsers || [];
  });

  function setActiveEditor(editor: FollowCommentActiveEditor | null, content = '') {
    activeEditor.value = editor;
    editorContent.value = content;
    emit('changeEditor', editor);
  }

  function focusCommentEditor() {
    nextTick(() => {
      commentEditorRef.value?.focus();
    });
  }

  function openCreateEditor() {
    setActiveEditor({
      action: 'create',
    });
    focusCommentEditor();
  }

  function openReplyEditor(comment: FollowCommentItem) {
    setActiveEditor({
      action: 'reply',
      commentId: comment.id,
    });
    focusCommentEditor();
  }

  function openEditEditor(comment: FollowCommentItem) {
    setActiveEditor(
      {
        action: 'edit',
        commentId: comment.id,
      },
      comment.content
    );
  }

  function closeEditor() {
    setActiveEditor(null);
  }

  async function reloadComments() {
    await commentListRef.value?.loadList(true);
  }

  function getReplies(comment: FollowCommentItem) {
    return comment.replies || [];
  }

  function hasReplies(comment: FollowCommentItem) {
    return getReplies(comment).length > 0;
  }

  function isLastComment(comment: FollowCommentItem) {
    return comments.value[comments.value.length - 1]?.id === comment.id;
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

  async function handleDelete(comment: FollowCommentItem) {
    if (!canDeleteComment(comment)) {
      return;
    }

    await deleteComment(comment);
    await reloadComments();
  }

  async function handleSubmit(value: FollowCommentSubmitValue) {
    if (!activeEditor.value) {
      return;
    }

    if (activeEditor.value.action === 'create') {
      if (!canOperateComment.value) {
        return;
      }

      await createComment(value);
      await reloadComments();
      closeEditor();
      return;
    }

    const activeComment = findCommentById(activeEditor.value.commentId);
    if (activeEditor.value.action === 'reply' && activeComment) {
      if (!canOperateComment.value) {
        return;
      }

      await replyComment(value, activeComment);
      await reloadComments();
      closeEditor();
      return;
    }

    if (activeEditor.value.action === 'edit' && activeComment) {
      if (!canEditComment(activeComment)) {
        return;
      }

      await editComment(value, activeComment);
      await reloadComments();
      closeEditor();
    }
  }

  watch(
    () => [props.type, props.sourceId],
    () => {
      // 同一路由切换不同跟进记录/计划时，不能沿用上一条的评论编辑状态。
      closeEditor();
      comments.value = [];
      expandedCommentIds.value = [];
    }
  );
</script>

<style scoped lang="less">
  .crm-comment {
    @apply flex min-h-0 flex-col overflow-hidden;
  }
  .crm-comment-body {
    @apply flex min-h-0 flex-1 flex-col overflow-hidden;

    background: var(--text-n10);
  }
  .crm-comment-list-toggle {
    @apply relative flex w-full items-center;

    margin: 8px 0 0;
    padding: 0 0 12px 56px;
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
  .crm-comment-editor-wrapper {
    @apply flex-none;

    padding: 0 16px;
    background: var(--text-n10);
    box-shadow: 0 -1px 6px rgb(0 0 0 / 4%);
  }
</style>
