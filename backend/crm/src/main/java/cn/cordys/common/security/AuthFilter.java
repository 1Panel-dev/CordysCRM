package cn.cordys.common.security;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.shiro.web.filter.authc.AuthenticationFilter;
import org.apache.shiro.web.util.WebUtils;

/**
 * @Author: jianxing
 * @CreateTime: 2025-04-23  10:47
 */
public class AuthFilter extends AuthenticationFilter {


    /**
     * 重写 onAccessDenied 方法，避免认证失败返回 302 重定向
     * 没有认证返回 401 状态码
     *
     * @param request
     * @param response
     *
     * @return
     *
     * @throws Exception
     */
    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
        // 根路径 GET 提供前端入口；登录仅由 /login 和已验证的 SSO/API Key 入口处理。
        var httpRequest = WebUtils.toHttp(request);
        if ("GET".equals(httpRequest.getMethod()) && "/".equals(WebUtils.getPathWithinApplication(httpRequest))) {
            return true;
        }
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        httpResponse.setContentType("application/json;charset=UTF-8");
        return false;
    }
}
