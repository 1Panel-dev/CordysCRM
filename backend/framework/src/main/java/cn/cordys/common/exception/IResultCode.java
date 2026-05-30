package cn.cordys.common.exception;

import cn.cordys.common.util.Translator;

/**
 * API 接口状态码
 *
 * @author jianxing
 * <p>
 * 1. 如果想返回具有 Http 含义的状态码，使用对应实现类
 * 2. 业务状态码，各模块定义自己的状态码枚举类，各自管理
 * 3. 业务错误码，定义规则为 6 位数字
 * 4. 当需要抛出异常时，给异常设置状态码枚举对象
 * <p>
 */
public interface IResultCode {
    /**
     * 返回状态码
     */
    int getCode();

    /**
     * 返回状态码信息
     */
    String getMessage();

    /**
     * 返回国际化后的状态码信息
     * 如果没有匹配则返回原文
     */
    default String getTranslationMessage(String message) {
        return Translator.get(message, message);
    }
}
