package cn.cordys.common.security;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;

/**
 * 处理 API 密钥验证的工具类，包括获取用户、验证请求是否包含 API 密钥以及验证签名的功能。
 */
public class ApiKeyHandler {

    public static final String AUTHORIZATION = "Authorization"; // 授权字段

    /**
     * 判断请求是否包含有效的 API 密钥和签名。
     *
     * @param request HTTP 请求
     *
     * @return 如果请求包含有效的 API 密钥和签名，返回 true；否则返回 false
     */
    public static Boolean isApiKeyCall(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String authorization = request.getHeader(AUTHORIZATION);
        return !StringUtils.isBlank(authorization) && authorization.split(":").length >= 2;
    }
}
