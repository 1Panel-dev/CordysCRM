package cn.cordys.crm.contract.constants;


/**
 * 合同审核状态
 */
public enum ContractApprovalStatus {

    /**
     * 审核中
     */
    APPROVING,

    /**
     * 通过
     */
    APPROVED,

    /**
     * 不通过
     */
    UNAPPROVED,
    /**
     * 撤销
     */
    REVOKED,
    /**
     * 当关闭审批时，审批状态为 NONE
     */
    NONE,
}
