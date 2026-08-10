<template>
  <CommentPanel
    v-model:expanded="expanded"
    :comments="comments"
    :comment-count="commentCount"
    :has-more="hasMore"
    :loading="loading"
    :submit-loading="submitLoading"
    @create-submit="handleCreateComment"
    @reply-submit="handleReplyComment"
    @edit-submit="editComment"
    @delete="handleDeleteComment"
    @reach-bottom="() => loadComments()"
  />
</template>

<script setup lang="ts">
  import CommentPanel from './commentPanel.vue';

  import useCommentResource, { type CommentResourceType } from './useCommentResource';

  const props = defineProps<{
    type: CommentResourceType;
    sourceId: string;
    initialCount?: number;
  }>();

  const expanded = defineModel<boolean>('expanded', {
    default: false,
  });

  const emit = defineEmits<{
    (e: 'refresh'): void;
  }>();

  const {
    comments,
    commentCount,
    loading,
    submitLoading,
    hasMore,
    initComments,
    loadComments,
    createComment,
    replyComment,
    editComment,
    deleteComment,
  } = useCommentResource({
    type: computed(() => props.type),
    sourceId: computed(() => props.sourceId),
    initialCount: computed(() => props.initialCount),
  });

  watch(
    () => [expanded.value, props.type, props.sourceId],
    ([isExpanded]) => {
      if (isExpanded) {
        initComments();
      }
    },
    {
      immediate: true,
    }
  );

  async function handleCreateComment(value: Parameters<typeof createComment>[0]) {
    await createComment(value);
    emit('refresh');
  }

  async function handleReplyComment(value: Parameters<typeof replyComment>[0]) {
    await replyComment(value);
    emit('refresh');
  }

  async function handleDeleteComment(comment: Parameters<typeof deleteComment>[0]) {
    await deleteComment(comment);
    emit('refresh');
  }
</script>
