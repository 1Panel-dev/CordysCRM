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
