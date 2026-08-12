<template>
  <CommentPanel
    v-model:expanded="expanded"
    :comments="comments"
    :comment-count="count"
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
  }>();

  const count = defineModel<number>('count', {
    default: 0,
  });

  const expanded = defineModel<boolean>('expanded', {
    default: false,
  });

  const {
    comments,
    commentCount: resourceCommentCount,
    loading,
    submitLoading,
    hasMore,
    loadComments,
    createComment,
    replyComment,
    editComment,
    deleteComment,
  } = useCommentResource({
    type: computed(() => props.type),
    sourceId: computed(() => props.sourceId),
    initialCount: computed(() => count.value),
  });

  watch(resourceCommentCount, (value) => {
    count.value = value;
  });

  watch(
    () => [expanded.value, props.type, props.sourceId],
    ([isExpanded]) => {
      if (isExpanded) {
        loadComments(true);
      }
    },
    {
      immediate: true,
    }
  );

  async function handleCreateComment(value: Parameters<typeof createComment>[0]) {
    await createComment(value);
  }

  async function handleReplyComment(value: Parameters<typeof replyComment>[0]) {
    await replyComment(value);
  }

  async function handleDeleteComment(comment: Parameters<typeof deleteComment>[0]) {
    await deleteComment(comment);
  }
</script>
