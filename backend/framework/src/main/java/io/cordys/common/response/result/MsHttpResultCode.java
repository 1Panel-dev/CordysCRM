package io.cordys.common.response.result;

import io.cordys.common.exception.IResultCode;

/**
 * <p>表示 HTTP 状态码的枚举，主要用于在抛出异常时，自动设置 HTTP 响应状态码为对应状态码的后三位数字。</p>
 * <p>枚举中的每个状态码代表一个 HTTP 请求的响应状态，常用于 REST API。</p>
 *
 * <p>状态码采用 100 系列，前三位代表业务域，后三位代表具体的 HTTP 状态码：</p>
 * <ul>
 *     <li>成功：100200</li>
 *     <li>失败：100500</li>
 *     <li>验证失败：100400</li>
 *     <li>未授权：100401</li>
 *     <li>禁止访问：100403</li>
 *     <li>未找到：100404</li>
 * </ul>
 *
 * <p>实现 {@link IResultCode} 接口，用于标准化异常处理。</p>
 *
 * @author jianxing
 * @see IResultCode
 * @see MsHttpResultCode#SUCCESS
 * @see MsHttpResultCode#FAILED
 * @see MsHttpResultCode#VALIDATE_FAILED
 * @see MsHttpResultCode#UNAUTHORIZED
 * @see MsHttpResultCode#FORBIDDEN
 * @see MsHttpResultCode#NOT_FOUND
 */
public enum MsHttpResultCode implements IResultCode {

    /**
     * 请求成功
     */
    SUCCESS(100200, "http_result_success"),

    /**
     * 请求失败，未知异常
     */
    FAILED(100500, "http_result_unknown_exception"),

    /**
     * 验证失败
     */
    VALIDATE_FAILED(100400, "http_result_validate"),

    /**
     * 未授权，需登录
     */
    UNAUTHORIZED(100401, "http_result_unauthorized"),

    /**
     * 禁止访问
     */
    FORBIDDEN(100403, "http_result_forbidden"),

    /**
     * 资源未找到
     */
    NOT_FOUND(100404, "http_result_not_found");

    private final int code;
    private final String message;

    /**
     * 枚举构造函数
     *
     * @param code    HTTP 状态码
     * @param message 状态码对应的消息键
     */
    MsHttpResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 获取 HTTP 状态码
     *
     * @return HTTP 状态码的数值
     */
    @Override
    public int getCode() {
        return code;
    }

    /**
     * 获取状态码的消息
     *
     * @return 状态码对应的消息，经过翻译处理
     */
    @Override
    public String getMessage() {
        return getTranslationMessage(this.message);
    }
}
