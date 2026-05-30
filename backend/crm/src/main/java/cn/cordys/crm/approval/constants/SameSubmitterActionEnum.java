package cn.cordys.crm.approval.constants;

/**
 * 审批人与提交人相同时动作枚举
 */
public enum SameSubmitterActionEnum {

    /** 自动跳过 */
    SKIP,
    /** 由提交人审批 */
    ALLOW,
    /** 交给直属上级审批 */
    ASSIGN_SUPERIOR
}