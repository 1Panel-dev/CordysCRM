<template>
  <div :class="['crm-comment-item', { 'crm-comment-item--reply': isReply }]">
    <CrmAvatar
      class="crm-comment-item-avatar"
      :is-word="!props.comment.createUserAvatar"
      :size="avatarSize"
      :text="props.comment.createUserName"
    />

    <div class="crm-comment-item-main">
      <div class="crm-comment-item-content">
        <div class="crm-comment-item-title">
          <div class="crm-comment-item-user">{{ props.comment.createUserName || '-' }}</div>
        </div>

        <div class="crm-comment-item-text">
          <span v-if="hasReplyTarget" class="crm-comment-item-reply-target">{{ replyTargetText }}</span
          ><span>{{ props.comment.content }}</span>
        </div>
      </div>

      <div class="crm-comment-item-meta">
        <div class="crm-comment-item-time">{{ createTimeText }}</div>

        <div v-if="showActions" class="crm-comment-item-actions">
          <button
            v-if="props.canReply"
            type="button"
            :aria-label="t('crmComment.reply')"
            @click.stop="emit('reply', props.comment)"
          >
            <CrmIcon name="iconicon_chat" width="14px" height="14px" color="var(--text-n4)" />
          </button>
          <button
            v-if="props.canEdit"
            type="button"
            :aria-label="t('crmComment.edit')"
            @click.stop="emit('edit', props.comment)"
          >
            <CrmIcon name="iconicon_edit1" width="14px" height="14px" color="var(--text-n4)" />
          </button>
          <button
            v-if="props.canDelete"
            type="button"
            :aria-label="t('crmComment.delete')"
            @click.stop="emit('delete', props.comment)"
          >
            <CrmIcon name="iconicon_delete" width="14px" height="14px" color="var(--text-n4)" />
          </button>
        </div>
      </div>

      <slot name="editor" />
    </div>

    <div v-if="props.showDivider" class="crm-comment-item-divider"></div>
  </div>
</template>

<script setup lang="ts">
  import dayjs from 'dayjs';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { FollowCommentItem } from '@lib/shared/models/follow';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmAvatar from '@/components/business/crm-avatar/index.vue';

  const props = withDefaults(
    defineProps<{
      comment: FollowCommentItem;
      level?: 1 | 2;
      canReply?: boolean;
      canEdit?: boolean;
      canDelete?: boolean;
      showDivider?: boolean;
    }>(),
    {
      level: 1,
      canReply: true,
      canEdit: true,
      canDelete: true,
      showDivider: true,
    }
  );

  const emit = defineEmits<{
    (e: 'reply', comment: FollowCommentItem): void;
    (e: 'edit', comment: FollowCommentItem): void;
    (e: 'delete', comment: FollowCommentItem): void;
  }>();

  const { t } = useI18n();

  const isReply = computed(() => props.level === 2);

  const avatarSize = computed(() => (isReply.value ? 24 : 32));

  const createTimeText = computed(() => {
    return props.comment.createTime ? dayjs(props.comment.createTime).format('YYYY-MM-DD HH:mm') : '-';
  });

  const hasReplyTarget = computed(() => isReply.value && Boolean(props.comment.replyToUserName));

  const replyTargetText = computed(() => {
    if (!hasReplyTarget.value) {
      return '';
    }
    return `${t('crmComment.replyTo')} ${props.comment.replyToUserName}：`;
  });

  const showActions = computed(() => props.canReply || props.canEdit || props.canDelete);
</script>

<style scoped lang="less">
  .crm-comment-item {
    position: relative;
    display: flex;
    padding: 12px 16px;
    gap: 8px;
    width: 100%;
  }
  .crm-comment-item--reply {
    padding-left: 56px;
  }
  .crm-comment-item--reply .crm-comment-item-divider {
    left: 56px;
  }
  .crm-comment-item-avatar {
    flex: none;
    margin-top: 2px;
  }
  .crm-comment-item-main {
    flex: 1;
    min-width: 0;
  }
  .crm-comment-item-content {
    min-width: 0;
  }
  .crm-comment-item-title {
    display: flex;
    align-items: center;
    min-width: 0;
    line-height: 18px;
  }
  .crm-comment-item-user {
    overflow: hidden;
    max-width: 120px;
    font-size: 12px;
    font-weight: 600;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: var(--text-n1);
  }
  .crm-comment-item-time {
    font-size: 12px;
    color: var(--text-n4);
    flex: none;
  }
  .crm-comment-item-text {
    margin-top: 4px;
    white-space: pre-wrap;
    color: var(--text-n2);
    line-height: 22px;
    word-break: break-word;
  }
  .crm-comment-item-reply-target {
    color: var(--text-n4);
  }
  .crm-comment-item-meta {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 4px;
    gap: 8px;
    line-height: 16px;
  }
  .crm-comment-item-actions {
    margin-top: 8px;
    gap: 12px;
    @apply flex items-center;
  }
  .crm-comment-item-divider {
    position: absolute;
    right: 0;
    bottom: 0;
    left: 0;
    height: 1px;
    background: var(--text-n8);
    transform: scaleY(0.5);
    transform-origin: 0 0;
  }
</style>
