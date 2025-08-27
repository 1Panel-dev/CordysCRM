package cn.cordys.common.security;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.shiro.web.filter.authc.FormAuthenticationFilter;

/**
 * @Author: jianxing
 * @CreateTime: 2025-04-23  10:47
 */
public class AuthFilter extends FormAuthenticationFilter {


    /**
     * 重写 onAccessDenied 方法，避免认证失败返回 302 重定向
     * 没有认证返回 401 状态码
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
        if (isLoginRequest(request, response)) {
            if (isLoginSubmission(request, response)) {
                return executeLogin(request, response);
            } else {
                return true;
            }
        } else {
            // 没有登入返回 401
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.setContentType("application/json;charset=UTF-8");
            return false;
        }
    }
}