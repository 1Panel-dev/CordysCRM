package io.cordys.crm.lead.controller;

import io.cordys.common.constants.PermissionConstants;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.lead.dto.LeadCapacityDTO;
import io.cordys.crm.lead.service.LeadCapacityService;
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
@RequestMapping("/lead-capacity")
@Tag(name = "线索库容设置")
public class LeadCapacityController {

	@Resource
	private LeadCapacityService leadCapacityService;

	@GetMapping("/get")
	@Operation(summary = "获取线索库容设置")
	@RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
	public List<LeadCapacityDTO> list() {
		return leadCapacityService.list(OrganizationContext.getOrganizationId());
	}

	@PostMapping("/save")
	@Operation(summary = "保存线索库容设置")
	@RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
	public void save(@Validated @RequestBody List<CapacityRequest> capacities) {
		leadCapacityService.save(capacities, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
	}
}
