<template>
  <CommentPanel
    v-model:expanded="expanded"
    :comments="comments"
    :comment-count="commentCount"
    :has-more="hasMore"
    :loading="loading"
    :submit-loading="submitLoading"
    @create-submit="createComment"
    @reply-submit="replyComment"
    @edit-submit="editComment"
    @delete="deleteComment"
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
</script>
