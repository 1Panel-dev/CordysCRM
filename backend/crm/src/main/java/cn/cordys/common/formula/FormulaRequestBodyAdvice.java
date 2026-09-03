package cn.cordys.common.formula;

import cn.cordys.common.constants.FormKey;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 把前端“点击提交前计算公式”的时机映射到服务端请求生命周期：反序列化之后、参数校验之前。
 * MCP 更新会在调用 CRM 前回读并补全当前可写字段，因此这里不需要增加 AI 入参。
 */
@ControllerAdvice(basePackages = "cn.cordys.crm")
public class FormulaRequestBodyAdvice extends RequestBodyAdviceAdapter {

    private static final Map<String, FormulaRequest> PATH_REQUESTS = pathRequests();

    private final FormulaRequestCompletionService completionService;

    public FormulaRequestBodyAdvice(FormulaRequestCompletionService completionService) {
        this.completionService = completionService;
    }

    @Override
    public boolean supports(
            MethodParameter methodParameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType
    ) {
        return true;
    }

    @Override
    public Object afterBodyRead(
            Object body,
            HttpInputMessage inputMessage,
            MethodParameter parameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType
    ) {
        FormulaRequest request = resolveRequest(inputMessage);
        if (request != null) {
            completionService.complete(request.formKey(), body, request.createMode());
        }
        return body;
    }

    private FormulaRequest resolveRequest(HttpInputMessage inputMessage) {
        if (!(inputMessage instanceof ServletServerHttpRequest servletRequest)) {
            return null;
        }
        String path = servletRequest.getServletRequest().getRequestURI();
        return PATH_REQUESTS.entrySet().stream()
                .filter(entry -> path.endsWith(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private static Map<String, FormulaRequest> pathRequests() {
        Map<String, FormulaRequest> paths = new LinkedHashMap<>();
        // 长路径必须排在短路径前，避免 /opportunity/quotation/add 被商机路径误判。
        register(paths, "/opportunity/quotation", FormKey.QUOTATION.getKey());
        register(paths, "/contract/payment-record", FormKey.CONTRACT_PAYMENT_RECORD.getKey());
        register(paths, "/contract/payment-plan", FormKey.CONTRACT_PAYMENT_PLAN.getKey());
        register(paths, "/account/contact", FormKey.CONTACT.getKey());
        register(paths, "/follow/record", FormKey.FOLLOW_RECORD.getKey());
        register(paths, "/follow/plan", FormKey.FOLLOW_PLAN.getKey());
        register(paths, "/opportunity", FormKey.OPPORTUNITY.getKey());
        register(paths, "/contract", FormKey.CONTRACT.getKey());
        register(paths, "/invoice", FormKey.INVOICE.getKey());
        register(paths, "/account", FormKey.CUSTOMER.getKey());
        register(paths, "/product", FormKey.PRODUCT.getKey());
        register(paths, "/price", FormKey.PRICE.getKey());
        register(paths, "/order", FormKey.ORDER.getKey());
        register(paths, "/lead", FormKey.CLUE.getKey());
        return Collections.unmodifiableMap(paths);
    }

    private static void register(Map<String, FormulaRequest> paths, String basePath, String formKey) {
        paths.put(basePath + "/add", new FormulaRequest(formKey, true));
        paths.put(basePath + "/update", new FormulaRequest(formKey, false));
    }

    private record FormulaRequest(String formKey, boolean createMode) {
    }
}
