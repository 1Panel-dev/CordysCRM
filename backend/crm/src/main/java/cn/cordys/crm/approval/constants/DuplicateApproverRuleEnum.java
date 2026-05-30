package cn.cordys.crm.approval.constants;

/**
 * 重复审批人规则枚举
 */
public enum DuplicateApproverRuleEnum {

    /** 仅首个节点需审批，后续自动同意 */
    FIRST_ONLY,
    /** 仅连续审批时自动同意 */
    SEQUENTIAL_ALL,
    /** 每个节点都需要审批 */
    EACH
}