package cn.cordys.crm.approval.constants;

/**
 * 审批人为空时动作枚举
 */
public enum EmptyApproverActionEnum {

    /** 自动通过 */
    AUTO_PASS,
    /** 指定人员审批 */
    ASSIGN_SPECIFIC,
    /** 交给审批管理员 */
    ASSIGN_ADMIN
}