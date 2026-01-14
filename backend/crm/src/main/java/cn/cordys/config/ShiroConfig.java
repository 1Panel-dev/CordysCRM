package cn.cordys.config;

import cn.cordys.common.security.ApiKeyFilter;
import cn.cordys.common.security.AuthFilter;
import cn.cordys.common.security.CsrfFilter;
import cn.cordys.common.security.realm.LocalRealm;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.security.ShiroFilter;
import jakarta.servlet.Filter;
import org.apache.shiro.aop.AnnotationResolver;
import org.apache.shiro.authz.aop.*;
import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.cache.MemoryConstrainedCacheManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.spring.LifecycleBeanPostProcessor;
import org.apache.shiro.spring.aop.SpringAnnotationResolver;
import org.apache.shiro.spring.security.interceptor.AopAllianceAnnotationsAuthorizingMethodInterceptor;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.session.mgt.ServletContainerSessionManager;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.web.filter.DelegatingFilterProxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static jakarta.servlet.DispatcherType.ASYNC;

/**
 * Shiro 配置类，用于配置 Shiro 的安全管理器、会话管理器、过滤器等。
 * <p>
 * 本类负责配置 Shiro 相关的 Bean，包括会话管理、缓存管理、安全过滤器链等。
 * 它还定义了授权和认证的处理逻辑，以及注解支持。
 * </p>
 *
 * @version 1.0
 */
@Configuration
public class ShiroConfig {

    /**
     * 配置 Shiro 的过滤器工厂。
     * <p>
     * 设置登录页、未授权页面、过滤器链等配置。还配置了 API Key 和 CSRF 防护的过滤器。
     * </p>
     *
     * @param sessionManager 默认的 Web 安全管理器
     *
     * @return 配置好的 {@link ShiroFilterFactoryBean} 实例
     */
    @Bean
    public ShiroFilterFactoryBean shiroFilterFactoryBean(final DefaultWebSecurityManager sessionManager) {
        final var bean = new ShiroFilterFactoryBean();
        bean.setLoginUrl("/");
        bean.setSecurityManager(sessionManager);
        bean.setUnauthorizedUrl("/403");
        bean.setSuccessUrl("/");

        final Map<String, String> chain = bean.getFilterChainDefinitionMap();
        final Map<String, Filter> filters = bean.getFilters();

        filters.put("apikey", new ApiKeyFilter());
        filters.put("csrf", new CsrfFilter());
        filters.put("authc", new AuthFilter());

        chain.putAll(ShiroFilter.loadBaseFilterChain());
        chain.putAll(ShiroFilter.ignoreCsrfFilter());

        // 配置自定义的过滤器链
        configureXFilter(chain, filters);

        return bean;
    }

    private void configureXFilter(Map<String, String> chain, Map<String, Filter> filters) {
        String pattern = "apikey, csrf, authc";
        final Filter preApiKey = CommonBeanFactory.getFilter();
        if (preApiKey != null) {
            filters.put("preApikey", preApiKey);
            pattern = "preApikey, " + pattern;
        }
        chain.put("/**", pattern);
    }


    /**
     * 配置 Shiro 的缓存管理器，使用内存缓存管理。
     *
     * @return 配置好的 {@link MemoryConstrainedCacheManager} 实例
     */
    @Bean
    public MemoryConstrainedCacheManager memoryConstrainedCacheManager() {
        return new MemoryConstrainedCacheManager();
    }

    /**
     * 配置 Shiro 的会话管理器。
     * <p>
     * 使用 {@link ServletContainerSessionManager} 来管理 Web 会话。
     * </p>
     *
     * @return 配置好的 {@link SessionManager} 实例
     */
    @Bean
    public SessionManager sessionManager() {
        return new ServletContainerSessionManager();
    }

    /**
     * 配置 Shiro 的安全管理器。
     * <p>
     * 在安全管理器中设置会话管理器、缓存管理器和自定义的 Realm。
     * </p>
     *
     * @param sessionManager 会话管理器
     * @param cacheManager   缓存管理器
     * @param localRealm     自定义 Realm 实例
     *
     * @return 配置好的 {@link DefaultWebSecurityManager} 实例
     */
    @Bean(name = "securityManager")
    public DefaultWebSecurityManager securityManager(SessionManager sessionManager, CacheManager cacheManager, Realm localRealm) {
        DefaultWebSecurityManager defaultManager = new DefaultWebSecurityManager();
        defaultManager.setSessionManager(sessionManager);
        defaultManager.setCacheManager(cacheManager);
        defaultManager.setRealm(localRealm);
        return defaultManager;
    }

    /**
     * 配置 Shiro 的自定义 Realm，用于认证和授权逻辑。
     * <p>
     * 自定义的 {@link LocalRealm} 实现了 Shiro 的 Realm 接口，用于处理用户的认证和授权。
     * </p>
     *
     * @return 配置好的 {@link LocalRealm} 实例
     */
    @Bean
    @DependsOn("lifecycleBeanPostProcessor")
    public LocalRealm localRealm() {
        return new LocalRealm();
    }

    /**
     * 配置 Shiro 的生命周期 Bean 后处理器，用于管理 Shiro 的生命周期。
     *
     * @return 配置好的 {@link LifecycleBeanPostProcessor} 实例
     */
    @Bean(name = "lifecycleBeanPostProcessor")
    public LifecycleBeanPostProcessor lifecycleBeanPostProcessor() {
        return new LifecycleBeanPostProcessor();
    }

    /**
     * 配置 Shiro 的默认代理自动创建器，用于支持方法级的注解授权。
     *
     * @return 配置好的 {@link DefaultAdvisorAutoProxyCreator} 实例
     */
    @Bean
    @DependsOn({"lifecycleBeanPostProcessor"})
    public DefaultAdvisorAutoProxyCreator getDefaultAdvisorAutoProxyCreator() {
        DefaultAdvisorAutoProxyCreator daap = new DefaultAdvisorAutoProxyCreator();
        daap.setProxyTargetClass(true);
        return daap;
    }

    /**
     * 配置 Shiro 的授权注解支持。
     * <p>
     * 使用 {@link AuthorizationAttributeSourceAdvisor} 和 {@link AopAllianceAnnotationsAuthorizingMethodInterceptor}
     * 配置 Shiro 的角色和认证注解。
     * </p>
     *
     * @param sessionManager 默认的 Web 安全管理器
     *
     * @return 配置好的 {@link AuthorizationAttributeSourceAdvisor} 实例
     */
    @Bean
    public AuthorizationAttributeSourceAdvisor getAuthorizationAttributeSourceAdvisor(DefaultWebSecurityManager sessionManager) {
        AuthorizationAttributeSourceAdvisor aasa = new AuthorizationAttributeSourceAdvisor();
        aasa.setSecurityManager(sessionManager);

        // 配置注解拦截器
        AopAllianceAnnotationsAuthorizingMethodInterceptor advice = new AopAllianceAnnotationsAuthorizingMethodInterceptor();
        List<AuthorizingAnnotationMethodInterceptor> interceptors = new ArrayList<>(5);

        AnnotationResolver resolver = new SpringAnnotationResolver();
        interceptors.add(new RoleAnnotationMethodInterceptor(resolver));
        interceptors.add(new AuthenticatedAnnotationMethodInterceptor(resolver));
        interceptors.add(new UserAnnotationMethodInterceptor(resolver));
        interceptors.add(new GuestAnnotationMethodInterceptor(resolver));
        interceptors.add(new PermissionAnnotationMethodInterceptor(resolver));

        advice.setMethodInterceptors(interceptors);
        aasa.setAdvice(advice);

        return aasa;
    }

    /**
     * 配置 Shiro 的过滤器注册 Bean。
     * <p>
     * 使用 {@link DelegatingFilterProxy} 将 Shiro 过滤器集成到 Spring Boot 的过滤器链中。
     * </p>
     *
     * @return
     */
    @Bean
    public FilterRegistrationBean<DelegatingFilterProxy> filterRegistrationBean() {
        // TODO 不知道会不会有问题，先验证一下
        DelegatingFilterProxy proxy = new DelegatingFilterProxy();
        proxy.setTargetBeanName("shiroFilterFactoryBean");
        proxy.setTargetFilterLifecycle(true);

        FilterRegistrationBean<DelegatingFilterProxy> registration = new FilterRegistrationBean<>();
        registration.setFilter(proxy);
        registration.setAsyncSupported(true);
        registration.setDispatcherTypes(ASYNC);
        return registration;
    }

}
