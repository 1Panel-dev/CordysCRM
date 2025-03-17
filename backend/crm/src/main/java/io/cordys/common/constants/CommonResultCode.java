package io.cordys.common.constants;

import io.cordys.common.exception.IResultCode;

/**
 * 通用功能状态码
 * 通用功能返回的状态码
 * @author jianxing
 */
public enum CommonResultCode implements IResultCode {

    FIELD_VALIDATE_ERROR(100001, "field_validate_error");


    private final int code;
    private final String message;

    CommonResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
