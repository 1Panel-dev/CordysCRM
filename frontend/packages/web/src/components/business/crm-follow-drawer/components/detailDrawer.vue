<template>
  <CrmDrawer
    v-model:show="showDrawer"
    :title="`${props.sourceName} ${
      props.formKey === FormDesignKeyEnum.FOLLOW_RECORD ? t('module.customer.followRecord') : t('common.plan')
    }`"
    :width="800"
    no-padding
    :footer="false"
  >
    <template v-if="!props.readonly" #titleRight>
      <div v-if="!props.readonly" class="flex items-center gap-[12px]">
        <n-button type="primary" ghost class="n-btn-outline-primary" @click="handleEdit">
          {{ t('common.edit') }}
        </n-button>
        <n-button
          v-if="
            props.detail?.status &&
            [CustomerFollowPlanStatusEnum.COMPLETED].includes(props.detail?.status) &&
            !props.detail?.converted
          "
          type="primary"
          ghost
          class="n-btn-outline-primary"
          @click="handleConvert"
        >
          {{ t('common.convertPlanToRecord') }}
        </n-button>
        <n-button type="error" ghost class="n-btn-outline-error" @click="handleDelete">
          {{ t('common.delete') }}
        </n-button>
      </div>
    </template>
    <div class="h-full bg-[var(--text-n9)] p-[16px]">
      <CrmCard hide-footer>
        <div class="flex-1">
          <CrmFormDescription
            :form-key="props.formKey"
            :source-id="props.sourceId"
            :refresh-key="props.refreshKey"
            :column="3"
            label-width="auto"
            value-align="start"
            readonly
          />
        </div>
        <n-divider class="!mb-[12px] !mt-[16px] bg-[var(--text-n8)]" />
        <CrmComment
          v-model:expanded="commentExpanded"
          :comments="mockCommentList"
          :comment-count="commentCount"
          @create-submit="handleCreateComment"
          @reply-submit="handleReplyComment"
          @edit-submit="handleEditComment"
          @delete="handleDeleteComment"
        />
      </CrmCard>
    </div>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { NButton, NDivider } from 'naive-ui';
  import dayjs from 'dayjs';

  import { CustomerFollowPlanStatusEnum } from '@lib/shared/enums/customerEnum';
  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import {
    type FollowCommentActionValue,
    type FollowCommentItem,
    FollowCommentSourceTypeEnum,
    type FollowCommentSubmitValue,
  } from '@lib/shared/models/follow';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmComment from '@/components/business/crm-comment/index.vue';
  import CrmFormDescription from '@/components/business/crm-form-description/index.vue';

  const props = defineProps<{
    sourceId: string;
    detail?: any;
    formKey: FormDesignKeyEnum;
    sourceName: string;
    refreshKey: number;
    readonly?: boolean;
  }>();

  const showDrawer = defineModel<boolean>('show', {
    required: true,
  });

  const emit = defineEmits<{
    (e: 'delete'): void;
    (e: 'edit'): void;
    (e: 'convert', detail?: any): void;
  }>();

  const { t } = useI18n();

  const commentExpanded = ref(true);
  const mockCommentList = ref<FollowCommentItem[]>([]);

  const commentSourceType = computed(() =>
    props.formKey === FormDesignKeyEnum.FOLLOW_RECORD
      ? FollowCommentSourceTypeEnum.FOLLOW_RECORD
      : FollowCommentSourceTypeEnum.FOLLOW_PLAN
  );

  const commentCount = computed(() =>
    mockCommentList.value.reduce((total, comment) => total + 1 + (comment.replies?.length || 0), 0)
  );

  function createMockCommentList(): FollowCommentItem[] {
    // todo xinixnwu
    return [];
  }

  function createLocalComment(content: string, mentionUserIds: string[]): FollowCommentItem {
    return {
      id: `detail-mock-comment-${Date.now()}`,
      sourceId: props.sourceId,
      sourceType: commentSourceType.value,
      content,
      createUser: 'mock-current-user',
      createUserName: '当前用户',
      createTime: Date.now(),
      mentionUsers: mentionUserIds.map((id) => ({
        id,
        name: id,
      })),
      replies: [],
      replyCount: 0,
    };
  }

  function handleCreateComment(value: FollowCommentSubmitValue) {
    // TODO xinxinwu: 后端评论新增接口完成后，按跟进记录/计划分流调用真实接口并刷新。
    mockCommentList.value.unshift(createLocalComment(value.content, value.mentionUserIds || []));
  }

  function handleReplyComment(value: FollowCommentActionValue) {
    // TODO xinxinwu: 后端评论回复接口完成后，提交 parentId、replyToUserId、mentionUserIds 并刷新。
    const reply = {
      ...createLocalComment(value.content, value.mentionUserIds || []),
      parentId: value.comment.parentId || value.comment.id,
      replyToUserId: value.comment.createUser,
      replyToUserName: value.comment.createUserName,
    };

    const parentComment = mockCommentList.value.find((comment) => comment.id === reply.parentId);
    if (!parentComment) {
      return;
    }
    parentComment.replies = [...(parentComment.replies || []), reply];
    parentComment.replyCount = parentComment.replies.length;
  }

  function handleEditComment(value: FollowCommentActionValue) {
    // TODO xinxinwu: 后端评论编辑接口完成后，调用真实接口并刷新。
    const comments = mockCommentList.value;
    const targetComment =
      comments.find((comment) => comment.id === value.comment.id) ||
      comments.flatMap((comment) => comment.replies || []).find((comment) => comment.id === value.comment.id);
    if (!targetComment) {
      return;
    }
    targetComment.content = value.content;
    targetComment.mentionUsers = (value.mentionUserIds || []).map((id) => ({
      id,
      name: id,
    }));
    targetComment.updateTime = Date.now();
  }

  function handleDeleteComment(comment: FollowCommentItem) {
    // TODO xinxinwu: 后端评论删除接口完成后，补删除确认并调用真实接口。
    mockCommentList.value = mockCommentList.value
      .filter((item) => item.id !== comment.id)
      .map((item) => {
        const replies = (item.replies || []).filter((reply) => reply.id !== comment.id);
        return {
          ...item,
          replies,
          replyCount: replies.length,
        };
      });
  }

  watch(
    () => [showDrawer.value, props.sourceId, props.formKey],
    ([visible]) => {
      if (visible) {
        commentExpanded.value = true;
        // TODO xinxinwu: 后端详情评论接口完成后，打开详情抽屉时按 sourceId 拉取真实评论列表。
        mockCommentList.value = createMockCommentList();
      }
    },
    {
      immediate: true,
    }
  );

  function handleDelete() {
    emit('delete');
  }

  function handleEdit() {
    emit('edit');
  }

  function handleConvert() {
    emit('convert', props.detail);
  }
</script>
