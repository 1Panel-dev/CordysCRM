package cn.cordys.crm.approval.constants;

/**
 * 表单类型枚举
 */
public enum ApprovalFormTypeEnum {

    /** 报价 */
    QUOTATION("QTE-APV", "OPPORTUNITY_MANAGEMENT_QUOTATION"),
    /** 合同 */
    CONTRACT("CTR-APV", "CONTRACT"),
    /** 发票 */
    INVOICE("INV-APV", "CONTRACT_INVOICE"),
    /** 订单 */
    ORDER("ORD-APV", "ORDER");

    private final String prefix;
    private final String permissionId;

    ApprovalFormTypeEnum(String prefix, String permissionId) {
        this.prefix = prefix;
        this.permissionId = permissionId;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getPermissionId() {
        return permissionId;
    }
}