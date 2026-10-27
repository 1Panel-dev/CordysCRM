package cn.cordys.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.HttpSessionIdResolver;

@Configuration
public class SessionConfig {

    /**
     * 普通 API 使用请求头恢复会话，SSO 回调和文件预览分别支持各自的专用 Cookie。
     *
     * @return 会话 ID 解析器
     */
    @Bean
    public HttpSessionIdResolver sessionIdResolver() {
        return new OAuthAwareSessionIdResolver();
    }
}
