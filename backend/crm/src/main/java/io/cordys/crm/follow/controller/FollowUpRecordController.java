package io.cordys.crm.follow.controller;

import io.cordys.common.constants.FormKey;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import io.cordys.crm.system.service.ModuleFormCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "跟进记录统一页面")
@RestController
@RequestMapping("/follow/record")
public class FollowUpRecordController {

    @Resource
    private ModuleFormCacheService moduleFormCacheService;


    @GetMapping("/module/form")
    @RequiresPermissions(value = {PermissionConstants.CUSTOMER_MANAGEMENT_READ, PermissionConstants.OPPORTUNITY_MANAGEMENT_READ, PermissionConstants.CLUE_MANAGEMENT_READ}, logical = Logical.OR)
    @Operation(summary = "获取表单配置")
    public ModuleFormConfigDTO getModuleFormConfig() {
        return moduleFormCacheService.getBusinessFormConfig(FormKey.FOLLOW_RECORD.getKey(), OrganizationContext.getOrganizationId());
    }

}
