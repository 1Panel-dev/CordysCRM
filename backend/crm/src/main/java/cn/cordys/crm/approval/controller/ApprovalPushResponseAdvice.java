package cn.cordys.crm.approval.controller;

import cn.cordys.crm.approval.dto.response.ApprovalPushResponse;
import jakarta.annotation.Resource;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 在统一响应包装前，为编辑提审的业务结果补充重审节点失效标识。
 */
@RestControllerAdvice("cn.cordys.crm")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApprovalPushResponseAdvice implements ResponseBodyAdvice<Object> {

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return JacksonJsonHttpMessageConverter.class.isAssignableFrom(converterType);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType contentType,
            Class<? extends HttpMessageConverter<?>> converterType, ServerHttpRequest request,
            ServerHttpResponse response) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)
                || !(servletRequest.getServletRequest().getAttribute(ApprovalPushResponse.class.getName())
                        instanceof ApprovalPushResponse result)
                || !result.isResubmitNodeInvalid()) {
            return body;
        }
        Map<String, Object> data = body == null ? new LinkedHashMap<>()
                : objectMapper.convertValue(body, new TypeReference<>() {});
        data.put("resubmitNodeInvalid", true);
        return data;
    }
}
