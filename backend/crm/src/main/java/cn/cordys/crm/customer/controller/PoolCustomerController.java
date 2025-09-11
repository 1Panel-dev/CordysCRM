package cn.cordys.crm.customer.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.ExportSelectRequest;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.customer.dto.CustomerPoolDTO;
import cn.cordys.crm.customer.dto.request.CustomerExportRequest;
import cn.cordys.crm.customer.dto.request.CustomerPageRequest;
import cn.cordys.crm.customer.dto.request.PoolCustomerAssignRequest;
import cn.cordys.crm.customer.dto.request.PoolCustomerPickRequest;
import cn.cordys.crm.customer.dto.response.CustomerGetResponse;
import cn.cordys.crm.customer.dto.response.CustomerListResponse;
import cn.cordys.crm.customer.service.CustomerPoolExportService;
import cn.cordys.crm.customer.service.CustomerService;
import cn.cordys.crm.customer.service.PoolCustomerService;
import cn.cordys.crm.system.dto.request.PoolBatchAssignRequest;
import cn.cordys.crm.system.dto.request.PoolBatchPickRequest;
import cn.cordys.crm.system.dto.request.PoolBatchRequest;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "公海客户")
@RestController
@RequestMapping("/pool/account")
public class PoolCustomerController {

	@Resource
	private PoolCustomerService poolCustomerService;
	@Resource
	private CustomerService customerService;
	@Resource
	private CustomerPoolExportService customerPoolExportService;

	@GetMapping("/options")
	@Operation(summary = "获取当前用户公海选项")
	@RequiresPermissions(value = {PermissionConstants.CUSTOMER_MANAGEMENT_POOL_READ})
	public List<CustomerPoolDTO> getPoolOptions() {
		return poolCustomerService.getPoolOptions(SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
	}

	@PostMapping("/page")
	@Operation(summary = "客户列表")
	@RequiresPermissions(value = {PermissionConstants.CUSTOMER_MANAGEMENT_POOL_READ})
	public PagerWithOption<List<CustomerListResponse>> list(@Validated @RequestBody CustomerPageRequest request) {
		ConditionFilterUtils.parseCondition(request);
		return customerService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), null);
	}

	@PostMapping("/pick")
	@Operation(summary = "领取客户")
	@RequiresPermissions(value = {PermissionConstants.CUSTOMER_MANAGEMENT_POOL_PICK})
	public void pick(@Validated @RequestBody PoolCustomerPickRequest request) {
		poolCustomerService.pick(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
	}

	@PostMapping("/assign")
	@Operation(summary = "分配客户")
	@RequiresPermissions(value = {PermissionConstants.CUSTOMER_MANAGEMENT_POOL_ASSIGN})
	public void assign(@Validated @RequestBody PoolCustomerAssignRequest request) {
		poolCustomerService.assign(request.getCustomerId(), request.getAssignUserId(), OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
	}

	@GetMapping("/delete/{id}")
	@Operation(summary = "删除客户")
	@RequiresPermissions(value = {PermissionConstants.CUSTOMER_MANAGEMENT_POOL_DELETE})
	public void delete(@PathVariable String id) {
		poolCustomerService.delete(id);
	}

	@GetMapping("/get/{id}")
	@RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_POOL_READ)
	@Operation(summary = "客户详情")
	public CustomerGetResponse get(@PathVariable String id) {
		return customerService.get(id, OrganizationContext.getOrganizationId());
	}

	@PostMapping("/batch-pick")
	@Operation(summary = "批量领取客户")
	@RequiresPermissions(value = {PermissionConstants.CUSTOMER_MANAGEMENT_POOL_PICK})
	public void batchPick(@Validated @RequestBody PoolBatchPickRequest request) {
		poolCustomerService.batchPick(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
	}

	@PostMapping("/batch-assign")
	@Operation(summary = "批量分配客户")
	@RequiresPermissions(value = {PermissionConstants.CUSTOMER_MANAGEMENT_POOL_ASSIGN})
	public void batchAssign(@Validated @RequestBody PoolBatchAssignRequest request) {
		poolCustomerService.batchAssign(request, request.getAssignUserId(), OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
	}

	@PostMapping("/batch-delete")
	@Operation(summary = "批量删除客户")
	@RequiresPermissions(value = {PermissionConstants.CUSTOMER_MANAGEMENT_POOL_DELETE})
	public void batchDelete(@Validated @RequestBody PoolBatchRequest request) {
		poolCustomerService.batchDelete(request.getBatchIds(), SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
	}


	@PostMapping("/export-all")
	@Operation(summary = "客户导出全部")
	@RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_POOL_EXPORT)
	public String customerPoolExportAll(@Validated @RequestBody CustomerExportRequest request) {
		ConditionFilterUtils.parseCondition(request);
        return customerPoolExportService.exportCrossPage(SessionUtils.getUserId(), request, OrganizationContext.getOrganizationId(), null, LocaleContextHolder.getLocale());
	}

	@PostMapping("/export-select")
	@Operation(summary = "导出选中客户")
	@RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_POOL_EXPORT)
	public String customerPoolExportSelect(@Validated @RequestBody ExportSelectRequest request) {
		return customerPoolExportService.exportCrossSelect(SessionUtils.getUserId(), request, OrganizationContext.getOrganizationId(), LocaleContextHolder.getLocale());
	}
}