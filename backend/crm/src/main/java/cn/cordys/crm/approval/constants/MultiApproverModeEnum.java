package cn.cordys.crm.approval.constants;

/**
 * 多人审批方式枚举
 */
public enum MultiApproverModeEnum {

    /** 会签（需所有审批人同意） */
    ALL,
    /** 或签（仅需一名审批人同意） */
    ANY,
    /** 依次审批 */
    SEQUENTIAL
}