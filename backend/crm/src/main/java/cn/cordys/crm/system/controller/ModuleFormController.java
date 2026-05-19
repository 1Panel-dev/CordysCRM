package cn.cordys.crm.system.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.system.dto.request.ModuleFormSaveRequest;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author song-cc-rock
 */
@RestController
@RequestMapping("/module/form")
@Tag(name = "模块-表单设置")
public class ModuleFormController {

    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private ModuleFormService moduleFormService;

    @PostMapping("/save")
    @Operation(summary = "保存")
    @RequiresPermissions(PermissionConstants.MODULE_SETTING_UPDATE)
    public ModuleFormConfigDTO save(@Validated @RequestBody ModuleFormSaveRequest request) {
        return moduleFormCacheService.save(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/config/{formKey}")
    @Operation(summary = "获取表单配置")
    @RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE, PermissionConstants.PROCESS_SETTING_READ}, logical = Logical.OR)
    public ModuleFormConfigDTO getFieldList(@PathVariable String formKey) {
        return moduleFormService.getBusinessFormConfig(formKey, OrganizationContext.getOrganizationId());
    }
}
