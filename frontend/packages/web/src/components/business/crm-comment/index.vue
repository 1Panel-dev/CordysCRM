<template>
  <CommentPanel
    v-model:expanded="expanded"
    :comments="comments"
    :comment-count="count"
    :has-more="hasMore"
    :loading="loading"
    :submit-loading="submitLoading"
    :can-create="props.canCreate"
    :can-reply="props.canReply"
    :enable-attachment="props.enableAttachment"
    :show-time-sort="props.showTimeSort"
    :sort-order="sortOrder"
    @create-submit="handleCreateComment"
    @reply-submit="handleReplyComment"
    @edit-submit="editComment"
    @delete="handleDeleteComment"
    @reach-bottom="() => loadComments()"
    @sort-change="handleSortChange"
  />
</template>

<script setup lang="ts">
  import CommentPanel from './commentPanel.vue';

  import useCommentResource, { type CommentResourceType } from './useCommentResource';

  const props = withDefaults(
    defineProps<{
      type?: CommentResourceType;
      sourceId: string;
      canCreate?: boolean;
      canReply?: boolean;
      enableAttachment?: boolean;
      showTimeSort?: boolean;
    }>(),
    {
      canCreate: true,
      canReply: true,
      showTimeSort: false,
    }
  );

  const count = defineModel<number>('count', {
    default: 0,
  });

  const expanded = defineModel<boolean>('expanded', {
    default: false,
  });

  const sortOrder = ref<'asc' | 'desc'>('desc');

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
    type: computed(() => props.type || 'followRecord'),
    sourceId: computed(() => props.sourceId),
    initialCount: computed(() => count.value),
    sortOrder,
    enableSort: computed(() => props.showTimeSort),
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

  function handleSortChange(value: 'asc' | 'desc') {
    sortOrder.value = value;
    if (expanded.value) {
      loadComments(true);
    }
  }
</script>
