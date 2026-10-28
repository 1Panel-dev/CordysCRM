import type {
  FollowCommentItem,
  FollowCommentSubmitValue,
  FollowCommentUser,
  SaveFollowCommentParams,
  UpdateFollowCommentParams,
} from '../models/follow';

export const FOLLOW_COMMENT_OPERATE_PERMISSIONS = [
  'CLUE_MANAGEMENT:READ',
  'CUSTOMER_MANAGEMENT:READ',
  'OPPORTUNITY_MANAGEMENT:READ',
];

export const FOLLOW_COMMENT_MAX_LENGTH = 300;

function getCommentReplyCount(comment: FollowCommentItem) {
  return typeof comment.replyCount === 'number' ? comment.replyCount : comment.replies?.length || 0;
}

export function getLocalCommentCount(comments: FollowCommentItem[] = []) {
  return comments.reduce((total, comment) => total + 1 + getCommentReplyCount(comment), 0);
}

export function getDeletedCommentCount(comment: FollowCommentItem) {
  return 1 + getCommentReplyCount(comment);
}

function escapeRegExp(value: string) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

export function filterMentionUsers(content: string, mentionUsers: FollowCommentUser[] = []) {
  return mentionUsers.filter((user) => {
    const mentionPattern = new RegExp(`@${escapeRegExp(user.name)}(?=$|[@\\s,，。.!！?？;；:：、])`);
    return mentionPattern.test(content);
  });
}

export interface CommentContentSegment {
  text: string;
  mention?: boolean;
}

/** 按已选用户拆分评论文本，避免将普通同名文本误展示为 @ 提及。 */
export function splitCommentMentions(content: string, mentionUsers: FollowCommentUser[] = []): CommentContentSegment[] {
  const users = [...mentionUsers].sort((a, b) => b.name.length - a.name.length);
  const segments: CommentContentSegment[] = [];
  let text = '';
  let index = 0;

  const appendText = () => {
    if (text) {
      segments.push({ text });
      text = '';
    }
  };

  while (index < content.length) {
    const user = users.find((item) => {
      const mention = `@${item.name}`;
      const nextChar = content[index + mention.length];
      return content.startsWith(mention, index) && (!nextChar || /[@\s,，。.!！?？;；:：、]/.test(nextChar));
    });
    if (user) {
      appendText();
      segments.push({ text: `@${user.name}`, mention: true });
      index += user.name.length + 1;
    } else {
      text += content[index];
      index += 1;
    }
  }
  appendText();

  return segments;
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
    mentionedUserIds: value.mentionUserIds || [],
    attachmentIds: value.attachmentIds || [],
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
    mentionedUserIds: value.mentionUserIds || [],
    attachmentIds: value.attachmentIds || [],
  };
}
