export enum ProcessStatusEnum {
  APPROVED = 'APPROVED', // 已通过
  UNAPPROVED = 'UNAPPROVED', // 已驳回
  APPROVING = 'APPROVING', // 审批中
  VOIDED = 'VOIDED', // 作废 todo xinxinwu 待确认后台历史数据是否处理
  PENDING = 'PENDING', // 待提审
  REVOKED = 'REVOKED', // 已撤销
  NONE = 'NONE', // 未开启审批状态
}

export enum ProcessResultEnum {
  AGREE = 'AGREE', // 同意
  REJECT = 'REJECT', // 驳回
  FALLBACK = 'FALLBACK', // 退回
  ADD_SIGN = 'ADD_SIGN', // 加签
}

export enum ApprovalTypeEnum {
  MANUAL = 'MANUAL', // 人工审批
  AUTO_PASS = 'AUTO_PASS',
  AUTO_REJECT = 'AUTO_REJECT',
}

export enum ApproverTypeEnum {
  SPECIFIED_MEMBER = 'MEMBER', // 指定成员
  DIRECT_SUPERVISOR = 'SUPERIOR', // 直属上级
  CONTINUOUS_SUPERVISOR = 'MULTIPLE_SUPERIOR', // 连续多级上级
  SPECIFIED_DEPARTMENT_LEADER = 'DEPT_HEAD', // 指定部门负责人
  CONTINUOUS_DEPARTMENT_LEADER = 'MULTIPLE_DEPT_HEAD', // 连续多级部门负责人
  ROLE = 'ROLE', // 角色
}
