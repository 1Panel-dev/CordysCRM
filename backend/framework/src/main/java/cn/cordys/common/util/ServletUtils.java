package cn.cordys.common.util;

import cn.cordys.aspectj.constants.RequestSource;
import cn.cordys.context.OrganizationContext;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 客户端工具类
 */
public class ServletUtils {
    /**
     * @param request 请求
     * @return ua
     */
    public static String getUserAgent(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        return ua != null ? ua : "";
    }

    /**
     * 获得请求
     *
     * @return HttpServletRequest
     */
    public static HttpServletRequest getRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return null;
        }
        return servletRequestAttributes.getRequest();
    }

    public static String getRequestSource() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return OrganizationContext.getRequestSource();
        }
        String source = request.getHeader("X-Request-Source");
        if (StringUtils.isNotBlank(source)) {
            return source;
        }
        if (StringUtils.isNotBlank(request.getHeader("X-Access-Key"))
                && StringUtils.isNotBlank(request.getHeader("X-Secret-Key"))) {
            return RequestSource.API.name();
        }
        return RequestSource.WEB.name();
    }

    public static String getUserAgent() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return null;
        }
        return getUserAgent(request);
    }

    public static String getRequestHost(HttpServletRequest request) {
        String port = ":" + request.getServerPort();
        if (request.getServerPort() == 80 || request.getServerPort() == 443) {
            port = "";
        }
        return request.getScheme() + "://" + request.getServerName() + port;
    }

    public static String getUrl() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return null;
        }

        return getRequestHost(request);
    }
}
