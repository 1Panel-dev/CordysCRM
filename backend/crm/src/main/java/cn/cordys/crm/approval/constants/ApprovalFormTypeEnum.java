package cn.cordys.crm.approval.constants;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.ValueEnum;
import org.apache.commons.lang3.StringUtils;

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
    ORDER("ORD-APV", "ORDER", "order"),
    /** 自定义表单（审批 formType 为具体 customFormId，本枚举用于权限/编码等通用配置） */
    CUSTOM_FORM("CFM-APV", "CUSTOM_FORM", "customForm");

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
        // 非标准枚举值视为自定义表单（formType 为具体 customFormId）
        if (StringUtils.isNotBlank(value) && FormKey.ofKey(value) == null) {
            return CUSTOM_FORM;
        }
        return null;
    }

    public String getValue() {
        return value;
    }
}