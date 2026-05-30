package cn.cordys.crm.system.constants;

import lombok.Getter;

/**
 * LicenseStatus
 */

@Getter
public enum LicenseStatus {
    VALID("valid"),
    INVALID("invalid"),
    EXPIRED("expired"),
    NOT_FOUND("not_found");

    private final String name;

    LicenseStatus(String status) {
        this.name = status;
    }

}
