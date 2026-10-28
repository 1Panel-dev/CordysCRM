<template>
  <div class="crm-comment border-b border-[var(--text-n8)]">
    <div class="crm-comment__header">
      <div class="flex items-center gap-[8px]">
        <n-button
          v-if="props.collapsible"
          quaternary
          class="flex !px-[4px] !text-[14px]"
          size="small"
          :disabled="!hasComment"
          @click="toggleExpanded"
        >
          <template #icon>
            <CrmIcon
              :type="expanded ? 'iconicon_chevron_down' : 'iconicon_chevron_right'"
              :size="16"
              class="mr-[8px] text-[var(--text-n4)]"
            />
          </template>
          <div class="flex items-center gap-[8px] font-semibold text-[var(--text-n1)]">
            <CrmIcon type="iconicon_comment" :size="16" class="text-[var(--text-n1)]" />
            {{ t('crmComment.title') }}
          </div>
        </n-button>
        <div v-else class="flex items-center gap-[8px] font-semibold text-[var(--text-n1)]">
          <CrmIcon type="iconicon_comment" :size="16" />
          {{ t('crmComment.title') }}
        </div>

        <span class="text-[var(--text-n4)]">{{ displayCount }}</span>
      </div>

      <div class="flex items-center gap-[8px]">
        <CrmTag v-if="props.showTimeSort" tooltipDisabled custom-class="cursor-pointer" @click="toggleSortOrder">
          <span class="flex items-center">
            {{ t('crmComment.timeSort') }}
            <span class="ml-[4px] flex flex-col">
              <CrmIcon
                type="iconicon_chevron_up"
                :size="16"
                class="mb-[-6px]"
                :class="props.sortOrder === 'asc' ? 'text-[var(--primary-1)]' : 'text-[var(--text-n4)]'"
              />
              <CrmIcon
                type="iconicon_chevron_down"
                :size="16"
                :class="props.sortOrder === 'desc' ? 'text-[var(--primary-1)]' : 'text-[var(--text-n4)]'"
              />
            </span>
          </span>
        </CrmTag>
        <n-button v-if="props.canCreate" size="small" type="primary" ghost @click="handleCreate">
          <template #icon>
            <CrmIcon type="iconicon_add" :size="16" />
          </template>
          {{ t('crmComment.addComment') }}
        </n-button>
      </div>
    </div>

    <n-divider v-if="showHeaderDivider" class="crm-comment__header-divider" />

    <div v-if="isExpanded" class="crm-comment__body">
      <MentionInput
        v-if="activeEditor?.action === 'create'"
        class="mb-[12px]"
        :loading="props.submitLoading"
        :enable-attachment="props.enableAttachment"
        :submit-text="t('common.confirm')"
        @submit="handleCreateSubmit"
        @cancel="handleCreateCancel"
      />

      <CommentList
        :active-editor="listActiveEditor"
        :comments="props.comments"
        :submit-loading="props.submitLoading"
        :can-reply="props.canReply"
        :enable-attachment="props.enableAttachment"
        :actions-in-more="props.actionsInMore"
        @reply="handleReply"
        @edit="handleEdit"
        @delete="emit('delete', $event)"
        @reply-submit="handleReplySubmit"
        @edit-submit="handleEditSubmit"
        @cancel-editor="closeEditor"
      />
      <div v-if="props.hasMore" ref="reachBottomTriggerRef" class="crm-comment__reach-bottom-trigger"></div>
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
      hasMore?: boolean;
      loading?: boolean;
      submitLoading?: boolean;
      canCreate?: boolean;
      canReply?: boolean;
      enableAttachment?: boolean;
      showTimeSort?: boolean;
      collapsible?: boolean;
      actionsInMore?: boolean;
      sortOrder?: 'asc' | 'desc';
    }>(),
    {
      comments: () => [],
      collapsible: true,
      actionsInMore: false,
    }
  );

  const emit = defineEmits<{
    (event: 'createSubmit', value: FollowCommentSubmitValue): void;
    (event: 'replySubmit', value: FollowCommentActionValue): void;
    (event: 'editSubmit', value: FollowCommentActionValue): void;
    (event: 'delete', comment: FollowCommentItem): void;
    (event: 'reachBottom'): void;
    (event: 'sortChange', value: 'asc' | 'desc'): void;
  }>();

  const expanded = defineModel<boolean>('expanded', {
    default: false,
  });

  const activeEditor = ref<FollowCommentActiveEditor | null>(null);
  const reachBottomTriggerRef = ref<HTMLElement | null>(null);
  let reachBottomObserver: IntersectionObserver | null = null;

  function closeEditor() {
    activeEditor.value = null;
  }

  const hasComment = computed(() => {
    return (props.commentCount || 0) > 0;
  });

  const isExpanded = computed(() => !props.collapsible || expanded.value);

  function handleCreateCancel() {
    closeEditor();
    if (props.collapsible && !hasComment.value) {
      expanded.value = false;
    }
  }

  const listActiveEditor = computed<FollowCommentListActiveEditor | null>(() => {
    if (activeEditor.value?.action === 'reply' || activeEditor.value?.action === 'edit') {
      return activeEditor.value as FollowCommentListActiveEditor;
    }
    return null;
  });

  const displayCount = computed(() => {
    return formatBadgeCount(props.commentCount || 0);
  });

  const showHeaderDivider = computed(() => !isExpanded.value);

  function toggleExpanded() {
    if (!props.collapsible) {
      return;
    }
    expanded.value = !expanded.value;
    if (!expanded.value) {
      closeEditor();
    }
  }

  function handleCreate() {
    if (!props.canCreate) {
      return;
    }

    expanded.value = true;
    activeEditor.value = activeEditor.value?.action === 'create' ? null : { action: 'create' };
  }

  function toggleSortOrder() {
    if (props.loading) {
      return;
    }
    emit('sortChange', props.sortOrder === 'desc' ? 'asc' : 'desc');
  }

  function handleReply(comment: FollowCommentItem) {
    if (!props.canReply) {
      return;
    }

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
    if (!props.canCreate) {
      return;
    }

    emit('createSubmit', value);
    closeEditor();
  }

  function handleReplySubmit(value: FollowCommentActionValue) {
    if (!props.canReply) {
      return;
    }

    emit('replySubmit', value);
    closeEditor();
  }

  function handleEditSubmit(value: FollowCommentActionValue) {
    emit('editSubmit', value);
    closeEditor();
  }

  function disconnectReachBottomObserver() {
    reachBottomObserver?.disconnect();
    reachBottomObserver = null;
  }

  function observeReachBottom() {
    disconnectReachBottomObserver();

    if (!isExpanded.value || !props.hasMore || !reachBottomTriggerRef.value) {
      return;
    }

    reachBottomObserver = new IntersectionObserver(
      ([entry]) => {
        if (entry?.isIntersecting && props.hasMore && !props.loading) {
          emit('reachBottom');
        }
      },
      {
        rootMargin: '120px 0px',
      }
    );
    reachBottomObserver.observe(reachBottomTriggerRef.value);
  }

  watch(
    () => [isExpanded.value, props.hasMore, props.comments.length],
    async () => {
      await nextTick();
      observeReachBottom();
    },
    {
      immediate: true,
    }
  );

  onBeforeUnmount(() => {
    disconnectReachBottomObserver();
  });

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
  .crm-comment__reach-bottom-trigger {
    height: 1px;
  }
</style>
