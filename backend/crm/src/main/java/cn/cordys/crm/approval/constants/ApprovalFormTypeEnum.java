package cn.cordys.crm.approval.constants;

/**
 * 表单类型枚举
 */
public enum ApprovalFormTypeEnum {

    /** 报价 */
    QUOTATION("QTE-APV"),
    /** 合同 */
    CONTRACT("CTR-APV"),
    /** 发票 */
    INVOICE("INV-APV"),
    /** 订单 */
    ORDER("ORD-APV");

    private final String prefix;

    ApprovalFormTypeEnum(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }
}