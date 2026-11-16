package cn.cordys.crm.approval.constants;

/**
 * 驳回后重审路径。
 */
public enum ApprovalResubmitModeEnum {

    /** 从提交人节点重新审批。 */
    SUBMITTER,
    /** 从当前审批节点重新审批。 */
    CURRENT_NODE,
    /** 由审批人指定重审起始节点。 */
    SPECIFIED_NODE
}
