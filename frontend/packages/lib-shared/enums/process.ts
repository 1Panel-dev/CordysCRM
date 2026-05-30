export enum ProcessStatusEnum {
  /** 无 **/
  NONE = 'NONE',
  /** 待提审, 待审批 */
  PENDING = 'PENDING',
  /** 审批中 */
  APPROVING = 'APPROVING',
  /** 已通过 */
  APPROVED = 'APPROVED',
  AUTO_APPROVED = 'AUTO_APPROVED', // 自动通过
  AUTO_UNAPPROVED = 'AUTO_UNAPPROVED', // 自动驳回
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

//  审批类型
export enum ApprovalTypeEnum {
  MANUAL = 'MANUAL', // 人工审批
  AUTO_PASS = 'AUTO_PASS', // 自动通过
  AUTO_REJECT = 'AUTO_REJECT', // 自动拒绝
}

// 审批流节点类型
export enum ApprovalNodeTypeEnum {
  START = 'START', // 开始节点
  APPROVER = 'APPROVER', // 审批节点
  CONDITION = 'CONDITION', // 条件分支
  DEFAULT = 'DEFAULT', // 默认分支
  END = 'END', // 结束节点
}

// 审批人/抄送人来源类型
export enum ApproverTypeEnum {
  SPECIFIED_MEMBER = 'MEMBER', // 指定成员
  DIRECT_SUPERVISOR = 'SUPERIOR', // 直属上级
  CONTINUOUS_SUPERVISOR = 'MULTIPLE_SUPERIOR', // 连续多级上级
  SPECIFIED_DEPARTMENT_LEADER = 'DEPT_HEAD', // 指定部门负责人
  CONTINUOUS_DEPARTMENT_LEADER = 'MULTIPLE_DEPT_HEAD', // 连续多级部门负责人
  ROLE = 'ROLE', // 角色
}

// 连续多级审批方向
export enum ApprovalLevelDirectionEnum {
  BOTTOM_UP = 'BOTTOM_UP', // 从下至上
  TOP_DOWN = 'TOP_DOWN', // 从上至下
}

// 多人审批方式
export enum MultiApproverModeEnum {
  ALL = 'ALL', // 会签
  ANY = 'ANY', // 或签
  SEQUENTIAL = 'SEQUENTIAL', // 依次审批
}

// 审批人为空时的处理方式
export enum EmptyApproverActionEnum {
  AUTO_PASS = 'AUTO_PASS', // 自动通过
  ASSIGN_SPECIFIC = 'ASSIGN_SPECIFIC', // 指定人员处理
  ASSIGN_ADMIN = 'ASSIGN_ADMIN', // 转交审批管理员
}

// 审批人与提交人相同时的处理方式
export enum SameSubmitterActionEnum {
  ALLOW = 'ALLOW', // 由提交人审批
  SKIP = 'SKIP', // 自动跳过
  ASSIGN_SUPERIOR = 'ASSIGN_SUPERIOR', // 转交直属上级审批
}

// 表单字段权限类型
export enum ApprovalFieldPermissionModeEnum {
  HIDDEN = 'HIDDEN', // 隐藏
  VIEW = 'VIEW', // 仅查看
  EDIT = 'EDIT', // 可编辑
}

export enum ApprovalResourceTypeEnum {
  QUOTATION = 'QUOTATION',
  CONTRACT = 'CONTRACT',
  ORDER = 'ORDER',
  INVOICE = 'INVOICE',
  ALL = 'ALL',
}

export enum ApprovalListTypeEnum {
  PENDING = 'pending',
  APPROVAL = 'approved',
  INITIATED = 'initiated',
  COPIED = 'copied',
}
