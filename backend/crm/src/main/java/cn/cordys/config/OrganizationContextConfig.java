package cn.cordys.config;

import cn.cordys.common.context.OrganizationContextWebFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: jianxing
 * @CreateTime: 2025-01-08  17:40
 */
@Configuration
public class OrganizationContextConfig {

    /**
     * 注册 OrganizationContextWebFilter 过滤器
     * @return
     */
    @Bean
    public FilterRegistrationBean<OrganizationContextWebFilter> tenantContextWebFilter() {
        FilterRegistrationBean<OrganizationContextWebFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new OrganizationContextWebFilter());
        return registrationBean;
    }
}
