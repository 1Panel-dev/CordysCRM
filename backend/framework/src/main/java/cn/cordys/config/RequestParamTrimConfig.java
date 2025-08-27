package cn.cordys.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

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
        return jacksonObjectMapperBuilder -> {
            // 为 String 类型字段定义自定义的反序列化操作
            jacksonObjectMapperBuilder
                    .deserializerByType(String.class, new StdScalarDeserializer<String>(String.class) {
                        @Override
                        public String deserialize(JsonParser jsonParser, DeserializationContext ctx)
                                throws IOException {
                            // 在反序列化时去除前后空格
                            return StringUtils.trim(jsonParser.getValueAsString());
                        }
                    });
        };
    }
}
