package io.cordys.crm.clue.controller;

import io.cordys.common.constants.PermissionConstants;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.clue.dto.ClueCapacityDTO;
import io.cordys.crm.clue.service.ClueCapacityService;
import io.cordys.crm.system.dto.request.CapacityRequest;
import io.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clue-capacity")
@Tag(name = "线索库容设置")
public class ClueCapacityController {

	@Resource
	private ClueCapacityService clueCapacityService;

	@GetMapping("/get")
	@Operation(summary = "获取线索库容设置")
	@RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
	public List<ClueCapacityDTO> list() {
		return clueCapacityService.list(OrganizationContext.getOrganizationId());
	}

	@PostMapping("/save")
	@Operation(summary = "保存线索库容设置")
	@RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
	public void save(@Validated @RequestBody List<CapacityRequest> capacities) {
		clueCapacityService.save(capacities, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
	}
}
