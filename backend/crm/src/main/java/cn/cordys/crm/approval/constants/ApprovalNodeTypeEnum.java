package cn.cordys.crm.approval.constants;

/**
 * 审批节点类型枚举
 */
public enum ApprovalNodeTypeEnum {

    /** 开始节点 */
    START,
    /** 审批人节点 */
    APPROVER,
    /** 触发条件节点 */
    CONDITION,
    /** 默认节点，条件不满足时的节点 */
    DEFAULT,
    /** 结束节点 */
    END
}