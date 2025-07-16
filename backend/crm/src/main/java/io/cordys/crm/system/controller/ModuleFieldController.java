package io.cordys.crm.system.controller;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.cordys.common.constants.InternalUser;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.dto.DeptDataPermissionDTO;
import io.cordys.common.dto.DeptUserTreeNode;
import io.cordys.common.pager.PageUtils;
import io.cordys.common.pager.Pager;
import io.cordys.common.service.DataScopeService;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.clue.dto.request.CluePageRequest;
import io.cordys.crm.clue.dto.response.ClueListResponse;
import io.cordys.crm.clue.service.ClueService;
import io.cordys.crm.customer.dto.request.CustomerContactPageRequest;
import io.cordys.crm.customer.dto.request.CustomerPageRequest;
import io.cordys.crm.customer.dto.response.CustomerContactListResponse;
import io.cordys.crm.customer.dto.response.CustomerListResponse;
import io.cordys.crm.customer.service.CustomerContactService;
import io.cordys.crm.customer.service.CustomerService;
import io.cordys.crm.opportunity.dto.request.OpportunityPageRequest;
import io.cordys.crm.opportunity.dto.response.OpportunityListResponse;
import io.cordys.crm.opportunity.service.OpportunityService;
import io.cordys.crm.system.dto.request.ProductPageRequest;
import io.cordys.crm.system.dto.response.product.ProductListResponse;
import io.cordys.crm.system.service.ModuleFieldService;
import io.cordys.crm.system.service.ModuleService;
import io.cordys.crm.system.service.ProductService;
import io.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author song-cc-rock
 */
@RestController
@RequestMapping("/field")
@Tag(name = "字段管理-数据")
public class ModuleFieldController {

	@Resource
	private ModuleService moduleService;
	@Resource
	private ModuleFieldService moduleFieldService;
	@Resource
	private CustomerService customerService;
	@Resource
	private CustomerContactService customerContactService;
	@Resource
	private ClueService clueService;
	@Resource
	private OpportunityService opportunityService;
	@Resource
	private ProductService productService;
	@Resource
	private DataScopeService dataScopeService;

	@GetMapping("/dept/tree")
	@Operation(summary = "获取部门树")
	public List<DeptUserTreeNode> getDeptTree() {
		return moduleFieldService.getDeptTree(OrganizationContext.getOrganizationId());
	}

	@GetMapping("/user/dept/tree")
	@Operation(summary = "获取部门用户树")
	public List<DeptUserTreeNode> getDeptUserTree() {
		return moduleService.getDeptUserTree(OrganizationContext.getOrganizationId());
	}

	@PostMapping("/source/clue")
	@Operation(summary = "分页获取线索")
	public Pager<List<ClueListResponse>> sourceCluePage(@Valid @RequestBody CluePageRequest request) {
		DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(), OrganizationContext.getOrganizationId(),
				"ALL", PermissionConstants.CLUE_MANAGEMENT_READ);
		return clueService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), deptDataPermission);
	}

	@PostMapping("/source/customer")
	@Operation(summary = "分页获取客户")
	public Pager<List<CustomerListResponse>> sourceCustomerPage(@Valid @RequestBody CustomerPageRequest request) {
		DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(), OrganizationContext.getOrganizationId(),
				"ALL",  PermissionConstants.CUSTOMER_MANAGEMENT_READ);
		return customerService.sourceList(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), deptDataPermission);
	}

	@PostMapping("/source/contact")
	@Operation(summary = "分页获取联系人")
	public Pager<List<CustomerContactListResponse>> sourceContactPage(@Valid @RequestBody CustomerContactPageRequest request) {
		DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(), OrganizationContext.getOrganizationId(),
				"ALL", PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ);
		return customerContactService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), deptDataPermission);
	}

	@PostMapping("/source/opportunity")
	@Operation(summary = "分页获取商机")
	public Pager<List<OpportunityListResponse>> sourceOpportunityPage(@Valid @RequestBody OpportunityPageRequest request) {
		DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), "ALL",
				PermissionConstants.OPPORTUNITY_MANAGEMENT_READ);
		return opportunityService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), deptDataPermission);
	}

	@PostMapping("/source/product")
	@Operation(summary = "分页获取产品")
	public Pager<List<ProductListResponse>> sourceProductPage(@Valid @RequestBody ProductPageRequest request) {
		// 数据源接口只展示上架数据
		request.setStatus("1");
		return productService.list(request, OrganizationContext.getOrganizationId());
	}
}
