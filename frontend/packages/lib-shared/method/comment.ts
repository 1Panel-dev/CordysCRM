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

export const FOLLOW_COMMENT_MAX_LENGTH = 512;

function getCommentReplyCount(comment: FollowCommentItem) {
  return typeof comment.replyCount === 'number' ? comment.replyCount : (comment.replies?.length || 0);
}

export function getLocalCommentCount(comments: FollowCommentItem[] = []) {
  return comments.reduce((total, comment) => total + 1 + getCommentReplyCount(comment), 0);
}

export function getDeletedCommentCount(comment: FollowCommentItem) {
  return 1 + getCommentReplyCount(comment);
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
    resourceId: sourceId,
    parentId: parentComment?.parentId || parentComment?.id,
    replyToUserId: parentComment?.createUser,
    content: value.content,
    mentionedUserIds: value.mentionUserIds,
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
    mentionedUserIds: value.mentionUserIds,
  };
}
