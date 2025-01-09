package io.cordys.common.response.handler;

import io.cordys.common.response.result.CrmHttpResultCode;
import lombok.Data;

/**
 * ResultHolder 类用于封装接口响应结果，包括状态码、消息、详细信息和返回数据。
 */
@Data
public class ResultHolder {

    /**
     * 请求是否成功的状态码，默认值为 200（成功）。
     */
    private int code = CrmHttpResultCode.SUCCESS.getCode();

    /**
     * 返回给前端的描述信息，一般是错误信息或成功信息。
     */
    private String message;

    /**
     * 详细描述信息，例如在发生异常时存储异常日志。
     */
    private Object messageDetail;

    /**
     * 返回的数据，可以是任何类型的对象。
     */
    private Object data = "";

    /**
     * 默认构造函数，初始化默认值。
     */
    public ResultHolder() {
    }

    /**
     * 构造函数，初始化返回数据。
     *
     * @param data 返回的数据
     */
    public ResultHolder(Object data) {
        this.data = data;
    }

    /**
     * 构造函数，初始化状态码和消息。
     *
     * @param code 状态码
     * @param msg 消息
     */
    public ResultHolder(int code, String msg) {
        this.code = code;
        this.message = msg;
    }

    /**
     * 构造函数，初始化状态码、消息和数据。
     *
     * @param code 状态码
     * @param msg 消息
     * @param data 返回的数据
     */
    public ResultHolder(int code, String msg, Object data) {
        this.code = code;
        this.message = msg;
        this.data = data;
    }

    /**
     * 构造函数，初始化状态码、消息、详细信息和数据。
     *
     * @param code 状态码
     * @param msg 消息
     * @param messageDetail 详细信息
     * @param data 返回的数据
     */
    public ResultHolder(int code, String msg, Object messageDetail, Object data) {
        this.code = code;
        this.message = msg;
        this.messageDetail = messageDetail;
        this.data = data;
    }

    /**
     * 成功响应，返回带有数据的 ResultHolder。
     *
     * @param obj 返回的数据
     * @return ResultHolder 返回封装的成功响应
     */
    public static ResultHolder success(Object obj) {
        return new ResultHolder(obj);
    }

    /**
     * 错误响应，返回带有状态码和消息的 ResultHolder。
     *
     * @param code 状态码
     * @param message 错误消息
     * @return ResultHolder 返回封装的错误响应
     */
    public static ResultHolder error(int code, String message) {
        return new ResultHolder(code, message, null, null);
    }

    /**
     * 错误响应，返回带有消息和详细信息的 ResultHolder。
     *
     * @param message 错误消息
     * @param messageDetail 错误的详细信息
     * @return ResultHolder 返回封装的错误响应
     */
    public static ResultHolder error(String message, String messageDetail) {
        return new ResultHolder(-1, message, messageDetail, null);
    }

    /**
     * 错误响应，返回带有状态码、消息和详细信息的 ResultHolder。
     *
     * @param code 状态码
     * @param message 错误消息
     * @param messageDetail 错误的详细信息
     * @return ResultHolder 返回封装的错误响应
     */
    public static ResultHolder error(int code, String message, Object messageDetail) {
        return new ResultHolder(code, message, messageDetail, null);
    }

    /**
     * 特殊情况的响应，例如接口可正常返回 HTTP 状态码 200，但需要给前端提供错误信息。
     *
     * @param code 自定义状态码
     * @param message 返回给前端的消息
     * @return ResultHolder 返回封装的响应
     */
    public static ResultHolder successCodeErrorInfo(int code, String message) {
        return new ResultHolder(code, message, null, null);
    }
}
