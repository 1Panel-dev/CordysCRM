package cn.cordys.crm.system.service.detailtab;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.InternalUserView;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.BasePageRequest;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.dto.condition.FilterCondition;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.permission.ResourcePermissionService;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.clue.dto.request.CluePageRequest;
import cn.cordys.crm.clue.service.ClueService;
import cn.cordys.crm.contract.dto.request.ContractInvoicePageRequest;
import cn.cordys.crm.contract.dto.request.ContractPageRequest;
import cn.cordys.crm.contract.dto.request.ContractPaymentPlanPageRequest;
import cn.cordys.crm.contract.dto.request.ContractPaymentRecordPageRequest;
import cn.cordys.crm.contract.service.ContractInvoiceService;
import cn.cordys.crm.contract.service.ContractPaymentPlanService;
import cn.cordys.crm.contract.service.ContractPaymentRecordService;
import cn.cordys.crm.contract.service.ContractService;
import cn.cordys.crm.customer.dto.request.CustomerContactPageRequest;
import cn.cordys.crm.customer.dto.request.CustomerPageRequest;
import cn.cordys.crm.customer.service.CustomerContactService;
import cn.cordys.crm.customer.service.CustomerService;
import cn.cordys.crm.follow.dto.request.PlanHomePageRequest;
import cn.cordys.crm.follow.dto.request.RecordHomePageRequest;
import cn.cordys.crm.follow.service.FollowUpPlanService;
import cn.cordys.crm.follow.service.FollowUpRecordService;
import cn.cordys.crm.opportunity.dto.request.OpportunityPageRequest;
import cn.cordys.crm.opportunity.dto.request.OpportunityQuotationPageRequest;
import cn.cordys.crm.opportunity.service.OpportunityQuotationService;
import cn.cordys.crm.opportunity.service.OpportunityService;
import cn.cordys.crm.order.dto.request.OrderPageRequest;
import cn.cordys.crm.order.service.OrderService;
import cn.cordys.crm.product.dto.request.ProductPageRequest;
import cn.cordys.crm.product.dto.request.ProductPricePageRequest;
import cn.cordys.crm.product.service.ProductPriceService;
import cn.cordys.crm.product.service.ProductService;
import cn.cordys.crm.system.domain.ModuleField;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.security.SessionUtils;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 标准表单自定义关联字段查询。
 *
 * <p>该 Handler 处理“关联表单是系统标准表单，但关联字段是用户新增的数据源字段”的场景。
 * 固定关联条件来自已保存的标签配置，并与用户输入的关键词、高级搜索条件共同以 AND 方式生效。</p>
 */
@Component
@RequiredArgsConstructor
public class StandardRelatedDetailTabDataHandler implements DetailTabDataHandler {

    @Resource
    private ResourcePermissionService resourcePermissionService;
    @Resource
    private DataScopeService dataScopeService;
    @Resource
    private ClueService clueService;
    @Resource
    private CustomerService customerService;
    @Resource
    private CustomerContactService customerContactService;
    @Resource
    private OpportunityService opportunityService;
    @Resource
    private OpportunityQuotationService opportunityQuotationService;
    @Resource
    private ContractService contractService;
    @Resource
    private ContractPaymentPlanService contractPaymentPlanService;
    @Resource
    private ContractPaymentRecordService contractPaymentRecordService;
    @Resource
    private ContractInvoiceService contractInvoiceService;
    @Resource
    private OrderService orderService;
    @Resource
    private ProductService productService;
    @Resource
    private ProductPriceService productPriceService;
    @Resource
    private BaseMapper<ModuleField> moduleFieldBaseMapper;
    @Resource
    private FollowUpRecordService followUpRecordService;
    @Resource
    private FollowUpPlanService followUpPlanService;


    /**
     * 处理非内置 tab 中的 内置表单的查询
     * (内置的 tab 走原来的接口逻辑)
     * @param context
     * @return
     */
    @Override
    public boolean supports(DetailTabQueryContext context) {
        if (StringUtils.isNotBlank(context.tab().getInternalKey())
                || context.tab().getRelatedForm() == null
                || context.tab().getRelatedField() == null) {
            return false;
        }
        return FormKey.ofKey(context.tab().getRelatedForm().getIdAsString()) != null;
    }

    @Override
    public PagerWithOption<?> page(DetailTabQueryContext context) {
        FormKey relatedForm = FormKey.ofKey(context.tab().getRelatedForm().getIdAsString());
        return switch (relatedForm) {
            case CLUE -> clues(context);
            case CUSTOMER -> customers(context);
            case CONTACT -> contacts(context);
            case OPPORTUNITY -> opportunities(context);
            case PRODUCT -> products(context);
            case PRICE -> prices(context);
            case QUOTATION -> quotations(context);
            case CONTRACT -> contracts(context);
            case INVOICE -> invoices(context);
            case CONTRACT_PAYMENT_PLAN -> paymentPlans(context);
            case CONTRACT_PAYMENT_RECORD -> paymentRecords(context);
            case ORDER -> orders(context);
            case FOLLOW_RECORD -> followRecords(context);
            case FOLLOW_PLAN -> followPlans(context);
        };
    }

    private PagerWithOption<?> followRecords(DetailTabQueryContext context) {
        replaceSearchBusinessKey(context);
        RecordHomePageRequest request = prepare(new RecordHomePageRequest(), FormKey.FOLLOW_RECORD, context);
        DeptDataPermissionDTO clueDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId(), request.getViewId(), PermissionConstants.CLUE_MANAGEMENT_READ);
        DeptDataPermissionDTO customerDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId(), request.getViewId(), PermissionConstants.CUSTOMER_MANAGEMENT_READ);
        return followUpRecordService.totalList(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), clueDataPermission, customerDataPermission);
    }

    private PagerWithOption<?> followPlans(DetailTabQueryContext context) {
        replaceSearchBusinessKey(context);
        PlanHomePageRequest request = prepare(new PlanHomePageRequest(), FormKey.FOLLOW_PLAN, context);
        DeptDataPermissionDTO clueDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId(), request.getViewId(), PermissionConstants.CLUE_MANAGEMENT_READ);
        DeptDataPermissionDTO customerDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId(), request.getViewId(), PermissionConstants.CUSTOMER_MANAGEMENT_READ);
        return followUpPlanService.totalList(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), clueDataPermission, customerDataPermission);
    }

    private void replaceSearchBusinessKey(DetailTabQueryContext context) {
        String fieldId = context.tab().getRelatedField().getIdAsString();
        ModuleField moduleField = moduleFieldBaseMapper.selectByPrimaryKey(fieldId);
        if (moduleField == null) {
            throw new GenericException(Translator.get("module.tab.not_exist"));
        }
        if (StringUtils.isNotBlank(moduleField.getInternalKey())) {
            BusinessModuleField[] values = BusinessModuleField.values();
            for (BusinessModuleField value : values) {
                if (Strings.CS.equals(value.getKey(), moduleField.getInternalKey()) && StringUtils.isNotBlank(value.getBusinessKey())) {
                    // 查询使用 businessKey
                    context.tab().getRelatedField().setId(value.getBusinessKey());
                    break;
                }
            }
        }
    }

    private PagerWithOption<?> clues(DetailTabQueryContext context) {
        replaceSearchBusinessKey(context);
        checkPermission(PermissionConstants.CLUE_MANAGEMENT_READ);
        CluePageRequest request = prepare(new CluePageRequest(), FormKey.CLUE, context);
        return clueService.list(request, context.userId(), context.organizationId(),
                dataPermission(context, PermissionConstants.CLUE_MANAGEMENT_READ), false);
    }

    private PagerWithOption<?> customers(DetailTabQueryContext context) {
        replaceSearchBusinessKey(context);
        checkPermission(PermissionConstants.CUSTOMER_MANAGEMENT_READ);
        CustomerPageRequest request = prepare(new CustomerPageRequest(), FormKey.CUSTOMER, context);
        return customerService.list(request, context.userId(), context.organizationId(),
                dataPermission(context, PermissionConstants.CUSTOMER_MANAGEMENT_READ));
    }

    private PagerWithOption<?> contacts(DetailTabQueryContext context) {
        replaceSearchBusinessKey(context);
        checkPermission(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ);
        CustomerContactPageRequest request = prepare(new CustomerContactPageRequest(), FormKey.CONTACT, context);
        return customerContactService.list(request, context.userId(), context.organizationId(),
                dataPermission(context, PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ));
    }

    private PagerWithOption<?> opportunities(DetailTabQueryContext context) {
        replaceSearchBusinessKey(context);
        checkPermission(PermissionConstants.OPPORTUNITY_MANAGEMENT_READ);
        OpportunityPageRequest request = prepare(new OpportunityPageRequest(), FormKey.OPPORTUNITY, context);
        return opportunityService.list(request, context.userId(), context.organizationId(),
                dataPermission(context, PermissionConstants.OPPORTUNITY_MANAGEMENT_READ), false);
    }

    private PagerWithOption<?> products(DetailTabQueryContext context) {
        replaceSearchBusinessKey(context);
        checkPermission(PermissionConstants.PRODUCT_MANAGEMENT_READ);
        ProductPageRequest request = prepare(new ProductPageRequest(), FormKey.PRODUCT, context);
        return productService.list(request, context.organizationId());
    }

    private PagerWithOption<?> prices(DetailTabQueryContext context) {
        replaceSearchBusinessKey(context);
        checkPermission(PermissionConstants.PRICE_READ);
        ProductPricePageRequest request = prepare(new ProductPricePageRequest(), FormKey.PRICE, context);
        return productPriceService.list(request, context.organizationId());
    }

    private PagerWithOption<?> quotations(DetailTabQueryContext context) {
        replaceSearchBusinessKey(context);
        checkPermission(PermissionConstants.OPPORTUNITY_QUOTATION_READ);
        OpportunityQuotationPageRequest request = prepare(new OpportunityQuotationPageRequest(), FormKey.QUOTATION, context);
        return opportunityQuotationService.list(request, context.organizationId(), context.userId(),
                dataPermission(context, PermissionConstants.OPPORTUNITY_QUOTATION_READ), false);
    }

    private PagerWithOption<?> contracts(DetailTabQueryContext context) {
        replaceSearchBusinessKey(context);
        checkPermission(PermissionConstants.CONTRACT_READ);
        ContractPageRequest request = prepare(new ContractPageRequest(), FormKey.CONTRACT, context);
        return contractService.list(request, context.userId(), context.organizationId(),
                dataPermission(context, PermissionConstants.CONTRACT_READ), false);
    }

    private PagerWithOption<?> invoices(DetailTabQueryContext context) {
        replaceSearchBusinessKey(context);
        checkPermission(PermissionConstants.CONTRACT_INVOICE_READ);
        ContractInvoicePageRequest request = prepare(new ContractInvoicePageRequest(), FormKey.INVOICE, context);
        return contractInvoiceService.list(request, context.userId(), context.organizationId(),
                dataPermission(context, PermissionConstants.CONTRACT_INVOICE_READ));
    }

    private PagerWithOption<?> paymentPlans(DetailTabQueryContext context) {
        replaceSearchBusinessKey(context);
        checkPermission(PermissionConstants.CONTRACT_PAYMENT_PLAN_READ);
        ContractPaymentPlanPageRequest request = prepare(new ContractPaymentPlanPageRequest(), FormKey.CONTRACT_PAYMENT_PLAN, context);
        return contractPaymentPlanService.list(request, context.userId(), context.organizationId(),
                dataPermission(context, PermissionConstants.CONTRACT_PAYMENT_PLAN_READ));
    }

    private PagerWithOption<?> paymentRecords(DetailTabQueryContext context) {
        replaceSearchBusinessKey(context);
        checkPermission(PermissionConstants.CONTRACT_PAYMENT_RECORD_READ);
        ContractPaymentRecordPageRequest request = prepare(new ContractPaymentRecordPageRequest(), FormKey.CONTRACT_PAYMENT_RECORD, context);
        return contractPaymentRecordService.list(request, context.userId(), context.organizationId(),
                dataPermission(context, PermissionConstants.CONTRACT_PAYMENT_RECORD_READ));
    }

    private PagerWithOption<?> orders(DetailTabQueryContext context) {
        replaceSearchBusinessKey(context);
        checkPermission(PermissionConstants.ORDER_READ);
        OrderPageRequest request = prepare(new OrderPageRequest(), FormKey.ORDER, context);
        return orderService.list(request, context.userId(), context.organizationId(),
                dataPermission(context, PermissionConstants.ORDER_READ), false);
    }

    private <T extends BasePageRequest> T prepare(T target, FormKey relatedForm, DetailTabQueryContext context) {
        T request = BeanUtils.copyBean(target, context.request());
        request.setViewId(InternalUserView.ALL.name());
        request.setFilters(new ArrayList<>(context.request().getFilters()));

        FilterCondition relation = new FilterCondition();
        relation.setName(context.tab().getRelatedField().getIdAsString());
        relation.setOperator(FilterCondition.CombineConditionOperator.EQUALS.name());
        relation.setValue(context.resourceId());
        List<FilterCondition> filters = request.getFilters();
        filters.add(relation);
        request.setFilters(filters);

        ConditionFilterUtils.parseCondition(request, relatedForm.getKey());
        return request;
    }

    private void checkPermission(String permission) {
        resourcePermissionService.checkPermission(permission);
    }

    private DeptDataPermissionDTO dataPermission(DetailTabQueryContext context, String permission) {
        return dataScopeService.getDeptDataPermission(context.userId(), context.organizationId(),
                InternalUserView.ALL.name(), permission);
    }
}
