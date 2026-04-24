package cn.cordys.crm.approval.constants;

import cn.cordys.common.constants.ValueEnum;

/**
 * 表单类型枚举
 */
public enum ApprovalFormTypeEnum implements ValueEnum<String> {

    /** 报价 */
    QUOTATION("QTE-APV", "OPPORTUNITY_MANAGEMENT_QUOTATION", "quotation"),
    /** 合同 */
    CONTRACT("CTR-APV", "CONTRACT", "contract"),
    /** 发票 */
    INVOICE("INV-APV", "CONTRACT_INVOICE", "invoice"),
    /** 订单 */
    ORDER("ORD-APV", "ORDER", "order");

    private final String prefix;
    private final String permissionId;
    private final String value;

    ApprovalFormTypeEnum(String prefix, String permissionId, String value) {
        this.prefix = prefix;
        this.permissionId = permissionId;
        this.value = value;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getPermissionId() {
        return permissionId;
    }

    public static ApprovalFormTypeEnum getByValue(String value) {
        for (ApprovalFormTypeEnum type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }

    public String getValue() {
        return value;
    }
}