package io.cordys.config;

import io.cordys.security.SessionConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.HeaderHttpSessionIdResolver;

/**
 * 配置类，用于管理与会话相关的配置和清理操作。
 * <p>
 * 本类主要用于配置会话的 ID 解析器，并通过定时任务清理没有绑定用户的会话。
 * </p>
 *
 * @version 1.0
 */
@Configuration
public class SessionConfig {
    /**
     * 创建 {@link HeaderHttpSessionIdResolver} Bean。
     * <p>
     * 该方法配置了会话 ID 的解析方式，使用请求头中的 {@link SessionConstants#HEADER_TOKEN} 字段作为会话 ID。
     * </p>
     *
     * @return 配置好的 {@link HeaderHttpSessionIdResolver} 实例
     */
    @Bean
    public HeaderHttpSessionIdResolver sessionIdResolver() {
        return new HeaderHttpSessionIdResolver(SessionConstants.HEADER_TOKEN);
    }
}
