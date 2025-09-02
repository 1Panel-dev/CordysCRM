package cn.cordys.crm.customer.constants;

import cn.cordys.common.exception.IResultCode;

/**
 * @author jianxing
 */
public enum CustomerResultCode implements IResultCode {

    CUSTOMER_EXIST(102001, "customer.exist"),
    CUSTOMER_CONTACT_EXIST(102002, "customer_contact.exist"),
    CUSTOMER_RESOURCE_REF(102003, "customer.ref_resource.exist");

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
