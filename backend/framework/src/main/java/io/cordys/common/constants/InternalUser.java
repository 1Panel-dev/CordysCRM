package io.cordys.common.constants;

import lombok.Getter;

/**
 * 系统内置用户ID
 * @author jianxing
 */
@Getter
public enum InternalUser {
    ADMIN("admin");

    private final String value;

    InternalUser(String value) {
        this.value = value;
    }

}
