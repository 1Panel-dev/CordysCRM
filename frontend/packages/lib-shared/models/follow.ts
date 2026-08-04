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

// 新增评论接口入参：用于新增一级评论、回复一级评论、回复二级评论
export interface SaveFollowCommentParams {
  sourceId: string;
  sourceType: FollowCommentSourceTypeEnum;
  parentId?: string;
  replyToUserId?: string;// 回复二级评论时，用于标记回复XX
  content: string;
  mentionUserIds?: string[]; // @成员ids
}

// 编辑评论接口入参
export interface UpdateFollowCommentParams {
  id: string;
  content: string;
  mentionUserIds?: string[];
}

// 评论输入组件提交值
export interface FollowCommentSubmitValue {
  content: string;
  mentionUsers?: FollowCommentUser[];  // 编辑回显使用
  mentionUserIds?: string[];  // 调新增/编辑接口使用
}

export type FollowCommentEditorAction = 'create' | 'reply' | 'edit';

// 评论操作事件值：回复/编辑时需要同时带上输入内容和当前被操作的评论
export interface FollowCommentActionValue extends FollowCommentSubmitValue {
  comment: FollowCommentItem;
}

// 当前正在打开的评论编辑器状态
export interface FollowCommentActiveEditor {
  action: FollowCommentEditorAction;
  commentId?: string;
}
