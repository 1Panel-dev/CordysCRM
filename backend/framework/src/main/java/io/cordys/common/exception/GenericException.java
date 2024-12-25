package io.cordys.common.exception;

import org.apache.commons.lang3.StringUtils;

/**
 * SystemException 是自定义的运行时异常，包含错误代码和详细信息。
 */
public class GenericException extends RuntimeException {

    /**
     * 错误代码
     */
    protected IResultCode errorCode;

    /**
     * 构造方法，接受错误信息。
     *
     * @param message 错误信息
     */
    public GenericException(String message) {
        super(message);
    }

    /**
     * 构造方法，接受一个异常对象。
     *
     * @param t 异常对象
     */
    public GenericException(Throwable t) {
        super(t);
    }

    /**
     * 构造方法，接受错误代码，默认没有详细信息。
     *
     * @param errorCode 错误代码
     */
    public GenericException(IResultCode errorCode) {
        super(StringUtils.EMPTY);
        if (errorCode == null) {
            throw new IllegalArgumentException("errorCode cannot be null");
        }
        this.errorCode = errorCode;
    }

    /**
     * 构造方法，接受错误代码和自定义错误信息。
     *
     * @param errorCode 错误代码
     * @param message   错误信息
     */
    public GenericException(IResultCode errorCode, String message) {
        super(message);
        if (errorCode == null) {
            throw new IllegalArgumentException("errorCode cannot be null");
        }
        this.errorCode = errorCode;
    }

    /**
     * 构造方法，接受错误代码和异常对象。
     *
     * @param errorCode 错误代码
     * @param t         异常对象
     */
    public GenericException(IResultCode errorCode, Throwable t) {
        super(t);
        if (errorCode == null) {
            throw new IllegalArgumentException("errorCode cannot be null");
        }
        this.errorCode = errorCode;
    }

    /**
     * 构造方法，接受自定义错误信息和异常对象。
     *
     * @param message 错误信息
     * @param t       异常对象
     */
    public GenericException(String message, Throwable t) {
        super(message, t);
    }

    /**
     * 获取错误代码。
     *
     * @return 错误代码
     */
    public IResultCode getErrorCode() {
        return errorCode;
    }

    /**
     * 重写toString方法，提供更有用的错误信息。
     *
     * @return 错误代码和错误信息
     */
    @Override
    public String toString() {
        return "MSException{errorCode=" + errorCode + ", message=" + getMessage() + "}";
    }
}
