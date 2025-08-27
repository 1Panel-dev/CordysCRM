package cn.cordys.common.response.handler;

import cn.cordys.common.util.JSON;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * <p>统一处理返回结果集的响应体增强类。</p>
 * <p>该类用于在返回响应之前统一包装返回的结果，使得所有响应都遵循统一格式。
 * 如果返回的是空值，自动包装为一个成功的响应。<p>
 *
 * <p>支持的消息转换器类型为：MappingJackson2HttpMessageConverter 和 StringHttpMessageConverter。</p>
 */
@RestControllerAdvice(value = {"cn.cordys"})
public class ResultResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    /**
     * 判断当前处理器是否支持该转换器。
     *
     * @param methodParameter 当前请求的方法参数
     * @param converterType   转换器类型
     * @return 如果支持则返回 true，否则返回 false
     */
    @Override
    public boolean supports(@NotNull MethodParameter methodParameter,
                            @NotNull Class<? extends HttpMessageConverter<?>> converterType) {
        return MappingJackson2HttpMessageConverter.class.isAssignableFrom(converterType) ||
                StringHttpMessageConverter.class.isAssignableFrom(converterType);
    }

    /**
     * 在响应体写出之前对响应结果进行处理。
     *
     * @param body               响应体内容
     * @param methodParameter    当前方法参数
     * @param mediaType          响应的媒体类型
     * @param converterType      当前使用的消息转换器类型
     * @param serverHttpRequest  当前的 HTTP 请求
     * @param serverHttpResponse 当前的 HTTP 响应
     * @return 处理后的响应体内容
     */
    @Override
    public Object beforeBodyWrite(Object body,
                                  @NotNull MethodParameter methodParameter,
                                  @NotNull MediaType mediaType,
                                  @NotNull Class<? extends HttpMessageConverter<?>> converterType,
                                  @NotNull ServerHttpRequest serverHttpRequest,
                                  @NotNull ServerHttpResponse serverHttpResponse) {
        // 处理空值响应，转换为 JSON 格式的成功响应
        if (body == null && StringHttpMessageConverter.class.isAssignableFrom(converterType)) {
            serverHttpResponse.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            return JSON.toJSONString(ResultHolder.success(body));
        }

        // 如果方法标注了 NoResultHolder 注解，则不做任何包装
        if (methodParameter.hasMethodAnnotation(NoResultHolder.class)) {
            return body;
        }

        // 如果响应体不是 ResultHolder 类型，则包装为 ResultHolder
        if (!(body instanceof ResultHolder)) {
            if (body instanceof String) {
                serverHttpResponse.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                return JSON.toJSONString(ResultHolder.success(body));
            }
            return ResultHolder.success(body);
        }

        // 如果响应体已经是 ResultHolder 类型，则直接返回
        return body;
    }
}
