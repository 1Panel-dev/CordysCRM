package cn.cordys.crm.follow.controller;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.ResourceTabEnableDTO;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.follow.service.FollowUpRecordService;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.security.SessionUtils;
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
    @Resource
    private FollowUpRecordService followUpRecordService;


    @GetMapping("/module/form")
    @Operation(summary = "获取表单配置")
    public ModuleFormConfigDTO getModuleFormConfig() {
        return moduleFormCacheService.getBusinessFormConfig(FormKey.FOLLOW_RECORD.getKey(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/tab")
    @RequiresPermissions(value = {PermissionConstants.CLUE_MANAGEMENT_READ, PermissionConstants.CUSTOMER_MANAGEMENT_READ}, logical = Logical.OR)
    @Operation(summary = "数据权限TAB")
    public ResourceTabEnableDTO getTabEnableConfig() {
        return followUpRecordService.getTabEnableConfig(SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

}
