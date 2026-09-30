package cn.cordys.crm.analytics.controller;

import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.analytics.dto.request.AnalyticsQueryRequest;
import cn.cordys.crm.analytics.dto.request.AnalyticsQueryRequest.View;
import cn.cordys.crm.analytics.dto.response.AnalyticsQueryResponse;
import cn.cordys.crm.analytics.dto.response.AnalyticsSchemaResponse;
import cn.cordys.crm.analytics.service.AnalyticsService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 登录会话与 API Key 都通过现有认证链路取得身份；各模块权限在 Service 中动态校验。 */
@RestController
@Validated
@RequiresAuthentication
@RequestMapping("/analytics")
@Tag(name = "通用统计")
public class AnalyticsController {
    @Resource private AnalyticsService analyticsService;

    @GetMapping("/schema")
    @Operation(operationId = "analyticsSchema", summary = "当前用户可用的统计字段与口径", extensions = @Extension(name = "x-mcp", properties = {
            @ExtensionProperty(name = "enabled", value = "true", parseValue = true),
            @ExtensionProperty(name = "readOnly", value = "true", parseValue = true)}))
    public AnalyticsSchemaResponse schema(@RequestParam @NotBlank @Size(max = 64) String formKey,
                                           @RequestParam(defaultValue = "ALL") View view,
                                           @RequestParam(required = false) @Size(max = 64) String poolId,
                                           @RequestParam(required = false) @Size(max = 64) String subTableFieldId,
                                           @RequestParam(defaultValue = "false") boolean includeRelated) {
        return analyticsService.schema(formKey, view, poolId, subTableFieldId, includeRelated,
                SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/query")
    @Operation(operationId = "analyticsQuery", summary = "按当前用户权限执行数据库聚合统计", extensions = @Extension(name = "x-mcp", properties = {
            @ExtensionProperty(name = "enabled", value = "true", parseValue = true),
            @ExtensionProperty(name = "readOnly", value = "true", parseValue = true)}))
    public AnalyticsQueryResponse query(@Valid @RequestBody AnalyticsQueryRequest request) {
        return analyticsService.query(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }
}
