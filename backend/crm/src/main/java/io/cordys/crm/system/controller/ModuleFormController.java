package io.cordys.crm.system.controller;

import io.cordys.common.constants.PermissionConstants;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.system.dto.request.ModuleFormSaveRequest;
import io.cordys.crm.system.dto.response.BusinessModuleFormConfigDTO;
import io.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import io.cordys.crm.system.service.ModuleFormCacheService;
import io.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/module/form")
@Tag(name = "模块-表单设置")
public class ModuleFormController {

	@Resource
	private ModuleFormCacheService moduleFormCacheService;

	@PostMapping("/save")
	@Operation(summary = "保存")
	@RequiresPermissions(PermissionConstants.MODULE_SETTING_UPDATE)
	public ModuleFormConfigDTO save(@Validated @RequestBody ModuleFormSaveRequest request) {
		return moduleFormCacheService.save(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
	}

	@GetMapping("/config/{formKey}")
	@Operation(summary = "获取表单配置")
	@RequiresPermissions(PermissionConstants.MODULE_SETTING_UPDATE)
	public BusinessModuleFormConfigDTO getFieldList(@PathVariable String formKey) {
		return moduleFormCacheService.getBusinessFormConfig(formKey, OrganizationContext.getOrganizationId());
	}
}
