package cn.cordys.crm.clue.constants;

import cn.cordys.common.exception.IResultCode;

/**
 * @author jianxing
 */
public enum ClueResultCode implements IResultCode {

    CLUE_EXIST(103001, "clue.exist");

    private final int code;
    private final String message;

    ClueResultCode(int code, String message) {
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
