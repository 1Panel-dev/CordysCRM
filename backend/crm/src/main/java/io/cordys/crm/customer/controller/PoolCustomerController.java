package io.cordys.crm.customer.controller;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.cordys.common.dto.DeptDataPermissionDTO;
import io.cordys.common.dto.OptionDTO;
import io.cordys.common.pager.PageUtils;
import io.cordys.common.pager.Pager;
import io.cordys.common.service.DataScopeService;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.customer.dto.request.CustomerPageRequest;
import io.cordys.crm.customer.dto.request.PoolCustomerAssignRequest;
import io.cordys.crm.customer.dto.response.CustomerListResponse;
import io.cordys.crm.customer.service.CustomerService;
import io.cordys.crm.customer.service.PoolCustomerService;
import io.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
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
	public List<OptionDTO> getPoolOptions() {
		return poolCustomerService.getPoolOptions(SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
	}

	@PostMapping("/page")
	@Operation(summary = "客户列表")
	public Pager<List<CustomerListResponse>> list(@Validated @RequestBody CustomerPageRequest request) {
		Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
		return PageUtils.setPageInfo(page, customerService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), null));
	}

	@GetMapping("/delete/{id}")
	@Operation(summary = "删除客户")
	public void delete(@PathVariable String id) {
		poolCustomerService.delete(id);
	}

	@PostMapping("/assign")
	@Operation(summary = "分配客户")
	public void assign(@Validated @RequestBody PoolCustomerAssignRequest request) {
		poolCustomerService.assign(request);
	}

	@GetMapping("/pick/{id}")
	@Operation(summary = "领取客户")
	public void pick(@PathVariable String id) {
		poolCustomerService.pick(id, SessionUtils.getUserId());
	}
}