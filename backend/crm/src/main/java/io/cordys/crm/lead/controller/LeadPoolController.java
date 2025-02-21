package io.cordys.crm.lead.controller;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.dto.BasePageRequest;
import io.cordys.common.pager.PageUtils;
import io.cordys.common.pager.Pager;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.lead.dto.LeadPoolDTO;
import io.cordys.crm.lead.dto.request.LeadPoolAddRequest;
import io.cordys.crm.lead.dto.request.LeadPoolUpdateRequest;
import io.cordys.crm.lead.service.LeadPoolService;
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
@RequestMapping("/lead-pool")
@Tag(name = "线索池设置")
public class LeadPoolController {

	@Resource
	private LeadPoolService leadPoolService;

	@PostMapping("/page")
	@Operation(summary = "分页获取线索池")
	@RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
	public Pager<List<LeadPoolDTO>> page(@Validated @RequestBody BasePageRequest request) {
		Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize(),
				StringUtils.isNotBlank(request.getSortString()) ? request.getSortString() : "create_time desc");
		return PageUtils.setPageInfo(page, leadPoolService.page(request));
	}

	@PostMapping("/add")
	@Operation(summary = "新增线索池")
	@RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
	public void add(@Validated @RequestBody LeadPoolAddRequest request) {
		leadPoolService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
	}

	@PostMapping("/update")
	@Operation(summary = "编辑线索池")
	@RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
	public void update(@Validated @RequestBody LeadPoolUpdateRequest request) {
		leadPoolService.update(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
	}

	@GetMapping("/check-pick/{id}")
	@Operation(summary = "线索池是否存在未领取线索")
	@RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
	public void checkPick(@PathVariable String id) {
		leadPoolService.checkPick(id);
	}

	@GetMapping("/delete/{id}")
	@Operation(summary = "删除线索池")
	@RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
	public void delete(@PathVariable String id) {
		leadPoolService.delete(id, SessionUtils.getUserId());
	}

	@GetMapping("/switch/{id}")
	@Operation(summary = "启用/禁用线索池")
	@RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
	public void switchStatus(@PathVariable String id) {
		leadPoolService.switchStatus(id, SessionUtils.getUserId());
	}
}
