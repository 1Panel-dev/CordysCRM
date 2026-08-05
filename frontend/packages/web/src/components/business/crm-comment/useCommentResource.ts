import { computed, ref, watch } from 'vue';

import { buildSaveCommentParams, buildUpdateCommentParams } from '@lib/shared/method/comment';
import type { CommonList } from '@lib/shared/models/common';
import type {
  FollowCommentActionValue,
  FollowCommentItem,
  FollowCommentListParams,
  FollowCommentSubmitValue,
  SaveFollowCommentParams,
  UpdateFollowCommentParams,
} from '@lib/shared/models/follow';

import {
  addFollowPlanComment,
  addFollowRecordComment,
  deleteFollowPlanComment,
  deleteFollowRecordComment,
  getFollowPlanCommentList,
  getFollowRecordCommentList,
  updateFollowPlanComment,
  updateFollowRecordComment,
} from '@/api/modules';

import type { Ref } from 'vue';

export type CommentResourceType = 'followRecord' | 'followPlan';

interface CommentApiGroup {
  list: (params: FollowCommentListParams) => Promise<CommonList<FollowCommentItem>>;
  add: (params: SaveFollowCommentParams) => Promise<unknown>;
  update: (params: UpdateFollowCommentParams) => Promise<unknown>;
  delete: (id: string) => Promise<unknown>;
}

interface UseCommentResourceOptions {
  type: Ref<CommentResourceType>;
  sourceId: Ref<string>;
  initialCount?: Ref<number | undefined>;
  pageSize?: number;
}

const DEFAULT_PAGE_SIZE = 20;

const commentApiMap: Record<CommentResourceType, CommentApiGroup> = {
  followRecord: {
    list: getFollowRecordCommentList,
    add: addFollowRecordComment,
    update: updateFollowRecordComment,
    delete: deleteFollowRecordComment,
  },
  followPlan: {
    list: getFollowPlanCommentList,
    add: addFollowPlanComment,
    update: updateFollowPlanComment,
    delete: deleteFollowPlanComment,
  },
};

export default function useCommentResource(options: UseCommentResourceOptions) {
  const comments = ref<FollowCommentItem[]>([]);
  const commentCount = ref(0);
  const loading = ref(false);
  const submitLoading = ref(false);
  const current = ref(1);
  const hasMore = ref(true);
  const initialized = ref(false);

  const pageSize = options.pageSize || DEFAULT_PAGE_SIZE;

  const currentApi = computed(() => commentApiMap[options.type.value]);

  function syncInitialCount() {
    const initialCount = options.initialCount?.value;
    if (!initialized.value && typeof initialCount === 'number') {
      commentCount.value = initialCount;
    }
  }

  function resetComments() {
    comments.value = [];
    current.value = 1;
    hasMore.value = true;
    initialized.value = false;
    syncInitialCount();
  }

  async function loadComments(refresh = false) {
    const sourceId = options.sourceId.value;
    if (!sourceId || loading.value || (!refresh && !hasMore.value)) {
      return;
    }

    loading.value = true;
    try {
      const requestCurrent = refresh ? 1 : current.value;
      const res = await currentApi.value.list({
        sourceId,
        current: requestCurrent,
        pageSize,
      });

      comments.value = refresh ? res.list : comments.value.concat(res.list);
      commentCount.value = res.total;
      current.value = requestCurrent + 1;
      hasMore.value = comments.value.length < res.total;
      initialized.value = true;
    } finally {
      loading.value = false;
    }
  }

  async function initComments() {
    if (initialized.value) {
      return;
    }
    await loadComments(true);
  }

  async function createComment(value: FollowCommentSubmitValue) {
    const sourceId = options.sourceId.value;
    if (!sourceId) {
      return;
    }

    submitLoading.value = true;
    try {
      await currentApi.value.add(
        buildSaveCommentParams({
          sourceId,
          value,
        })
      );
      await loadComments(true);
    } finally {
      submitLoading.value = false;
    }
  }

  async function replyComment(value: FollowCommentActionValue) {
    const sourceId = options.sourceId.value;
    if (!sourceId) {
      return;
    }

    submitLoading.value = true;
    try {
      await currentApi.value.add(
        buildSaveCommentParams({
          sourceId,
          value,
          parentComment: value.comment,
        })
      );
      await loadComments(true);
    } finally {
      submitLoading.value = false;
    }
  }

  async function editComment(value: FollowCommentActionValue) {
    submitLoading.value = true;
    try {
      await currentApi.value.update(
        buildUpdateCommentParams({
          commentId: value.comment.id,
          value,
        })
      );
      await loadComments(true);
    } finally {
      submitLoading.value = false;
    }
  }

  async function deleteComment(comment: FollowCommentItem) {
    submitLoading.value = true;
    try {
      await currentApi.value.delete(comment.id);
      await loadComments(true);
    } finally {
      submitLoading.value = false;
    }
  }

  watch(
    () => [options.type.value, options.sourceId.value],
    () => {
      resetComments();
    }
  );

  watch(
    () => options.initialCount?.value,
    (val) => {
      if (!initialized.value && typeof val === 'number') {
        commentCount.value = val;
      }
    },
    {
      immediate: true,
    }
  );

  return {
    comments,
    commentCount,
    loading,
    submitLoading,
    current,
    hasMore,
    initialized,
    initComments,
    loadComments,
    createComment,
    replyComment,
    editComment,
    deleteComment,
    resetComments,
  };
}
