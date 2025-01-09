package io.cordys.crm.lead.controller;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.pager.PageUtils;
import io.cordys.common.pager.Pager;
import io.cordys.crm.lead.domain.LeadCapacity;
import io.cordys.crm.lead.dto.request.LeadCapacityPageRequest;
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
	public Pager<List<LeadCapacity>> page(@Validated @RequestBody LeadCapacityPageRequest request) {
		Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize(),
				StringUtils.isNotBlank(request.getSortString()) ? request.getSortString() : "create_time desc");
		return PageUtils.setPageInfo(page, leadCapacityService.page(request));
	}

	@PostMapping("/add")
	@Operation(summary = "新增线索库容规则")
	@RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
	public void add(@Validated @RequestBody LeadCapacitySaveRequest request) {
		leadCapacityService.save(request, SessionUtils.getUserId());
	}

	@PostMapping("/update")
	@Operation(summary = "编辑线索库容规则")
	@RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
	public void update(@Validated @RequestBody LeadCapacitySaveRequest request) {
		leadCapacityService.save(request, SessionUtils.getUserId());
	}

	@GetMapping("/delete/{id}")
	@Operation(summary = "删除线索库容规则")
	@RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
	public void delete(@PathVariable String id) {
		leadCapacityService.delete(id);
	}
}
