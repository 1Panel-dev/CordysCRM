export enum ContractStatusEnum {
  PENDING_SIGNING = 'PENDING_SIGNING', // 待签署
  SIGNED = 'SIGNED', // 已签署
  IN_PROGRESS = 'IN_PROGRESS', // 履行中
  COMPLETED_PERFORMANCE = 'COMPLETED_PERFORMANCE', // 履行完毕
  VOID = 'VOID', // 作废
  ARCHIVED = 'ARCHIVED', // 归档
}

export enum ContractPaymentPlanEnum {
  PENDING = 'PENDING', // 未完成
  PARTIALLY_COMPLETED = 'PARTIALLY_COMPLETED', // 部分完成
  COMPLETED = 'COMPLETED', // 已完成
}

export enum ContractBusinessTitleStatusEnum {
  APPROVED = 'APPROVED', // 通过
  UNAPPROVED = 'UNAPPROVED', // 未通过
  APPROVING = 'APPROVING', // 提审中
  REVOKED = 'REVOKED', // 撤销
}
export enum ContractInvoiceEnum {
  UNDER_REVIEW = 'UNDER_REVIEW', // 审核中
  APPROVED = 'APPROVED', // 已通过
  REJECTED = 'REJECTED', // 已拒绝
}
