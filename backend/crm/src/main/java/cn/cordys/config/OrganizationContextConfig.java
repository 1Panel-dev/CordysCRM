package cn.cordys.config;

import cn.cordys.common.context.OrganizationContextWebFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author jianxing
 */
@Configuration
public class OrganizationContextConfig {

    /**
     * 注册 OrganizationContextWebFilter 过滤器
     */
    @Bean
    public FilterRegistrationBean<OrganizationContextWebFilter> tenantContextWebFilter() {
        FilterRegistrationBean<OrganizationContextWebFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new OrganizationContextWebFilter());
        return registrationBean;
    }
}
