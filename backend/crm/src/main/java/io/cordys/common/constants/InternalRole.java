package io.cordys.common.constants;

/**
 * 系统内置角色ID
 * @author jianxing
 */
public enum InternalRole {
    ORG_ADMIN("org_admin"),
    SALES_MANAGER("sales_manager"),
    SALES_STAFF("sales_staff");

    private String value;

    InternalRole(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
