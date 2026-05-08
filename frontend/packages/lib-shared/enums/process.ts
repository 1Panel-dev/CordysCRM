export enum ProcessStatusEnum {
  /** 无 **/
  NONE = 'NONE',
  /** 待提审, 待审批 */
  PENDING = 'PENDING',
  /** 审批中 */
  APPROVING = 'APPROVING',
  /** 已通过 */
  APPROVED = 'APPROVED',
  /** 已驳回 */
  UNAPPROVED = 'UNAPPROVED',
  /** 已撤销 */
  REVOKED = 'REVOKED',
}

export enum ApprovalOperationEnum {
  APPROVE = 'APPROVE', // 通过
  REJECT = 'REJECT', // 驳回
  SIGN = 'SIGN', // 加签
  BACK = 'BACK', // 退回
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

export enum ProcessTypeEnum {
  OR_APPROVAL = 'OR_APPROVAL', // 或签
  AND_APPROVAL = 'AND_APPROVAL', // 会签
  SEQUENTIAL_APPROVAL = 'SEQUENTIAL_APPROVAL', // 依次审批
  COUNTERSIGNATURE = 'COUNTERSIGNATURE', // 加签
}
