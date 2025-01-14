package io.cordys.crm.customer.controller;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.pager.PageUtils;
import io.cordys.common.pager.Pager;
import io.cordys.crm.customer.dto.CustomerPoolDTO;
import io.cordys.crm.customer.dto.request.CustomerPoolPageRequest;
import io.cordys.crm.customer.dto.request.CustomerPoolSaveRequest;
import io.cordys.crm.customer.service.CustomerPoolService;
import io.cordys.crm.lead.dto.request.LeadPoolSaveRequest;
import io.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/customer-pool")
@Tag(name = "公海池")
public class CustomerPoolController {

	@Resource
	private CustomerPoolService customerPoolService;

	@PostMapping("/page")
	@Operation(summary = "分页获取公海池")
	@RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE}, logical = Logical.OR)
	public Pager<List<CustomerPoolDTO>> page(CustomerPoolPageRequest request) {
		Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize(),
				StringUtils.isNotBlank(request.getSortString()) ? request.getSortString() : "create_time desc");
		return PageUtils.setPageInfo(page, customerPoolService.page(request));
	}

	@PostMapping("/add")
	@Operation(summary = "保存公海池")
	@RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE}, logical = Logical.OR)
	public void save(@Validated @RequestBody CustomerPoolSaveRequest request) {
		customerPoolService.save(request, SessionUtils.getUserId());
	}

	@PostMapping("/update")
	@Operation(summary = "编辑线索池")
	@RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE}, logical = Logical.OR)
	public void update(@Validated @RequestBody CustomerPoolSaveRequest request) {
		customerPoolService.save(request, SessionUtils.getUserId());
	}

	@GetMapping("/delete/{id}")
	@Operation(summary = "删除线索池")
	@RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE}, logical = Logical.OR)
	public void delete(@PathVariable String id) {
		customerPoolService.delete(id, SessionUtils.getUserId());
	}

	@GetMapping("/switch/{id}")
	@Operation(summary = "启用/禁用线索池")
	@RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE}, logical = Logical.OR)
	public void switchStatus(@PathVariable String id) {
		customerPoolService.switchStatus(id, SessionUtils.getUserId());
	}
}
