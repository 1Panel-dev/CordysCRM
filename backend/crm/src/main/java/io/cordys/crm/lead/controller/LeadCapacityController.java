package io.cordys.crm.lead.controller;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.pager.PageUtils;
import io.cordys.common.pager.Pager;
import io.cordys.common.dto.BasePageRequest;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.lead.dto.LeadCapacityDTO;
import io.cordys.crm.lead.dto.request.LeadCapacitySaveRequest;
import io.cordys.crm.lead.service.LeadCapacityService;
import io.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/lead-capacity")
@Tag(name = "线索库容规则")
public class LeadCapacityController {

	@Resource
	private LeadCapacityService leadCapacityService;

	@PostMapping("/page")
	@Operation(summary = "分页获取线索库容规则")
	@RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
	public Pager<List<LeadCapacityDTO>> page(@Validated @RequestBody BasePageRequest request) {
		Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize(),
				StringUtils.isNotBlank(request.getSortString()) ? request.getSortString() : "create_time desc");
		return PageUtils.setPageInfo(page, leadCapacityService.page(OrganizationContext.getOrganizationId()));
	}

	@PostMapping("/save")
	@Operation(summary = "保存线索库容规则")
	@RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
	public void save(@Validated @RequestBody LeadCapacitySaveRequest request) {
		leadCapacityService.save(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
	}
}
