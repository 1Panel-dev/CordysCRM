<template>
  <CrmPageWrapper :title="t('crmComment.title')">
    <div class="h-full bg-[var(--text-n9)]">
      <CrmComment
        :comments="comments"
        :count="commentCount"
        :default-reply-count="1"
        @create="handleCreate"
        @reply="handleReply"
        @edit="handleEdit"
        @delete="handleDelete"
      />
    </div>
  </CrmPageWrapper>
</template>

<script setup lang="ts">
  import { useRoute } from 'vue-router';
  import { showSuccessToast } from 'vant';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { FollowCommentItem, FollowCommentSubmitValue } from '@lib/shared/models/follow';
  import { FollowCommentSourceTypeEnum } from '@lib/shared/models/follow';

  import CrmPageWrapper from '@/components/pure/crm-page-wrapper/index.vue';
  import CrmComment from './index.vue';

  const route = useRoute();
  const { t } = useI18n();
  const commentSourceType = computed(() =>
    route.query.type === 'plan' ? FollowCommentSourceTypeEnum.FOLLOW_PLAN : FollowCommentSourceTypeEnum.FOLLOW_RECORD
  );

  function getInitialCommentCount() {
    const count = Number(route.query.commentCount);
    return Number.isFinite(count) ? count : undefined;
  }

  // TODO xinxinwu等待联调
  const comments = ref<FollowCommentItem[]>([
    {
      id: 'mock-comment-1',
      sourceId: String(route.query.id || 'mock-source'),
      sourceType: commentSourceType.value,
      content: '我已经是第三次参加 SICC 大会了，作为一名服务体验设计行业的从业者每次参与都受匪浅。',
      createUser: 'user-1',
      createUserName: '小新',
      createTime: Date.now() - 1000 * 60 * 60,
      replies: [
        {
          id: 'mock-reply-1',
          sourceId: String(route.query.id || 'mock-source'),
          sourceType: commentSourceType.value,
          parentId: 'mock-comment-1',
          content: '但是经常打瞌睡的基础还是要补一下。',
          createUser: 'user-2',
          createUserName: '罗婷',
          createTime: Date.now() - 1000 * 60 * 40,
          replyToUserId: 'user-1',
          replyToUserName: '小新',
        },
        {
          id: 'mock-reply-2',
          sourceId: String(route.query.id || 'mock-source'),
          sourceType: commentSourceType.value,
          parentId: 'mock-comment-1',
          content: '回复评论 2-3-1，这里用于查看二级回复样式。',
          createUser: 'user-3',
          createUserName: '吴鑫鑫',
          createTime: Date.now() - 1000 * 60 * 20,
          replyToUserId: 'user-2',
          replyToUserName: '罗婷',
        },
      ],
      replyCount: 2,
    },
    {
      id: 'mock-comment-2',
      sourceId: String(route.query.id || 'mock-source'),
      sourceType: commentSourceType.value,
      content: '这是一条一级评论，用来看列表间距、分割线和操作按钮。',
      createUser: 'user-4',
      createUserName: 'admin',
      createTime: Date.now() - 1000 * 60 * 10,
      replies: [],
      replyCount: 0,
    },
    {
      id: 'mock-comment-3',
      sourceId: String(route.query.id || 'mock-source'),
      sourceType: commentSourceType.value,
      content: '第三条评论内容稍微长一点，便于观察移动端自动换行后的展示效果。',
      createUser: 'user-5',
      createUserName: 'Name1 名称',
      createTime: Date.now() - 1000 * 60 * 5,
      replies: [],
      replyCount: 0,
    },
  ]);

  function getLocalCommentCount() {
    return comments.value.reduce((total, comment) => total + 1 + (comment.replies?.length || 0), 0);
  }

  const commentCount = ref(getInitialCommentCount() ?? getLocalCommentCount());

  function syncLocalCommentCount() {
    commentCount.value = getLocalCommentCount();
  }
  // todo xinxinwu mock 等待联调
  function createMockComment(value: FollowCommentSubmitValue, parentComment?: FollowCommentItem): FollowCommentItem {
    return {
      id: `mock-comment-${Date.now()}`,
      sourceId: String(route.query.id || 'mock-source'),
      sourceType: commentSourceType.value,
      parentId: parentComment?.id,
      content: value.content,
      createUser: 'current-user',
      createUserName: '当前用户',
      createTime: Date.now(),
      replyToUserId: parentComment?.createUser,
      replyToUserName: parentComment?.createUserName,
      mentionUsers: value.mentionUsers,
      replies: [],
      replyCount: 0,
    };
  }

  function handleCreate(value: FollowCommentSubmitValue) {
    comments.value = [createMockComment(value), ...comments.value];
    syncLocalCommentCount();
    showSuccessToast(t('common.operationSuccess'));
  }

  function handleReply(value: FollowCommentSubmitValue, comment: FollowCommentItem) {
    const reply = createMockComment(value, comment);
    comments.value = comments.value.map((item) => {
      if (item.id === comment.id) {
        return {
          ...item,
          replies: [...(item.replies || []), reply],
          replyCount: (item.replyCount || 0) + 1,
        };
      }

      const hasReply = item.replies?.some((replyItem) => replyItem.id === comment.id);
      if (hasReply) {
        return {
          ...item,
          replies: [...(item.replies || []), reply],
          replyCount: (item.replyCount || 0) + 1,
        };
      }
      return item;
    });
    syncLocalCommentCount();
    showSuccessToast(t('common.operationSuccess'));
  }

  function handleEdit(value: FollowCommentSubmitValue, comment: FollowCommentItem) {
    comments.value = comments.value.map((item) => {
      if (item.id === comment.id) {
        return { ...item, content: value.content, updateTime: Date.now() };
      }

      return {
        ...item,
        replies: item.replies?.map((reply) =>
          reply.id === comment.id ? { ...reply, content: value.content, updateTime: Date.now() } : reply
        ),
      };
    });
    showSuccessToast(t('common.operationSuccess'));
  }

  function handleDelete(comment: FollowCommentItem) {
    comments.value = comments.value
      .filter((item) => item.id !== comment.id)
      .map((item) => {
        const replies = item.replies?.filter((reply) => reply.id !== comment.id) || [];
        return {
          ...item,
          replies,
          replyCount: replies.length,
        };
      });
    syncLocalCommentCount();
    showSuccessToast(t('common.deleteSuccess'));
  }
</script>

<style scoped lang="less"></style>
