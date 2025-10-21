// 部门树节点类型枚举
export enum DeptNodeTypeEnum {
  ORG = 'ORG',
  USER = 'USER',
  ROLE = 'ROLE',
}

export enum PersonalEnum {
  INFO = 'INFO',
  MY_PLAN = 'MY_PLAN',
  API_KEY = 'API_KEY',
}

export enum SystemMessageTypeEnum {
  ANNOUNCEMENT_NOTICE = 'ANNOUNCEMENT_NOTICE', // 系统公告
  SYSTEM_NOTICE = 'SYSTEM_NOTICE', // 系统消息
}

export enum SystemResourceMessageTypeEnum {
  CUSTOMER = 'CUSTOMER',
  CUSTOMER_POOL = 'CUSTOMER_POOL',
  CLUE = 'CLUE',
  CLUE_POOL = 'CLUE_POOL',
  OPPORTUNITY = 'OPPORTUNITY',
  SYSTEM = 'SYSTEM',
  CUSTOMER_CONTACT = 'CUSTOMER_CONTACT',
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
  PICK = 'PICK',
  ASSIGN = 'ASSIGN',
  CANCEL = 'CANCEL',
  ADD_USER = 'ADD_USER',
  REMOVE_USER = 'REMOVE_USER',
  MERGE = 'MERGE',
}

export enum PersonalExportStatusEnum {
  STOP = 'STOP', // 已取消
  PREPARED = 'PREPARED', // 导出中
  ERROR = 'ERROR', // 导出失败
  SUCCESS = 'SUCCESS', // 导出成功
}
