import { computed, ref, watch } from 'vue';
import { showSuccessToast } from 'vant';

import { useI18n } from '@lib/shared/hooks/useI18n';
import { buildSaveCommentParams, buildUpdateCommentParams } from '@lib/shared/method/comment';
import type { TableQueryParams } from '@lib/shared/models/common';
import type {
  FollowCommentItem,
  FollowCommentListParams,
  FollowCommentPageResult,
  FollowCommentSubmitValue,
  SaveFollowCommentParams,
  UpdateFollowCommentParams,
} from '@lib/shared/models/follow';

import {
  addApprovalComment,
  addFollowPlanComment,
  addFollowRecordComment,
  deleteApprovalComment,
  deleteFollowPlanComment,
  deleteFollowRecordComment,
  getApprovalCommentList,
  getFollowPlanCommentList,
  getFollowRecordCommentList,
  updateApprovalComment,
  updateFollowPlanComment,
  updateFollowRecordComment,
} from '@/api/modules';

import type { Ref } from 'vue';

export type MobileCommentResourceType = 'record' | 'plan' | 'approval';

interface CommentApiGroup {
  list: (params: FollowCommentListParams) => Promise<FollowCommentPageResult>;
  add: (params: SaveFollowCommentParams) => Promise<unknown>;
  update: (params: UpdateFollowCommentParams) => Promise<unknown>;
  delete: (id: string) => Promise<unknown>;
}

const commentApiMap: Record<MobileCommentResourceType, CommentApiGroup> = {
  record: {
    list: getFollowRecordCommentList,
    add: addFollowRecordComment,
    update: updateFollowRecordComment,
    delete: deleteFollowRecordComment,
  },
  plan: {
    list: getFollowPlanCommentList,
    add: addFollowPlanComment,
    update: updateFollowPlanComment,
    delete: deleteFollowPlanComment,
  },
  approval: {
    list: getApprovalCommentList,
    add: addApprovalComment,
    update: updateApprovalComment,
    delete: deleteApprovalComment,
  },
};

export default function useCommentResource(options: {
  type: Ref<MobileCommentResourceType>;
  sourceId: Ref<string>;
  sortOrder?: Ref<'asc' | 'desc'>;
  enableSort?: Ref<boolean>;
}) {
  const { t } = useI18n();
  const submitLoading = ref(false);
  const loadedComments = ref<FollowCommentItem[]>([]);

  const currentApi = computed(() => commentApiMap[options.type.value]);

  watch([options.type, options.sourceId], () => {
    loadedComments.value = [];
  });

  async function loadCommentList(params: TableQueryParams): Promise<FollowCommentPageResult> {
    const result = await currentApi.value.list({
      ...params,
      resourceId: options.sourceId.value,
      ...(options.enableSort?.value
        ? {
            sort: {
              type: options.sortOrder?.value || 'desc',
            },
          }
        : {}),
    });

    if ((params.current || 1) === 1) {
      loadedComments.value = result.list;
    } else {
      const loadedCommentIds = new Set(loadedComments.value.map((comment) => comment.id));
      loadedComments.value = loadedComments.value.concat(
        result.list.filter((comment) => !loadedCommentIds.has(comment.id))
      );
    }

    return {
      ...result,
      list: loadedComments.value,
    };
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
      showSuccessToast(t('common.operationSuccess'));
    } finally {
      submitLoading.value = false;
    }
  }

  async function replyComment(value: FollowCommentSubmitValue, comment: FollowCommentItem) {
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
          parentComment: comment,
        })
      );
      showSuccessToast(t('common.operationSuccess'));
    } finally {
      submitLoading.value = false;
    }
  }

  async function editComment(value: FollowCommentSubmitValue, comment: FollowCommentItem) {
    submitLoading.value = true;
    try {
      await currentApi.value.update(
        buildUpdateCommentParams({
          commentId: comment.id,
          value,
        })
      );
      showSuccessToast(t('common.operationSuccess'));
    } finally {
      submitLoading.value = false;
    }
  }

  async function deleteComment(comment: FollowCommentItem) {
    submitLoading.value = true;
    try {
      await currentApi.value.delete(comment.id);
      showSuccessToast(t('common.deleteSuccess'));
    } finally {
      submitLoading.value = false;
    }
  }

  return {
    submitLoading,
    loadCommentList,
    createComment,
    replyComment,
    editComment,
    deleteComment,
  };
}
