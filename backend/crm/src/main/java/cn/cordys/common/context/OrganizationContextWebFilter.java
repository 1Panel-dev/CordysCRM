package cn.cordys.common.context;

import cn.cordys.context.OrganizationContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;

/**
 * 组织信息及请求来源的 Web 过滤器
 * <p>
 * 根据请求头自动设置组织上下文与请求来源，并在请求结束时清理资源。
 *
 * @author jianxing
 */
public class OrganizationContextWebFilter extends OncePerRequestFilter {

    public static final String ORGANIZATION_ID_HEADER = "Organization-Id";
    private static final Locale DEFAULT_LOCALE = Locale.SIMPLIFIED_CHINESE;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // 提取所有头信息
        String organizationId = request.getHeader(ORGANIZATION_ID_HEADER);
        String acceptLanguage = request.getHeader(HttpHeaders.ACCEPT_LANGUAGE);
        Locale locale = parseLocale(acceptLanguage);

        // 设置组织 ID
        if (StringUtils.isNotBlank(organizationId)) {
            OrganizationContext.setOrganizationId(organizationId);
        }
        LocaleContextHolder.setLocale(locale);

        try {
            chain.doFilter(request, response);
        } finally {
            // 保证上下文清理，避免内存泄漏
            OrganizationContext.clear();
            LocaleContextHolder.resetLocaleContext();
        }
    }

    private Locale parseLocale(String acceptLanguage) {
        if (StringUtils.isBlank(acceptLanguage)) {
            return DEFAULT_LOCALE;
        }
        String primaryLanguage = StringUtils.trimToEmpty(acceptLanguage);
        int commaIndex = primaryLanguage.indexOf(',');
        if (commaIndex >= 0) {
            primaryLanguage = primaryLanguage.substring(0, commaIndex);
        }
        int semicolonIndex = primaryLanguage.indexOf(';');
        if (semicolonIndex >= 0) {
            primaryLanguage = primaryLanguage.substring(0, semicolonIndex);
        }
        String fixedLanguage = StringUtils.trimToEmpty(primaryLanguage);
        if (StringUtils.isBlank(fixedLanguage)) {
            return DEFAULT_LOCALE;
        }

        Locale locale = Locale.forLanguageTag(fixedLanguage.replace('_', '-'));
        if (StringUtils.isBlank(locale.getLanguage())) {
            return DEFAULT_LOCALE;
        }
        return locale;
    }
}
