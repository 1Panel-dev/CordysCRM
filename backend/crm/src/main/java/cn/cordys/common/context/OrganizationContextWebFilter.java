package cn.cordys.common.context;

import cn.cordys.context.OrganizationContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 组织信息及请求来源的 Web 过滤器
 * <p>
 * 根据请求头自动设置组织上下文与请求来源，并在请求结束时清理资源。
 *
 * @author jianxing
 */
public class OrganizationContextWebFilter extends OncePerRequestFilter {

    public static final String ORGANIZATION_ID_HEADER = "Organization-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // 提取所有头信息
        String organizationId = request.getHeader(ORGANIZATION_ID_HEADER);

        // 设置组织 ID
        if (StringUtils.isNotBlank(organizationId)) {
            OrganizationContext.setOrganizationId(organizationId);
        }

        try {
            chain.doFilter(request, response);
        } finally {
            // 保证上下文清理，避免内存泄漏
            OrganizationContext.clear();
        }
    }
}