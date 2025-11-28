package cn.cordys.crm.system.controller;

import cn.cordys.common.constants.InternalUserView;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.BaseTreeNode;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.dto.DeptUserTreeNode;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.clue.dto.request.CluePageRequest;
import cn.cordys.crm.clue.dto.response.ClueListResponse;
import cn.cordys.crm.clue.service.ClueService;
import cn.cordys.crm.contract.dto.request.ContractPageRequest;
import cn.cordys.crm.contract.dto.response.ContractListResponse;
import cn.cordys.crm.contract.service.ContractService;
import cn.cordys.crm.customer.dto.request.CustomerContactPageRequest;
import cn.cordys.crm.customer.dto.request.CustomerPageRequest;
import cn.cordys.crm.customer.dto.response.CustomerContactListResponse;
import cn.cordys.crm.customer.dto.response.CustomerListResponse;
import cn.cordys.crm.customer.service.CustomerContactService;
import cn.cordys.crm.customer.service.CustomerService;
import cn.cordys.crm.opportunity.dto.request.OpportunityPageRequest;
import cn.cordys.crm.opportunity.dto.request.OpportunityQuotationPageRequest;
import cn.cordys.crm.opportunity.dto.response.OpportunityListResponse;
import cn.cordys.crm.opportunity.dto.response.OpportunityQuotationListResponse;
import cn.cordys.crm.opportunity.service.OpportunityQuotationService;
import cn.cordys.crm.opportunity.service.OpportunityService;
import cn.cordys.crm.product.dto.request.ProductPageRequest;
import cn.cordys.crm.product.dto.request.ProductPricePageRequest;
import cn.cordys.crm.product.dto.response.ProductListResponse;
import cn.cordys.crm.product.dto.response.ProductPriceResponse;
import cn.cordys.crm.product.service.ProductPriceService;
import cn.cordys.crm.product.service.ProductService;
import cn.cordys.crm.system.dto.request.FieldRepeatCheckRequest;
import cn.cordys.crm.system.dto.request.FieldResolveRequest;
import cn.cordys.crm.system.dto.response.FieldRepeatCheckResponse;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFieldService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.crm.system.service.ModuleService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
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
    private ModuleFormService moduleFormService;
    @Resource
    private CustomerService customerService;
    @Resource
    private CustomerContactService customerContactService;
    @Resource
    private ClueService clueService;
    @Resource
    private OpportunityService opportunityService;
    @Resource
    private OpportunityQuotationService opportunityQuotationService;
    @Resource
    private ContractService contractService;
    @Resource
    private ProductService productService;
    @Resource
    private ProductPriceService productPriceService;
    @Resource
    private DataScopeService dataScopeService;

    @GetMapping("/dept/tree")
    @Operation(summary = "获取部门树")
    public List<BaseTreeNode> getDeptTree() {
        return moduleFieldService.getDeptTree(OrganizationContext.getOrganizationId());
    }

    @GetMapping("/user/dept/tree")
    @Operation(summary = "获取部门用户树")
    public List<DeptUserTreeNode> getDeptUserTree() {
        return moduleService.getDeptUserTree(OrganizationContext.getOrganizationId());
    }

    @PostMapping("/source/lead")
    @Operation(summary = "分页获取线索")
    public Pager<List<ClueListResponse>> sourceCluePage(@Valid @RequestBody CluePageRequest request) {
        request.setCombineSearch(request.getCombineSearch().convert());
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(), OrganizationContext.getOrganizationId(),
                "ALL", PermissionConstants.CLUE_MANAGEMENT_READ);
        return clueService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), deptDataPermission);
    }

    @PostMapping("/source/account")
    @Operation(summary = "分页获取客户")
    public Pager<List<CustomerListResponse>> sourceCustomerPage(@Valid @RequestBody CustomerPageRequest request) {
        request.setCombineSearch(request.getCombineSearch().convert());
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(), OrganizationContext.getOrganizationId(),
                "ALL", PermissionConstants.CUSTOMER_MANAGEMENT_READ);
        return customerService.sourceList(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), deptDataPermission);
    }

    @PostMapping("/source/contact")
    @Operation(summary = "分页获取联系人")
    public Pager<List<CustomerContactListResponse>> sourceContactPage(@Valid @RequestBody CustomerContactPageRequest request) {
        request.setCombineSearch(request.getCombineSearch().convert());
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(), OrganizationContext.getOrganizationId(),
                "ALL", PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ);
        return customerContactService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), deptDataPermission);
    }

    @PostMapping("/source/opportunity")
    @Operation(summary = "分页获取商机")
    public Pager<List<OpportunityListResponse>> sourceOpportunityPage(@Valid @RequestBody OpportunityPageRequest request) {
        request.setCombineSearch(request.getCombineSearch().convert());
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), "ALL",
                PermissionConstants.OPPORTUNITY_MANAGEMENT_READ);
        return opportunityService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), deptDataPermission);
    }

    @PostMapping("/source/quotation")
    @Operation(summary = "分页获取报价单")
    public Pager<List<OpportunityQuotationListResponse>> sourceOpportunityQuotationPage(@Valid @RequestBody OpportunityQuotationPageRequest request) {
        request.setCombineSearch(request.getCombineSearch().convert());
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), "ALL",
                PermissionConstants.OPPORTUNITY_MANAGEMENT_READ);
        return opportunityQuotationService.list(request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId(), deptDataPermission);
    }

    @PostMapping("/source/contract")
    @Operation(summary = "分页获取合同")
    public Pager<List<ContractListResponse>> sourceContractPage(@Valid @RequestBody ContractPageRequest request) {
        request.setCombineSearch(request.getCombineSearch().convert());
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), InternalUserView.ALL.name(),
                PermissionConstants.CONTRACT_READ);
        return contractService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), deptDataPermission);
    }

    @PostMapping("/source/product")
    @Operation(summary = "分页获取产品")
    public Pager<List<ProductListResponse>> sourceProductPage(@Valid @RequestBody ProductPageRequest request) {
        request.setCombineSearch(request.getCombineSearch().convert());
        // 数据源接口只展示上架数据
        request.setStatus("1");
        return productService.list(request, OrganizationContext.getOrganizationId());
    }

    @PostMapping("/source/price")
    @Operation(summary = "分页获取产品价格表")
    public Pager<List<ProductPriceResponse>> sourceProductPage(@Valid @RequestBody ProductPricePageRequest request) {
        request.setCombineSearch(request.getCombineSearch().convert());
        return productPriceService.list(request, OrganizationContext.getOrganizationId());
    }

    @PostMapping("/check/repeat")
    @Operation(summary = "校验重复值")
    public FieldRepeatCheckResponse checkRepeat(@Valid @RequestBody FieldRepeatCheckRequest checkRequest) {
        return moduleFieldService.checkRepeat(checkRequest, OrganizationContext.getOrganizationId());
    }

    @PostMapping("/resolve/business")
    @Operation(summary = "解析业务ID")
    public List<String> resolveBusinessId(@Valid @RequestBody FieldResolveRequest request) {
        return moduleFormService.resolveSourceNames(request.getSourceType(), request.getNames());
    }

    @GetMapping("/display/{formKey}")
    @Operation(summary = "获取表单配置")
    public ModuleFormConfigDTO getFieldList(@PathVariable String formKey) {
        return moduleFormService.getSourceDisplayFields(formKey, OrganizationContext.getOrganizationId());
    }
}
