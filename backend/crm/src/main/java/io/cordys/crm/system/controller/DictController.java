package io.cordys.crm.system.controller;

import io.cordys.common.constants.PermissionConstants;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.system.domain.Dict;
import io.cordys.crm.system.dto.request.DictAddRequest;
import io.cordys.crm.system.dto.request.DictUpdateRequest;
import io.cordys.crm.system.service.DictService;
import io.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dict")
@Tag(name = "模块设置-字典管理")
public class DictController {

	@Resource
	private DictService dictService;

	@GetMapping("/get/{type}")
	@Operation(summary = "获取字典列表")
	@RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_READ})
	public List<Dict> getDictListByType(
			@Schema(description = "类型", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"OPPORTUNITY_FAIL", "CUSTOMER_POOL_RS", "CLUE_POOL_RS"})
			@PathVariable("type") String type) {
		return dictService.getDictListByType(type, OrganizationContext.getOrganizationId());
	}

	@PostMapping("/add")
	@Operation(summary = "添加字典值")
	@RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
	public void addDict(@RequestBody DictAddRequest request) {
		dictService.addDict(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
	}

	@PostMapping("/update")
	@Operation(summary = "修改字典值")
	@RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
	public void updateDict(@RequestBody DictUpdateRequest request) {
		dictService.updateDict(request, SessionUtils.getUserId());
	}

	@GetMapping("/delete/{id}")
	@Operation(summary = "删除字典值")
	@RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
	public void deleteDict(@PathVariable("id") String id) {
		dictService.deleteDict(id);
	}
}
