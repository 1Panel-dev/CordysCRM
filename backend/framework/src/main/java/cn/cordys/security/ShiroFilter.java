package cn.cordys.security;

import cn.cordys.common.util.CommonBeanFactory;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用于管理应用程序中过滤器链的工具类。
 * 包含加载基础过滤器链和忽略 CSRF 过滤器链的方法。
 */
public final class ShiroFilter {
    private static final Map<String, String> FILTER_CHAIN_DEFINITION_MAP = new ConcurrentHashMap<>();

    // 私有构造函数防止实例化
    private ShiroFilter() {
        throw new AssertionError("工具类不应该被实例化");
    }

    /**
     * 初始化过滤器链
     */
    public static void chain() {
        boolean isValid = CommonBeanFactory.packageExists("cn.cordys.xpack");
        if (isValid) {
            try {
                Class.forName("cn.cordys.xpack.crm.config.CsChain");
            } catch (ClassNotFoundException ignored) {
                // 类未找到时不执行任何操作
            }
        }
    }

    /**
     * 添加URL过滤规则
     *
     * @param url  需要过滤的URL路径
     * @param rule 过滤规则
     */
    public static void putFilter(String url, String rule) {
        if (url != null && rule != null) {
            FILTER_CHAIN_DEFINITION_MAP.put(url, rule);
        }
    }

    /**
     * 加载应用程序的基础过滤器链。
     * 该过滤器链是一个映射，关联 URL 模式和过滤规则。
     *
     * @return 返回一个不可变Map，包含过滤器链定义，键是 URL 模式，值是关联的过滤规则。
     */
    public static Map<String, String> loadBaseFilterChain() {
        // 静态资源路径
        addStaticResourceFilters();

        // 认证相关路径
        addAuthenticationFilters();

        // 其他公共路径
        addPublicPathFilters();

        return Collections.unmodifiableMap(FILTER_CHAIN_DEFINITION_MAP);
    }

    /**
     * 添加静态资源过滤器规则
     */
    private static void addStaticResourceFilters() {
        FILTER_CHAIN_DEFINITION_MAP.put("/web/**", "anon");
        FILTER_CHAIN_DEFINITION_MAP.put("/mobile/**", "anon");
        FILTER_CHAIN_DEFINITION_MAP.put("/static/**", "anon");
        FILTER_CHAIN_DEFINITION_MAP.put("/templates/**", "anon");
        FILTER_CHAIN_DEFINITION_MAP.put("/*.html", "anon");
        FILTER_CHAIN_DEFINITION_MAP.put("/css/**", "anon");
        FILTER_CHAIN_DEFINITION_MAP.put("/js/**", "anon");
        FILTER_CHAIN_DEFINITION_MAP.put("/images/**", "anon");
        FILTER_CHAIN_DEFINITION_MAP.put("/assets/**", "anon");
        FILTER_CHAIN_DEFINITION_MAP.put("/fonts/**", "anon");
        FILTER_CHAIN_DEFINITION_MAP.put("/favicon.ico", "anon");
        FILTER_CHAIN_DEFINITION_MAP.put("/logo.*", "anon");
        FILTER_CHAIN_DEFINITION_MAP.put("/base-display/**", "anon");
        FILTER_CHAIN_DEFINITION_MAP.put("/cordys/**", "anon");
    }

    /**
     * 添加认证相关过滤器规则
     */
    private static void addAuthenticationFilters() {
        FILTER_CHAIN_DEFINITION_MAP.put("/login", "anon");
        FILTER_CHAIN_DEFINITION_MAP.put("/logout", "anon");
        FILTER_CHAIN_DEFINITION_MAP.put("/is-login", "anon");
        FILTER_CHAIN_DEFINITION_MAP.put("/get-key", "anon");
        FILTER_CHAIN_DEFINITION_MAP.put("/403", "anon");
        FILTER_CHAIN_DEFINITION_MAP.put("/sso/callback/**", "anon");
    }

    /**
     * 添加其他公共路径过滤器规则
     */
    private static void addPublicPathFilters() {
        FILTER_CHAIN_DEFINITION_MAP.put("/display/info", "anon");
        FILTER_CHAIN_DEFINITION_MAP.put("/pic/preview/**", "anon");
        FILTER_CHAIN_DEFINITION_MAP.put("/attachment/preview/**", "anon");
        FILTER_CHAIN_DEFINITION_MAP.put("/ui/display/preview", "anon");
        FILTER_CHAIN_DEFINITION_MAP.put("/ui/display/info", "anon");
        FILTER_CHAIN_DEFINITION_MAP.put("/anonymous/**", "anon");
        FILTER_CHAIN_DEFINITION_MAP.put("/system/version/current", "anon");
        FILTER_CHAIN_DEFINITION_MAP.put("/sse/subscribe/**", "anon");
        FILTER_CHAIN_DEFINITION_MAP.put("/sse/close/**", "anon");
        FILTER_CHAIN_DEFINITION_MAP.put("/sse/broadcast/**", "anon");
        FILTER_CHAIN_DEFINITION_MAP.put("/organization/settings/third-party/types", "anon");
        FILTER_CHAIN_DEFINITION_MAP.put("/organization/settings/third-party/get/**", "anon");
        FILTER_CHAIN_DEFINITION_MAP.put("/organization/settings/third-party/sync/resource", "anon");
        FILTER_CHAIN_DEFINITION_MAP.put("/license/validate/**", "anon");
        FILTER_CHAIN_DEFINITION_MAP.put("/mcp/**", "anon");
        FILTER_CHAIN_DEFINITION_MAP.put("/opportunity/stage/get", "anon");
    }

    /**
     * 返回忽略 CSRF 保护的过滤器链定义。
     *
     * @return 返回一个不可变Map，包含应绕过 CSRF 检查的 URL 路径的过滤器链定义。
     */
    public static Map<String, String> ignoreCsrfFilter() {
        return Map.of("/", "apikey, authc", "/language", "apikey, authc", "/mock", "apikey, authc");
    }
}