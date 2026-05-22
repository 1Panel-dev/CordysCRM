package cn.cordys.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.util.Arrays;

/**
 * 配置类，用于定制 Jackson 的反序列化过程。
 * <p>
 * 本类的作用是为 String 类型字段的反序列化过程添加自定义逻辑，去除请求参数中的前后空格。
 * </p>
 *
 * @version 1.0
 */
@Configuration
public class RequestParamTrimConfig {

    /**
     * 需要开启 XSS 过滤的 URL 列表（Ant 风格，逗号分隔）。
     * 示例：/account/follow/**,/announcement/**
     * 留空表示不对任何请求做 HTML 转义。
     */
    @Value("${xss.protection.url.list:}")
    private String xssFilterUrlList;

    /**
     * 定义一个 {@link Jackson2ObjectMapperBuilderCustomizer} Bean，
     * 用于定制 Jackson 的 ObjectMapper 设置，特别是针对 String 类型字段的反序列化。
     * <p>
     * 通过此定制，所有从 JSON 反序列化的 String 类型字段将在反序列化时自动去除前后空格。
     * </p>
     *
     * @return 定制后的 {@link Jackson2ObjectMapperBuilderCustomizer} 实例
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        AntPathMatcher pathMatcher = new AntPathMatcher(); // 线程安全，可复用

        return builder -> builder
                .deserializerByType(String.class, new StdScalarDeserializer<String>(String.class) {
                    @Override
                    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                        String rawValue = p.getValueAsString();
                        String trimmedValue = StringUtils.trim(rawValue);

                        // 判断是否需要对当前值进行 HTML 转义
                        if (shouldEscapeXss(pathMatcher)) {
                            return HtmlUtils.htmlEscape(trimmedValue);
                        }
                        return trimmedValue;
                    }
                });
    }

    /**
     * 判断当前请求是否命中 XSS 过滤白名单（命中 → 执行转义）。
     */
    private boolean shouldEscapeXss(AntPathMatcher pathMatcher) {
        // 未配置过滤列表
        if (StringUtils.isBlank(xssFilterUrlList)) {
            return false;
        }

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return false; // 非 Web 上下文（定时任务等）不处理
        }

        HttpServletRequest request = attributes.getRequest();
        String requestURI = request.getRequestURI();
        if (StringUtils.isBlank(requestURI)) {
            return false;
        }

        // 逗号分隔的 Ant 路径列表，任一匹配即需要转义
        return Arrays.stream(xssFilterUrlList.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)   // 过滤掉空白配置项
                .anyMatch(pattern -> pathMatcher.match(pattern, requestURI));
    }

}
