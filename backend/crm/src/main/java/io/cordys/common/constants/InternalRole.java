package io.cordys.common.constants;

import lombok.Getter;

/**
 * 系统内置角色ID
 *
 * @author jianxing
 */
@Getter
public enum InternalRole {
    ORG_ADMIN("org_admin"),
    SALES_MANAGER("sales_manager"),
    SALES_STAFF("sales_staff");

    private final String value;

    InternalRole(String value) {
        this.value = value;
    }

}
