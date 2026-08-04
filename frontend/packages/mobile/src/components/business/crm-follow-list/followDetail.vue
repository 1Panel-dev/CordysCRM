<template>
  <CrmPageWrapper :title="t('common.detail')">
    <div class="bg-[var(--text-n9)] py-[16px]">
      <CrmDescription :description="descriptions" />
    </div>
    <div class="bg-[var(--text-n9)] px-[16px] pt-0">
      <div class="bg-[var(--text-n10)] p-[16px]">
        <div class="font-[600]">{{ t('common.communicationContent') }}</div>
        <div class="mt-[16px] rounded-[var(--border-radius-large)] bg-[var(--text-n9)] p-[16px]">
          {{ detail.content }}
        </div>
      </div>
    </div>
    <div class="bg-[var(--text-n9)] p-[16px]">
      <CrmComment
        class="mt-0"
        :comments="comments"
        :count="commentCount"
        :show-add="canEditComment"
        :can-reply="canEditComment"
        :can-edit="canEditComment"
        :can-delete="canEditComment"
        @create="handleCreateComment"
        @reply="handleReplyComment"
        @edit="handleEditComment"
        @delete="handleDeleteComment"
        @change-editor="commentEditing = Boolean($event)"
      />
    </div>
    <template v-if="canEditDetail && !commentEditing" #footer>
      <div class="flex items-center justify-center gap-[16px]">
        <div class="flex w-[100px] items-center">
          <CrmTextButton
            color="var(--text-n1)"
            icon="iconicon_delete"
            :text="t('common.delete')"
            icon-size="18px"
            direction="column"
            class="flex-1"
            @click="handleDelete"
          />
        </div>
        <van-button
          type="primary"
          class="flex-1 !rounded-[var(--border-radius-small)] !text-[16px]"
          plain
          @click="handleEdit"
        >
          {{ t('common.edit') }}
        </van-button>
      </div>
    </template>
  </CrmPageWrapper>
</template>

<script setup lang="ts">
  import { useRoute, useRouter } from 'vue-router';
  import { showSuccessToast } from 'vant';

  import { CustomerFollowPlanStatusEnum } from '@lib/shared/enums/customerEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { FollowCommentItem, FollowCommentSubmitValue } from '@lib/shared/models/follow';
  import { FollowCommentSourceTypeEnum } from '@lib/shared/models/follow';

  import CrmDescription from '@/components/pure/crm-description/index.vue';
  import CrmPageWrapper from '@/components/pure/crm-page-wrapper/index.vue';
  import CrmTextButton from '@/components/pure/crm-text-button/index.vue';
  import CrmComment from '@/components/business/crm-comment/index.vue';

  import { followPlanApiMap, followRecordApiMap, PlanEnumType, RecordEnumType } from '@/config/follow';
  import useFormCreateApi from '@/hooks/useFormCreateApi';

  import { CommonRouteEnum } from '@/enums/routeEnum';

  const route = useRoute();
  const router = useRouter();
  const { t } = useI18n();

  const isPlan = computed(() => route.query.formKey?.includes('plan'));
  const formKey = computed(() => (route.query.formKey?.toString() as RecordEnumType | PlanEnumType) || '');
  const sourceId = computed(() => route.query.id?.toString() || '');
  const canEditDetail = computed(() => route.query.readonly?.toString() !== 'true');
  const canEditComment = computed(() => canEditDetail.value);
  const commentEditing = ref(false);
  const commentSourceType = computed(() =>
    isPlan.value ? FollowCommentSourceTypeEnum.FOLLOW_PLAN : FollowCommentSourceTypeEnum.FOLLOW_RECORD
  );

  const { descriptions, initFormConfig, initFormDescription, detail } = useFormCreateApi({
    formKey: formKey.value,
    sourceId,
    needInitDetail: route.query.needInitDetail === 'Y',
  });

  // TODO xinxinwu: 后端评论接口联调，按 commentSourceType 切换跟进记录/计划评论真实接口。
  const comments = ref<FollowCommentItem[]>([
    {
      id: 'mock-detail-comment-1',
      sourceId: sourceId.value,
      sourceType: commentSourceType.value,
      content: '我已经是第三次参加 SICC 大会了，作为一名服务体验设计行业的从业者每次参与都受匪浅。',
      createUser: 'user-1',
      createUserName: '小新',
      createTime: Date.now() - 1000 * 60 * 60,
      replies: [
        {
          id: 'mock-detail-reply-1',
          sourceId: sourceId.value,
          sourceType: commentSourceType.value,
          parentId: 'mock-detail-comment-1',
          content: '但是经常打瞌睡的基础还是要补一下。',
          createUser: 'user-2',
          createUserName: '罗婷',
          createTime: Date.now() - 1000 * 60 * 40,
          replyToUserId: 'user-1',
          replyToUserName: '小新',
        },
      ],
      replyCount: 1,
    },
    {
      id: 'mock-detail-comment-2',
      sourceId: sourceId.value,
      sourceType: commentSourceType.value,
      content: '这是一条详情页评论，用来看详情内嵌评论的列表间距。',
      createUser: 'user-3',
      createUserName: 'admin',
      createTime: Date.now() - 1000 * 60 * 20,
      replies: [],
      replyCount: 0,
    },
  ]);

  function getLocalCommentCount() {
    return comments.value.reduce((total, comment) => total + 1 + (comment.replies?.length || 0), 0);
  }

  const commentCount = ref(getLocalCommentCount());

  function syncLocalCommentCount() {
    commentCount.value = getLocalCommentCount();
  }

  onBeforeMount(async () => {
    await initFormConfig();
    initFormDescription();

    const detailCommentCount = Number((detail.value as { commentCount?: number }).commentCount);
    if (Number.isFinite(detailCommentCount)) {
      commentCount.value = detailCommentCount;
    }
  });

  async function handleDelete() {
    try {
      if (isPlan.value) {
        await followPlanApiMap.delete?.[formKey.value as PlanEnumType]?.(sourceId.value);
      } else {
        await followRecordApiMap.delete?.[formKey.value as RecordEnumType]?.(sourceId.value);
      }
      showSuccessToast(t('common.deleteSuccess'));
      router.back();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  function handleEdit() {
    router.push({
      name: CommonRouteEnum.FORM_CREATE,
      query: {
        formKey: formKey.value,
        id: sourceId.value,
        needInitDetail: 'Y',
      },
      ...(detail.value.converted === undefined
        ? {}
        : {
            state: {
              params: JSON.stringify({ converted: detail.value.converted }),
            },
          }),
    });
  }

  function createMockComment(value: FollowCommentSubmitValue, parentComment?: FollowCommentItem): FollowCommentItem {
    return {
      id: `mock-detail-comment-${Date.now()}`,
      sourceId: sourceId.value,
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

  function handleCreateComment(value: FollowCommentSubmitValue) {
    comments.value = [createMockComment(value), ...comments.value];
    syncLocalCommentCount();
    showSuccessToast(t('common.operationSuccess'));
  }

  function handleReplyComment(value: FollowCommentSubmitValue, comment: FollowCommentItem) {
    const reply = createMockComment(value, comment);
    comments.value = comments.value.map((item) => {
      if (item.id === comment.id) {
        return {
          ...item,
          replies: [...(item.replies || []), reply],
          replyCount: (item.replyCount || 0) + 1,
        };
      }

      if (item.replies?.some((replyItem) => replyItem.id === comment.id)) {
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

  function handleEditComment(value: FollowCommentSubmitValue, comment: FollowCommentItem) {
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

  function handleDeleteComment(comment: FollowCommentItem) {
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

<style lang="less" scoped>
  :deep(.crm-page-content) {
    @apply !overflow-auto;
  }
</style>
