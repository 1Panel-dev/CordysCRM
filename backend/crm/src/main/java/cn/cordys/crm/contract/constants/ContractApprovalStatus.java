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

}
