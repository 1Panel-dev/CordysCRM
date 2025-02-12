package io.cordys.crm.system.controller;

import io.cordys.common.constants.PermissionConstants;
import io.cordys.crm.system.dto.request.ModuleFormRequest;
import io.cordys.crm.system.dto.request.ModuleFormSaveRequest;
import io.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import io.cordys.crm.system.service.ModuleFormCacheService;
import io.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
		return moduleFormCacheService.save(request, SessionUtils.getUserId());
	}

	@PostMapping("/config")
	@Operation(summary = "获取表单配置")
	@RequiresPermissions(PermissionConstants.MODULE_SETTING_UPDATE)
	public ModuleFormConfigDTO getFieldList(@Validated @RequestBody ModuleFormRequest request) {
		return moduleFormCacheService.getConfig(request);
	}
}
