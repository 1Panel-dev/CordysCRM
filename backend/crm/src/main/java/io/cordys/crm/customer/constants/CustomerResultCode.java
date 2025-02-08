package io.cordys.crm.customer.constants;

import io.cordys.common.exception.IResultCode;

/**
 * @author jianxing
 */
public enum CustomerResultCode implements IResultCode {

    CUSTOMER_EXIST(102001, "customer.exist"),
    ;


    private final int code;
    private final String message;

    CustomerResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return getTranslationMessage(this.message);
    }
}
