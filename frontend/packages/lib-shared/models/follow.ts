import type { TableQueryParams } from './common';

export enum FollowCommentSourceTypeEnum {
  FOLLOW_RECORD = 'FOLLOW_RECORD',
  FOLLOW_PLAN = 'FOLLOW_PLAN',
}

export interface FollowCommentUser {
  id: string;
  name: string;
  avatar?: string;
  enable?: boolean;
}

export interface FollowCommentItem {
  id: string;
  sourceId: string;
  sourceType: FollowCommentSourceTypeEnum;
  parentId?: string;
  content: string;
  createUser: string;
  createUserName: string;
  createUserAvatar?: string;
  createTime: number;
  updateTime?: number;
  replyToUserId?: string;
  replyToUserName?: string;
  mentionUsers?: FollowCommentUser[];
  replies?: FollowCommentItem[];
  replyCount?: number;
}

export interface FollowCommentListParams extends TableQueryParams {
  sourceId: string;
  sourceType: FollowCommentSourceTypeEnum;
}

export interface SaveFollowCommentParams {
  sourceId: string;
  sourceType: FollowCommentSourceTypeEnum;
  parentId?: string;
  replyToUserId?: string;
  content: string;
  mentionUserIds?: string[];
}

export interface UpdateFollowCommentParams {
  id: string;
  content: string;
  mentionUserIds?: string[];
}
