package io.cordys.common.security;

import io.cordys.common.util.CodingUtils;
import io.cordys.common.util.CommonBeanFactory;
import io.cordys.security.SessionConstants;
import io.cordys.security.SessionUser;
import io.cordys.security.SessionUtils;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.web.filter.authc.AnonymousFilter;
import org.apache.shiro.web.util.WebUtils;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;

/**
 * 自定义过滤器，用于处理 CSRF 校验。
 * 该过滤器确保只有认证用户才能访问非公开资源，并对请求中的 CSRF token 和 Referer 进行校验。
 */
public class CsrfFilter extends AnonymousFilter {

    /**
     * 在处理请求之前进行 CSRF 校验。
     * 如果请求没有通过认证或请求头中包含有效的 CSRF token，则允许请求继续。
     * 否则，抛出相应的异常或返回无效的认证状态。
     *
     * @param request     Servlet 请求
     * @param response    Servlet 响应
     * @param mappedValue 过滤器链中的映射值
     * @return true 如果请求继续处理，false 如果请求被中断
     */
    @Override
    protected boolean onPreHandle(ServletRequest request, ServletResponse response, Object mappedValue) {
        HttpServletRequest httpServletRequest = WebUtils.toHttp(request);

        // 如果用户未认证，返回认证无效状态
        if (!SecurityUtils.getSubject().isAuthenticated()) {
            ((HttpServletResponse) response).setHeader(SessionConstants.AUTHENTICATION_STATUS, SessionConstants.AUTHENTICATION_INVALID);
            return true;
        }

        // 错误页面无需 CSRF 校验
        if (httpServletRequest.getRequestURI().equals("/error")) {
            return true;
        }

        // API 请求无需 CSRF 校验
        if (ApiKeyHandler.isApiKeyCall(httpServletRequest)) {
            return true;
        }

        // WebSocket 请求无需 CSRF 校验
        String websocketKey = httpServletRequest.getHeader("Sec-WebSocket-Key");
        if (StringUtils.isNotBlank(websocketKey)) {
            return true;
        }

        // 获取请求头中的 CSRF token 和 X-Auth-Token
        String csrfToken = httpServletRequest.getHeader(SessionConstants.CSRF_TOKEN);
        String xAuthToken = httpServletRequest.getHeader(SessionConstants.HEADER_TOKEN);

        // 校验 CSRF token 和 X-Auth-Token
        validateToken(csrfToken, xAuthToken);

        // 校验 Referer
        validateReferer(httpServletRequest);

        return true;
    }

    /**
     * 校验请求中的 Referer 是否符合配置的域名要求。
     * 如果没有配置 Referer 域名，默认不进行校验。
     *
     * @param request HttpServletRequest 请求
     */
    private void validateReferer(HttpServletRequest request) {
        Environment env = CommonBeanFactory.getBean(Environment.class);
        assert env != null;
        String domains = env.getProperty("referer.urls");

        // 如果没有配置 referer.urls，则不校验
        if (StringUtils.isBlank(domains)) {
            return;
        }

        String[] allowedDomains = StringUtils.split(domains, ",");
        String referer = request.getHeader(HttpHeaders.REFERER);

        // 校验 Referer 是否在允许的域名列表中
        if (allowedDomains != null && !ArrayUtils.contains(allowedDomains, referer)) {
            throw new RuntimeException("CSRF error: invalid referer");
        }
    }

    /**
     * 校验请求中的 CSRF token 和 X-Auth-Token 是否有效。
     * 如果 token 无效，抛出异常。
     *
     * @param csrfToken  CSRF token
     * @param xAuthToken X-Auth-Token
     */
    private void validateToken(String csrfToken, String xAuthToken) {
        if (StringUtils.isBlank(csrfToken)) {
            throw new RuntimeException("CSRF token is empty");
        }

        // 解密 CSRF token
        csrfToken = CodingUtils.aesDecrypt(csrfToken, SessionUser.secret, CodingUtils.generateIv());

        String[] signatureArray = StringUtils.split(StringUtils.trimToNull(csrfToken), "|");
        if (signatureArray.length != 4) {
            throw new RuntimeException("Invalid CSRF token format");
        }

        // 校验用户 ID 和会话 ID 是否匹配
        if (!Strings.CS.equals(SessionUtils.getUserId(), signatureArray[0])) {
            throw new RuntimeException("CSRF token does not match the current user");
        }

        // 校验 sessionId 或 X-Auth-Token 是否匹配
        if (!Strings.CS.equals(SessionUtils.getSessionId(), signatureArray[2]) &&
                !Strings.CS.equals(xAuthToken, signatureArray[2])) {
            throw new RuntimeException("CSRF token does not match the current session");
        }
    }
}
