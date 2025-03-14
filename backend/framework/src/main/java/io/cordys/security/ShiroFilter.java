package io.cordys.security;

import java.util.HashMap;
import java.util.Map;

/**
 * 用于管理应用程序中过滤器链的工具类。
 * 包含加载基础过滤器链和忽略 CSRF 过滤器链的方法。
 */
public class ShiroFilter {

    /**
     * 加载应用程序的基础过滤器链。
     * 该过滤器链是一个映射，关联 URL 模式和过滤规则。
     *
     * @return 返回一个 Map，包含过滤器链定义，键是 URL 模式，值是关联的过滤规则。
     */
    public static Map<String, String> loadBaseFilterChain() {
        Map<String, String> filterChainDefinitionMap = new HashMap<>();

        filterChainDefinitionMap.put("/web/**", "anon");
        filterChainDefinitionMap.put("/mobile/**", "anon");
        filterChainDefinitionMap.put("/static/**", "anon");

        // 模版文件
        filterChainDefinitionMap.put("/templates/**", "anon");

        // 公共可访问的 URL
        filterChainDefinitionMap.put("/login", "anon");
        filterChainDefinitionMap.put("/signout", "anon");
        filterChainDefinitionMap.put("/is-login", "anon");
        filterChainDefinitionMap.put("/get-key", "anon");

        // 静态资源路径
        filterChainDefinitionMap.put("/*.html", "anon");
        filterChainDefinitionMap.put("/css/**", "anon");
        filterChainDefinitionMap.put("/js/**", "anon");
        filterChainDefinitionMap.put("/images/**", "anon");
        filterChainDefinitionMap.put("/assets/**", "anon");
        filterChainDefinitionMap.put("/fonts/**", "anon");
        filterChainDefinitionMap.put("/display/info", "anon");
        filterChainDefinitionMap.put("/file/preview/**", "anon");

        // 暂定 favicon.ico 和 logo.png 为匿名访问
        filterChainDefinitionMap.put("/favicon.ico", "anon");
        filterChainDefinitionMap.put("/logo.png", "anon");
        filterChainDefinitionMap.put("/base-display/**", "anon");

        // Swagger API 文档相关路径
        filterChainDefinitionMap.put("/swagger-ui.html", "anon");
        filterChainDefinitionMap.put("/swagger-ui/**", "anon");
        filterChainDefinitionMap.put("/api-docs/**", "anon");
        filterChainDefinitionMap.put("/v3/api-docs/**", "anon");

        // 403 错误页面路径
        filterChainDefinitionMap.put("/403", "anon");
        filterChainDefinitionMap.put("/cordys/**", "anon");

        // 匿名路径
        filterChainDefinitionMap.put("/anonymous/**", "anon");
        filterChainDefinitionMap.put("/system/version/current", "anon");
        filterChainDefinitionMap.put("/sse/**", "anon");
        filterChainDefinitionMap.put("/sso/callback/**", "anon");
        filterChainDefinitionMap.put("/organization/config/third/type/list", "anon");
        filterChainDefinitionMap.put("/organization/config/third/by/**", "anon");


        return filterChainDefinitionMap;
    }

    /**
     * 返回忽略 CSRF 保护的过滤器链定义。
     *
     * @return 返回一个 Map，包含应绕过 CSRF 检查的 URL 路径的过滤器链定义。
     */
    public static Map<String, String> ignoreCsrfFilter() {
        Map<String, String> filterChainDefinitionMap = new HashMap<>();

        // 跳过 CSRF 验证的路径
        filterChainDefinitionMap.put("/", "apikey, authc"); // 根路径跳过 CSRF 检查
        filterChainDefinitionMap.put("/language", "apikey, authc"); // /language 路径跳过 CSRF 检查
        filterChainDefinitionMap.put("/mock", "apikey, authc"); // /mock 路径跳过 CSRF 检查

        return filterChainDefinitionMap;
    }
}
