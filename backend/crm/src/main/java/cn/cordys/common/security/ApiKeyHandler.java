package cn.cordys.common.security;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;

/**
 * 根据 Authorization 请求头格式识别 API Key 调用。
 */
public class ApiKeyHandler {

    public static final String AUTHORIZATION = "Authorization"; // 授权字段

    /**
     * 判断 Authorization 按冒号分割后是否至少包含两段；不校验密钥或签名的有效性。
     *
     * @param request HTTP 请求
     * @return 请求头符合上述格式时返回 {@code true}，请求或请求头为空时返回 {@code false}
     */
    public static Boolean isApiKeyCall(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String authorization = request.getHeader(AUTHORIZATION);
        return !StringUtils.isBlank(authorization) && authorization.split(":").length >= 2;
    }
}
