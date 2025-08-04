package io.cordys.crm.system.controller;

import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.dto.DeptUserTreeNode;
import io.cordys.common.dto.OptionDTO;
import io.cordys.common.dto.RoleUserTreeNode;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.system.dto.ModuleDTO;
import io.cordys.crm.system.dto.request.ModuleRequest;
import io.cordys.crm.system.dto.request.ModuleSortRequest;
import io.cordys.crm.system.service.ModuleService;
import io.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/module")
@Tag(name = "模块设置")
public class ModuleController {

	@Resource
	private ModuleService moduleService;

	@PostMapping("/list")
	@Operation(summary = "获取模块设置列表")
	public List<ModuleDTO> getModuleList(@Validated @RequestBody ModuleRequest request) {
		return moduleService.getModuleList(request);
	}

	@GetMapping("/form/list")
	@Operation(summary = "获取表单列表")
	public List<OptionDTO> getFormList() {
		return moduleService.getFormList();
	}

	@GetMapping("/switch/{id}")
	@Operation(summary = "单个模块开启或关闭")
	@RequiresPermissions(PermissionConstants.MODULE_SETTING_UPDATE)
	public void switchModule(@PathVariable String id) {
		moduleService.switchModule(id, SessionUtils.getUserId());
	}

	@PostMapping("/sort")
	@Operation(summary = "模块排序")
	@RequiresPermissions(PermissionConstants.MODULE_SETTING_UPDATE)
	public void sortModule(@Validated @RequestBody ModuleSortRequest request) {
		moduleService.sort(request, SessionUtils.getUserId());
	}

	@GetMapping("/user/dept/tree")
	@Operation(summary = "获取部门用户树")
	@RequiresPermissions(PermissionConstants.MODULE_SETTING_READ)
	public List<DeptUserTreeNode> getDeptUserTree() {
		return moduleService.getDeptUserTree(OrganizationContext.getOrganizationId());
	}

	@GetMapping("/role/tree")
	@Operation(summary = "获取角色树")
	@RequiresPermissions(PermissionConstants.MODULE_SETTING_READ)
	public List<RoleUserTreeNode> getRoleTree() {
		return moduleService.getRoleTree(OrganizationContext.getOrganizationId());
	}
}
