package cn.cordys.common.context;

import cn.cordys.common.constants.RequestSource;
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
    public static final String ACCESS_KEY_HEADER = "X-Access-Key";
    public static final String SECRET_KEY_HEADER = "X-Secret-Key";
    public static final String REQUEST_SOURCE_HEADER = "X-Request-Source";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // 提取所有头信息
        String organizationId = request.getHeader(ORGANIZATION_ID_HEADER);
        String accessKey = request.getHeader(ACCESS_KEY_HEADER);
        String secretKey = request.getHeader(SECRET_KEY_HEADER);
        String requestSource = request.getHeader(REQUEST_SOURCE_HEADER);

        // 设置组织 ID
        if (StringUtils.isNotBlank(organizationId)) {
            OrganizationContext.setOrganizationId(organizationId);
        }

        // 设置请求来源
        String source = resolveRequestSource(requestSource, accessKey, secretKey);
        OrganizationContext.setRequestSource(source);

        try {
            chain.doFilter(request, response);
        } finally {
            // 保证上下文清理，避免内存泄漏
            OrganizationContext.clear();
        }
    }

    /**
     * 根据请求头解析请求来源，优先级：
     * 1. 明确指定的 X-Request-Source
     * 2. 携带鉴权密钥对 (Access/Secret) 时视为 API 调用
     * 3. 否则默认为 WEB
     */
    private String resolveRequestSource(String requestSource, String accessKey, String secretKey) {
        if (StringUtils.isNotBlank(requestSource)) {
            return requestSource;
        }
        if (StringUtils.isNotBlank(accessKey) && StringUtils.isNotBlank(secretKey)) {
            return RequestSource.API.name();
        }
        return RequestSource.WEB.name();
    }
}