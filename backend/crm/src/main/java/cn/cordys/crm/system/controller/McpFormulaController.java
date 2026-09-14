package cn.cordys.crm.system.controller;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.formula.FormulaRequestCompletionService;
import cn.cordys.common.permission.PermissionUtils;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.form.service.CustomFormService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** MCP 内部预计算入口，不发布为大模型工具，也不改变原有 CRM 写接口。 */
@Hidden
@RestController
@RequiresAuthentication
@RequestMapping("/mcp/formula")
public class McpFormulaController {
    private final FormulaRequestCompletionService completion;
    private final CustomFormService customForms;

    public McpFormulaController(FormulaRequestCompletionService completion, CustomFormService customForms) {
        this.completion = completion;
        this.customForms = customForms;
    }

    @PostMapping("/prepare")
    public Map<String, Object> prepare(@Valid @RequestBody PrepareRequest request) {
        // 与表单读取入口相同权限；不读取目标记录，写入权限仍由原 API 校验。
        FormKey key = FormKey.ofKey(request.formKey());
        if (key == null) {
            if (!PermissionUtils.hasPermission(PermissionConstants.CUSTOM_FORM_READ)) {
                throw new GenericException(CrmHttpResultCode.FORBIDDEN);
            }
            customForms.get(request.formKey(), SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
        } else {
            List<String> permissions = switch (key) {
                case CLUE -> List.of(PermissionConstants.CLUE_MANAGEMENT_READ, PermissionConstants.CLUE_MANAGEMENT_POOL_READ);
                case CUSTOMER -> List.of(PermissionConstants.CUSTOMER_MANAGEMENT_READ, PermissionConstants.CUSTOMER_MANAGEMENT_POOL_READ);
                case CONTACT -> List.of(PermissionConstants.CUSTOMER_MANAGEMENT_READ, PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ, PermissionConstants.OPPORTUNITY_MANAGEMENT_READ);
                case OPPORTUNITY -> List.of(PermissionConstants.OPPORTUNITY_MANAGEMENT_READ);
                case QUOTATION -> List.of(PermissionConstants.OPPORTUNITY_QUOTATION_READ);
                case CONTRACT -> List.of(PermissionConstants.CONTRACT_READ);
                case INVOICE -> List.of(PermissionConstants.CONTRACT_INVOICE_READ);
                case CONTRACT_PAYMENT_PLAN -> List.of(PermissionConstants.CONTRACT_PAYMENT_PLAN_READ);
                case CONTRACT_PAYMENT_RECORD -> List.of(PermissionConstants.CONTRACT_PAYMENT_RECORD_READ);
                case ORDER -> List.of(PermissionConstants.ORDER_READ);
                case PRODUCT, PRICE, FOLLOW_RECORD, FOLLOW_PLAN -> List.of();
            };
            if (!permissions.isEmpty() && permissions.stream().noneMatch(PermissionUtils::hasPermission)) {
                throw new GenericException(CrmHttpResultCode.FORBIDDEN);
            }
        }
        return completion.prepare(request.formKey(), request.values());
    }

    public record PrepareRequest(@NotBlank String formKey, @NotNull Map<String, Object> values) { }
}
