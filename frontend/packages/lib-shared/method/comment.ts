import type {
  FollowCommentItem,
  FollowCommentSubmitValue,
  SaveFollowCommentParams,
  UpdateFollowCommentParams,
} from '../models/follow';

export const FOLLOW_COMMENT_OPERATE_PERMISSIONS = [
  'CLUE_MANAGEMENT:READ',
  'CUSTOMER_MANAGEMENT:READ',
  'OPPORTUNITY_MANAGEMENT:READ',
];

export function getLocalCommentCount(comments: FollowCommentItem[] = []) {
  return comments.reduce((total, comment) => total + 1 + (comment.replies?.length || 0), 0);
}

export function buildSaveCommentParams({
  sourceId,
  value,
  parentComment,
}: {
  sourceId: string;
  value: FollowCommentSubmitValue;
  parentComment?: FollowCommentItem;
}): SaveFollowCommentParams {
  return {
    sourceId,
    parentId: parentComment?.parentId || parentComment?.id,
    replyToUserId: parentComment?.createUser,
    content: value.content,
    mentionUserIds: value.mentionUserIds,
  };
}

export function buildUpdateCommentParams({
  commentId,
  value,
}: {
  commentId: string;
  value: FollowCommentSubmitValue;
}): UpdateFollowCommentParams {
  return {
    id: commentId,
    content: value.content,
    mentionUserIds: value.mentionUserIds,
  };
}
