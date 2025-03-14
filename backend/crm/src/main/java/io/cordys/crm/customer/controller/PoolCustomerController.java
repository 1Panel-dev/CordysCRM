package io.cordys.crm.customer.controller;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.dto.OptionDTO;
import io.cordys.common.pager.PageUtils;
import io.cordys.common.pager.Pager;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.customer.dto.request.*;
import io.cordys.crm.customer.dto.response.CustomerListResponse;
import io.cordys.crm.customer.service.CustomerService;
import io.cordys.crm.customer.service.PoolCustomerService;
import io.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "公海客户")
@RestController
@RequestMapping("/pool/customer")
public class PoolCustomerController {

	@Resource
	private PoolCustomerService poolCustomerService;
	@Resource
	private CustomerService customerService;

	@GetMapping("/options")
	@Operation(summary = "获取当前用户公海选项")
	@RequiresPermissions(value = {PermissionConstants.CUSTOMER_MANAGEMENT_READ})
	public List<OptionDTO> getPoolOptions() {
		return poolCustomerService.getPoolOptions(SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
	}

	@PostMapping("/page")
	@Operation(summary = "客户列表")
	@RequiresPermissions(value = {PermissionConstants.CUSTOMER_MANAGEMENT_READ})
	public Pager<List<CustomerListResponse>> list(@Validated @RequestBody CustomerPageRequest request) {
		return customerService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), null);
	}

	@PostMapping("/pick")
	@Operation(summary = "领取客户")
	@RequiresPermissions(value = {PermissionConstants.CUSTOMER_MANAGEMENT_PICK})
	public void pick(@Validated @RequestBody PoolCustomerPickRequest request) {
		poolCustomerService.pick(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
	}

	@PostMapping("/assign")
	@Operation(summary = "分配客户")
	@RequiresPermissions(value = {PermissionConstants.CUSTOMER_MANAGEMENT_ASSIGN})
	public void assign(@Validated @RequestBody PoolCustomerAssignRequest request) {
		poolCustomerService.assign(request.getCustomerId(), request.getAssignUserId(), OrganizationContext.getOrganizationId());
	}

	@GetMapping("/delete/{id}")
	@Operation(summary = "删除客户")
	@RequiresPermissions(value = {PermissionConstants.CUSTOMER_MANAGEMENT_DELETE})
	public void delete(@PathVariable String id) {
		poolCustomerService.delete(id);
	}

	@PostMapping("/batch-pick")
	@Operation(summary = "批量领取客户")
	@RequiresPermissions(value = {PermissionConstants.CUSTOMER_MANAGEMENT_PICK})
	public void batchPick(@Validated @RequestBody PoolCustomerBatchRequest request) {
		poolCustomerService.batchPick(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
	}

	@PostMapping("/batch-assign")
	@Operation(summary = "批量分配客户")
	@RequiresPermissions(value = {PermissionConstants.CUSTOMER_MANAGEMENT_ASSIGN})
	public void batchAssign(@Validated @RequestBody PoolCustomerBatchAssignRequest request) {
		poolCustomerService.batchAssign(request, request.getAssignUserId(), OrganizationContext.getOrganizationId());
	}

	@PostMapping("/batch-delete")
	@Operation(summary = "批量删除客户")
	@RequiresPermissions(value = {PermissionConstants.CUSTOMER_MANAGEMENT_DELETE})
	public void batchDelete(@Validated @RequestBody PoolCustomerBatchRequest request) {
		poolCustomerService.batchDelete(request.getBatchIds());
	}
}