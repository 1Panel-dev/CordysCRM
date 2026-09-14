package cn.cordys.crm.system.controller;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.InternalUserView;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.BaseTreeNode;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.dto.DeptUserTreeNode;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.JSON;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.permission.PermissionUtils;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.clue.dto.request.CluePageRequest;
import cn.cordys.crm.clue.dto.response.ClueListResponse;
import cn.cordys.crm.clue.service.ClueService;
import cn.cordys.crm.contract.dto.request.*;
import cn.cordys.crm.contract.dto.response.*;
import cn.cordys.crm.contract.service.*;
import cn.cordys.crm.customer.dto.request.CustomerContactPageRequest;
import cn.cordys.crm.customer.dto.request.CustomerPageRequest;
import cn.cordys.crm.customer.dto.response.CustomerContactListResponse;
import cn.cordys.crm.customer.dto.response.CustomerListResponse;
import cn.cordys.crm.customer.service.CustomerContactService;
import cn.cordys.crm.customer.service.CustomerService;
import cn.cordys.crm.form.dto.request.CustomFormDataPageRequest;
import cn.cordys.crm.form.dto.response.CustomFormDataListResponse;
import cn.cordys.crm.form.service.CustomFormDataService;
import cn.cordys.crm.opportunity.dto.request.OpportunityPageRequest;
import cn.cordys.crm.opportunity.dto.request.OpportunityQuotationPageRequest;
import cn.cordys.crm.opportunity.dto.response.OpportunityListResponse;
import cn.cordys.crm.opportunity.dto.response.OpportunityQuotationListResponse;
import cn.cordys.crm.opportunity.service.OpportunityQuotationService;
import cn.cordys.crm.opportunity.service.OpportunityService;
import cn.cordys.crm.order.dto.request.OrderPageRequest;
import cn.cordys.crm.order.dto.response.OrderListResponse;
import cn.cordys.crm.order.service.OrderService;
import cn.cordys.crm.product.dto.request.ProductPageRequest;
import cn.cordys.crm.product.dto.request.ProductPricePageRequest;
import cn.cordys.crm.product.dto.response.ProductListResponse;
import cn.cordys.crm.product.dto.response.ProductPriceResponse;
import cn.cordys.crm.product.service.ProductPriceService;
import cn.cordys.crm.product.service.ProductService;
import cn.cordys.crm.system.dto.DatasourceRefDTO;
import cn.cordys.crm.system.dto.request.DatasourceRefQueryRequest;
import cn.cordys.crm.system.dto.request.FieldRepeatCheckRequest;
import cn.cordys.crm.system.dto.request.FieldResolveRequest;
import cn.cordys.crm.system.dto.response.FieldRepeatCheckResponse;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFieldService;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.crm.system.service.ModuleService;
import cn.cordys.security.SessionUtils;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

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
	private ModuleFormCacheService formCacheService;
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
    private ContractPaymentPlanService contractPaymentPlanService;
	@Resource
	private ContractPaymentRecordService contractPaymentRecordService;
    @Resource
    private DataScopeService dataScopeService;
    @Resource
    private BusinessTitleService businessTitleService;
	@Resource
	private OrderService orderService;
    @Resource
    private CustomFormDataService customFormDataService;
	@Resource
	private ContractInvoiceService contractInvoiceService;

    @GetMapping("/dept/tree")
    @Operation(summary = "获取部门树")
    public List<BaseTreeNode> getDeptTree() {
        return moduleFieldService.getDeptTree(OrganizationContext.getOrganizationId());
    }

    @GetMapping("/user/dept/tree")
    @Operation(summary = "获取部门用户树")
    public List<DeptUserTreeNode> getDeptUserTree(@RequestParam(required = false, defaultValue = "false") Boolean includeDisabled) {
        return moduleService.getDeptUserTree(OrganizationContext.getOrganizationId(), includeDisabled);
    }

    @PostMapping("/source/lead")
    @Operation(summary = "分页获取线索")
    public Pager<List<ClueListResponse>> sourceCluePage(@Valid @RequestBody CluePageRequest request) {
        ConditionFilterUtils.parseCondition(request, FormKey.CLUE.getKey());
        request.setCombineSearch(request.getCombineSearch().convert());
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(), OrganizationContext.getOrganizationId(),
				InternalUserView.ALL.name(), PermissionConstants.CLUE_MANAGEMENT_READ);
        return clueService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), deptDataPermission, true);
    }

    @PostMapping("/source/custom-form-data")
    @Operation(summary = "分页获取自定义表单数据")
    public Pager<List<CustomFormDataListResponse>> sourceCustomFormDataPage(@Valid @RequestBody CustomFormDataPageRequest request) {
        ConditionFilterUtils.parseCondition(request, request.getCustomFormId());
        request.setCombineSearch(request.getCombineSearch().convert());
        if (!PermissionUtils.hasPermission(PermissionConstants.CUSTOM_FORM_READ)) {
            Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
            return PageUtils.setPageInfoWithOption(page, List.of(), Map.of());
        }
        return customFormDataService.page(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), true);
    }

    @PostMapping("/source/account")
    @Operation(summary = "分页获取客户")
    public Pager<List<CustomerListResponse>> sourceCustomerPage(@Valid @RequestBody CustomerPageRequest request) {
        ConditionFilterUtils.parseCondition(request, FormKey.CUSTOMER.getKey());
        request.setCombineSearch(request.getCombineSearch().convert());
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(), OrganizationContext.getOrganizationId(),
				InternalUserView.ALL.name(), PermissionConstants.CUSTOMER_MANAGEMENT_READ);
        return customerService.sourceList(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), deptDataPermission);
    }

    @PostMapping("/source/contact")
    @Operation(summary = "分页获取联系人")
    public Pager<List<CustomerContactListResponse>> sourceContactPage(@Valid @RequestBody CustomerContactPageRequest request) {
        ConditionFilterUtils.parseCondition(request, FormKey.CONTACT.getKey());
        request.setCombineSearch(request.getCombineSearch().convert());
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(), OrganizationContext.getOrganizationId(),
				InternalUserView.ALL.name(), PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ);
        return customerContactService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), deptDataPermission);
    }

    @PostMapping("/source/opportunity")
    @Operation(summary = "分页获取商机")
    public Pager<List<OpportunityListResponse>> sourceOpportunityPage(@Valid @RequestBody OpportunityPageRequest request) {
        ConditionFilterUtils.parseCondition(request, FormKey.OPPORTUNITY.getKey());
        request.setCombineSearch(request.getCombineSearch().convert());
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), InternalUserView.ALL.name(),
                PermissionConstants.OPPORTUNITY_MANAGEMENT_READ);
        return opportunityService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), deptDataPermission, true);
    }

    @PostMapping("/source/quotation")
    @Operation(summary = "分页获取报价单")
    public Pager<List<OpportunityQuotationListResponse>> sourceOpportunityQuotationPage(@Valid @RequestBody OpportunityQuotationPageRequest request) {
        ConditionFilterUtils.parseCondition(request, FormKey.QUOTATION.getKey());
        request.setCombineSearch(request.getCombineSearch().convert());
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), InternalUserView.ALL.name(),
                PermissionConstants.OPPORTUNITY_MANAGEMENT_READ);
        return opportunityQuotationService.list(request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId(), deptDataPermission, true);
    }

    @PostMapping("/source/contract")
    @Operation(summary = "分页获取合同")
    public Pager<List<ContractListResponse>> sourceContractPage(@Valid @RequestBody ContractPageRequest request) {
        ConditionFilterUtils.parseCondition(request, FormKey.CONTRACT.getKey());
        request.setCombineSearch(request.getCombineSearch().convert());
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), InternalUserView.ALL.name(),
                PermissionConstants.CONTRACT_READ);
        return contractService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), deptDataPermission, true);
    }

    @PostMapping("/source/product")
    @Operation(summary = "分页获取产品")
    public Pager<List<ProductListResponse>> sourceProductPage(@Valid @RequestBody ProductPageRequest request) {
        ConditionFilterUtils.parseCondition(request, FormKey.PRODUCT.getKey());
        request.setCombineSearch(request.getCombineSearch().convert());
        // 数据源接口只展示上架数据
        request.setStatus("1");
        return productService.list(request, OrganizationContext.getOrganizationId());
    }

    @PostMapping("/source/price")
    @Operation(summary = "分页获取产品价格表")
    public Pager<List<ProductPriceResponse>> sourceProductPage(@Valid @RequestBody ProductPricePageRequest request) {
        ConditionFilterUtils.parseCondition(request, FormKey.PRICE.getKey());
        request.setCombineSearch(request.getCombineSearch().convert());
        return productPriceService.list(request, OrganizationContext.getOrganizationId());
    }

    @PostMapping("/source/contract/payment-plan")
    @Operation(summary = "分页获取合同回款计划")
    public Pager<List<ContractPaymentPlanListResponse>> sourcePlanPage(@Valid @RequestBody ContractPaymentPlanPageRequest request) {
        ConditionFilterUtils.parseCondition(request, FormKey.CONTRACT_PAYMENT_PLAN.getKey());
        request.setCombineSearch(request.getCombineSearch().convert());
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), InternalUserView.ALL.name(),
                PermissionConstants.CONTRACT_PAYMENT_PLAN_READ);
        return contractPaymentPlanService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), deptDataPermission);
    }

	@PostMapping("/source/contract/payment-record")
	@Operation(summary = "分页获取合同回款记录")
	public Pager<List<ContractPaymentRecordResponse>> sourceRecordPage(@Valid @RequestBody ContractPaymentRecordPageRequest request) {
        ConditionFilterUtils.parseCondition(request, FormKey.CONTRACT_PAYMENT_RECORD.getKey());
        request.setCombineSearch(request.getCombineSearch().convert());
		DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), InternalUserView.ALL.name(),
				PermissionConstants.CONTRACT_PAYMENT_RECORD_READ);
		return contractPaymentRecordService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), deptDataPermission);
	}

	@PostMapping("/source/order")
	@Operation(summary = "分页获取订单列表")
	public PagerWithOption<List<OrderListResponse>> list(@Validated @RequestBody OrderPageRequest request) {
        ConditionFilterUtils.parseCondition(request, FormKey.ORDER.getKey());
        request.setCombineSearch(request.getCombineSearch().convert());
		DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(), OrganizationContext.getOrganizationId(),
				InternalUserView.ALL.name(), PermissionConstants.ORDER_READ);
		return orderService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), deptDataPermission, false);
	}

	@PostMapping("/source/business-title")
	@Operation(summary = "分页获取工商抬头信息")
	public Pager<List<BusinessTitleListResponse>> sourceBusinessTitlePage(@Valid @RequestBody BusinessTitlePageRequest request) {
		return businessTitleService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
	}

	@PostMapping("/source/invoice")
	@Operation(summary = "分页获取发票信息")
	public PagerWithOption<List<ContractInvoiceListResponse>> list(@Validated @RequestBody ContractInvoicePageRequest request) {
		ConditionFilterUtils.parseCondition(request, FormKey.INVOICE.getKey());
		DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(),
				OrganizationContext.getOrganizationId(), request.getViewId(), PermissionConstants.CONTRACT_INVOICE_READ);
		return contractInvoiceService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), deptDataPermission);
	}

    @PostMapping("/check/repeat")
    @Operation(summary = "校验重复值")
    public FieldRepeatCheckResponse checkRepeat(@Valid @RequestBody FieldRepeatCheckRequest checkRequest) {
        return moduleFieldService.checkRepeat(checkRequest, OrganizationContext.getOrganizationId());
    }

    @PostMapping("/resolve/business")
    @Operation(summary = "解析业务ID")
    public List<OptionDTO> resolveBusinessId(@Valid @RequestBody FieldResolveRequest request) {
        String source = request.getSourceType();
        List<String> keywords = request.getKeywords();
        if (source == null || !source.matches("[A-Za-z0-9_-]{1,128}") || keywords == null
                || keywords.isEmpty() || keywords.size() > 100
                || keywords.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new GenericException("来源或解析关键字无效，单次最多 100 个");
        }
        Map<String, OptionDTO> matches = new LinkedHashMap<>();
        if ("MEMBER".equals(source) || "DEPARTMENT".equals(source)) {
            List<? extends BaseTreeNode> roots = "MEMBER".equals(source)
                    ? getDeptUserTree(false) : getDeptTree();
            collectSourceTree(roots, "MEMBER".equals(source), keywords, matches);
            return new ArrayList<>(matches.values());
        }
        // 兼容旧解析协议，但复用页面的组织、角色、数据范围和上下架过滤，不能直查业务表。
        for (String identity : List.of("id", "name")) {
            for (int page = 1; page <= 100; page++) {
                String body = JSON.toJSONString(Map.of("current", page, "pageSize", 100,
                        "viewId", InternalUserView.ALL.name(), "customFormId", source,
                        "filters", List.of(Map.of("name", identity, "operator", "IN", "value", keywords,
                                "type", "INPUT", "multipleValue", false))));
                Pager<?> response = resolveSourcePage(source, body);
                if (response == null || !(response.getList() instanceof List<?> rows)) {
                    throw new GenericException("数据源候选响应不完整");
                }
                for (OptionDTO option : JSON.parseArray(JSON.toJSONString(rows), OptionDTO.class)) {
                    if (option.getId() != null && (keywords.contains(option.getIdAsString())
                            || keywords.contains(option.getName()))) {
                        matches.put(option.getIdAsString(), option);
                    }
                }
                if ((long) page * 100 >= response.getTotal()) break;
                // ponytail: 每个精确条件最多一万候选，超过上限拒绝；更大规模需专用权限化唯一查询。
                if (rows.isEmpty() || page == 100) throw new GenericException("候选未完整读取，请缩小解析范围");
            }
        }
        return new ArrayList<>(matches.values());
    }

    private Pager<?> resolveSourcePage(String source, String body) {
        return switch (source) {
            case "CUSTOMER" -> sourceCustomerPage(JSON.parseObject(body, CustomerPageRequest.class));
            case "CLUE" -> sourceCluePage(JSON.parseObject(body, CluePageRequest.class));
            case "CONTACT" -> sourceContactPage(JSON.parseObject(body, CustomerContactPageRequest.class));
            case "OPPORTUNITY" -> sourceOpportunityPage(JSON.parseObject(body, OpportunityPageRequest.class));
            case "PRODUCT" -> sourceProductPage(JSON.parseObject(body, ProductPageRequest.class));
            case "PRICE" -> sourceProductPage(JSON.parseObject(body, ProductPricePageRequest.class));
            case "QUOTATION" -> sourceOpportunityQuotationPage(JSON.parseObject(body, OpportunityQuotationPageRequest.class));
            case "CONTRACT" -> sourceContractPage(JSON.parseObject(body, ContractPageRequest.class));
            case "PAYMENT_PLAN" -> sourcePlanPage(JSON.parseObject(body, ContractPaymentPlanPageRequest.class));
            case "CONTRACT_PAYMENT_RECORD" -> sourceRecordPage(JSON.parseObject(body, ContractPaymentRecordPageRequest.class));
            case "ORDER" -> list(JSON.parseObject(body, OrderPageRequest.class));
            case "INVOICE" -> list(JSON.parseObject(body, ContractInvoicePageRequest.class));
            case "BUSINESS_TITLE" -> sourceBusinessTitlePage(JSON.parseObject(body, BusinessTitlePageRequest.class));
            default -> sourceCustomFormDataPage(JSON.parseObject(body, CustomFormDataPageRequest.class));
        };
    }

    private void collectSourceTree(List<? extends BaseTreeNode> nodes, boolean members, List<String> keywords,
                                   Map<String, OptionDTO> matches) {
        for (BaseTreeNode node : nodes) {
            boolean selectable = !members || node instanceof DeptUserTreeNode user && "USER".equals(user.getNodeType());
            if (selectable && (keywords.contains(node.getId()) || keywords.contains(node.getName()))) {
                matches.put(node.getId(), new OptionDTO(node.getId(), node.getName()));
            }
            if (node.getChildren() != null) collectSourceTree(node.getChildren(), members, keywords, matches);
        }
    }

    @GetMapping("/display/{formKey}")
    @Operation(summary = "获取表单配置")
    public ModuleFormConfigDTO getFieldList(@PathVariable String formKey) {
        return moduleFormService.getSourceDisplayFields(formKey, OrganizationContext.getOrganizationId());
    }

	@GetMapping("/source/config/{formKey}")
	@Operation(summary = "获取数据源表单配置")
	public ModuleFormConfigDTO getSourceFormConfig(@PathVariable String formKey) {
		return formCacheService.getBusinessFormConfig(formKey, OrganizationContext.getOrganizationId());
	}

	@PostMapping("/source/ref-detail")
	@Operation(summary = "批量获取数据源引用详情")
	public List<DatasourceRefDTO> getDatasourceRefDetail(@Valid @RequestBody DatasourceRefQueryRequest request) {
		return moduleFieldService.getSourceRefDetail(request);
	}

}
