export enum ArchiveStatusEnum {
  ARCHIVED = 'ARCHIVED', // 已归档
  UN_ARCHIVED = 'UN_ARCHIVED', // 未归档
}

export enum ContractStatusEnum {
  SIGNED = 'SIGNED', // 已签署
  IN_PROGRESS = 'IN_PROGRESS', // 履行中
  COMPLETED_PERFORMANCE = 'COMPLETED_PERFORMANCE', // 履行完毕
  VOID = 'VOID', // 作废
}

export enum ContractPaymentPlanEnum {
  PENDING = 'PENDING', // 未完成
  PARTIALLY_COMPLETED = 'PARTIALLY_COMPLETED', // 部分完成
  COMPLETED = 'COMPLETED', // 已完成
}
