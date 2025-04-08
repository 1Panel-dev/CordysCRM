// 部门树节点类型枚举
export enum DeptNodeTypeEnum {
  ORG = 'ORG',
  USER = 'USER',
  ROLE = 'ROLE',
}

export enum PersonalEnum {
  INFO = 'INFO',
  MY_PLAN = 'MY_PLAN',
}

export enum SystemMessageTypeEnum {
  ANNOUNCEMENT_NOTICE = 'ANNOUNCEMENT_NOTICE', // 系统公告
  SYSTEM_NOTICE = 'SYSTEM_NOTICE', // 系统消息
}

export enum SystemResourceMessageTypeEnum {
  CUSTOMER = 'CUSTOMER',
  CLUE = 'CLUE',
  OPPORTUNITY = 'OPPORTUNITY',
  SYSTEM = 'SYSTEM',
}

export enum SystemMessageStatusEnum {
  READ = 'READ', // 已读
  UNREAD = 'UNREAD', // 未读
}

export enum OperationTypeEnum {
  UPDATE = 'UPDATE',
  ADD = 'ADD',
  DELETE = 'DELETE',
  IMPORT = 'IMPORT',
  EXPORT = 'EXPORT',
  SYNC = 'SYNC',
  MOVE_TO_CUSTOMER_POOL = 'MOVE_TO_CUSTOMER_POOL',
}
