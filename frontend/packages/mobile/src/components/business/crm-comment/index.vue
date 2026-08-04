<template>
  <div class="crm-comment mt-[16px]">
    <CommentHeader :count="commentCount" :title="props.title" :show-add="props.showAdd" @add="openCreateEditor" />
    <div :class="['crm-comment-body', { 'crm-comment-body--editing': activeEditor }]">
      <CommentList
        :comments="props.comments"
        :default-reply-count="props.defaultReplyCount"
        :can-reply="props.canReply"
        :can-edit="props.canEdit"
        :can-delete="props.canDelete"
        @reply="openReplyEditor"
        @edit="openEditEditor"
        @delete="emit('delete', $event)"
      />
    </div>
    <CommentEditor
      v-if="activeEditor"
      v-model:value="editorContent"
      class="crm-comment-fixed-editor"
      :mode="activeEditor.action"
      :reply-user-name="fixedEditorReplyUserName"
      :loading="props.submitLoading"
      @submit="handleSubmit"
      @cancel="closeEditor"
    />
  </div>
</template>

<script setup lang="ts">
  import type {
    FollowCommentActiveEditor,
    FollowCommentItem,
    FollowCommentSubmitValue,
  } from '@lib/shared/models/follow';

  import CommentEditor from './components/commentEditor.vue';
  import CommentHeader from './components/commentHeader.vue';
  import CommentList from './components/commentList.vue';

  const props = withDefaults(
    defineProps<{
      comments?: FollowCommentItem[];
      count?: number;
      title?: string;
      defaultReplyCount?: number;
      showAdd?: boolean;
      canReply?: boolean;
      canEdit?: boolean;
      canDelete?: boolean;
      submitLoading?: boolean;
    }>(),
    {
      comments: () => [],
      count: undefined,
      title: '',
      defaultReplyCount: 1,
      showAdd: true,
      canReply: true,
      canEdit: true,
      canDelete: true,
      submitLoading: false,
    }
  );

  const emit = defineEmits<{
    (e: 'create', value: FollowCommentSubmitValue): void;
    (e: 'reply', value: FollowCommentSubmitValue, comment: FollowCommentItem): void;
    (e: 'edit', value: FollowCommentSubmitValue, comment: FollowCommentItem): void;
    (e: 'delete', comment: FollowCommentItem): void;
    (e: 'changeEditor', editor: FollowCommentActiveEditor | null): void;
  }>();

  const activeEditor = ref<FollowCommentActiveEditor | null>(null);
  const editorContent = ref('');

  const commentCount = computed(() => {
    if (props.count !== undefined) {
      return props.count;
    }
    return props.comments.reduce((total, comment) => total + 1 + (comment.replies?.length || 0), 0);
  });

  function findCommentById(commentId?: string) {
    if (!commentId) {
      return undefined;
    }
    return (
      props.comments.find((comment) => comment.id === commentId) ||
      props.comments.flatMap((comment) => comment.replies || []).find((comment) => comment.id === commentId)
    );
  }

  const fixedEditorReplyUserName = computed(() => {
    if (activeEditor.value?.action !== 'reply') {
      return '';
    }
    return findCommentById(activeEditor.value.commentId)?.createUserName || '';
  });

  function setActiveEditor(editor: FollowCommentActiveEditor | null, content = '') {
    activeEditor.value = editor;
    editorContent.value = content;
    emit('changeEditor', editor);
  }

  function openCreateEditor() {
    setActiveEditor({
      action: 'create',
    });
  }

  function openReplyEditor(comment: FollowCommentItem) {
    setActiveEditor({
      action: 'reply',
      commentId: comment.id,
    });
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

  function handleSubmit(value: FollowCommentSubmitValue) {
    if (!activeEditor.value) {
      return;
    }

    if (activeEditor.value.action === 'create') {
      emit('create', value);
      closeEditor();
      return;
    }

    const activeComment = findCommentById(activeEditor.value.commentId);
    if (activeEditor.value.action === 'reply' && activeComment) {
      emit('reply', value, activeComment);
      closeEditor();
      return;
    }

    if (activeEditor.value.action === 'edit' && activeComment) {
      emit('edit', value, activeComment);
      closeEditor();
    }
  }
</script>

<style scoped lang="less">
  .crm-comment {
    width: 100%;
  }
  .crm-comment-body {
    background: var(--text-n10);
  }
  .crm-comment-body--editing {
    padding-bottom: calc(58px + env(safe-area-inset-bottom));
  }
  .crm-comment-fixed-editor {
    position: fixed;
    right: 0;
    bottom: 0;
    left: 0;
    z-index: 100;
    padding: 8px 12px calc(8px + env(safe-area-inset-bottom));
    background: var(--text-n10);
    box-shadow: 0 -1px 6px rgb(0 0 0 / 4%);
  }
</style>
