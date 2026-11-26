package cn.cordys.crm.system.service.detailtab;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.InternalUserView;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.CustomerTabRow;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.dto.condition.BaseCondition;
import cn.cordys.common.dto.condition.FilterCondition;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.permission.RelatedResourcePermissionService;
import cn.cordys.common.permission.ResourcePermissionService;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.crm.clue.dto.request.CluePageRequest;
import cn.cordys.crm.clue.service.ClueService;
import cn.cordys.crm.contract.dto.request.ContractInvoicePageRequest;
import cn.cordys.crm.contract.dto.request.ContractPageRequest;
import cn.cordys.crm.contract.dto.request.ContractPaymentPlanPageRequest;
import cn.cordys.crm.contract.dto.request.ContractPaymentRecordPageRequest;
import cn.cordys.crm.contract.dto.request.ContractPaymentRecordStatisticRequest;
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
import cn.cordys.crm.opportunity.dto.request.OpportunitySearchStatisticRequest;
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
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * 标准表单自定义关联字段查询。
 * <p>
 * 该 Handler 处理“关联表单是系统标准表单，但关联字段是用户新增的数据源字段”的场景。
 * 固定关联条件来自已保存的标签配置，并与用户输入的关键词、高级搜索条件共同以 AND 方式生效。
 * </p>
 */
@Component
@RequiredArgsConstructor
public class StandardRelatedDetailTabDataHandler implements DetailTabDataHandler {

    @Resource
    private ResourcePermissionService resourcePermissionService;
    @Resource
    private RelatedResourcePermissionService relatedResourcePermissionService;
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
     * 处理关联表单是标准表单的标签查询（含带关联表单/字段的内置标签）。
     * <p>
     * 独立内置标签（无关联表单，如变更记录）和自定义表单标签不在此处理：
     * 前者仍走原有专用接口，后者由 {@link CustomRelatedDetailTabDataHandler} 处理。
     * </p>
     */
    @Override
    public boolean supports(DetailTabQueryContext context) {
        if (context.tab().getRelatedForm() == null || context.tab().getRelatedField() == null) {
            return false;
        }
        return FormKey.ofKey(context.tab().getRelatedForm().getIdAsString()) != null;
    }

    @Override
    public PagerWithOption<?> page(DetailTabQueryContext context) {
        FormKey relatedForm = FormKey.ofKey(context.tab().getRelatedForm().getIdAsString());
        return switch (relatedForm) {
        case CLUE -> relatedPage(context, relatedForm, PermissionConstants.CLUE_MANAGEMENT_READ,
                new CluePageRequest(), (request, scope) -> clueService.list(request, context.userId(),
                        context.organizationId(), scope, false));
        case CUSTOMER -> relatedPage(context, relatedForm, PermissionConstants.CUSTOMER_MANAGEMENT_READ,
                new CustomerPageRequest(), (request, scope) -> customerService.list(request, context.userId(),
                        context.organizationId(), scope));
        case CONTACT -> relatedPage(context, relatedForm, PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ,
                new CustomerContactPageRequest(), (request, scope) -> customerContactService.list(request,
                        context.userId(), context.organizationId(), scope));
        case OPPORTUNITY -> relatedPage(context, relatedForm, PermissionConstants.OPPORTUNITY_MANAGEMENT_READ,
                new OpportunityPageRequest(), (request, scope) -> opportunityService.list(request, context.userId(),
                        context.organizationId(), scope, false));
        case PRODUCT -> products(context);
        case PRICE -> prices(context);
        case QUOTATION -> quotations(context);
        case CONTRACT -> relatedPage(context, relatedForm, PermissionConstants.CONTRACT_READ,
                new ContractPageRequest(), (request, scope) -> contractService.list(request, context.userId(),
                        context.organizationId(), scope, false));
        case INVOICE -> relatedPage(context, relatedForm, PermissionConstants.CONTRACT_INVOICE_READ,
                new ContractInvoicePageRequest(), (request, scope) -> contractInvoiceService.list(request,
                        context.userId(), context.organizationId(), scope));
        case CONTRACT_PAYMENT_PLAN -> relatedPage(context, relatedForm, PermissionConstants.CONTRACT_PAYMENT_PLAN_READ,
                new ContractPaymentPlanPageRequest(), (request, scope) -> contractPaymentPlanService.list(request,
                        context.userId(), context.organizationId(), scope));
        case CONTRACT_PAYMENT_RECORD -> relatedPage(context, relatedForm,
                PermissionConstants.CONTRACT_PAYMENT_RECORD_READ, new ContractPaymentRecordPageRequest(),
                (request, scope) -> contractPaymentRecordService.list(request, context.userId(),
                        context.organizationId(), scope));
        case ORDER -> relatedPage(context, relatedForm, PermissionConstants.ORDER_READ,
                new OrderPageRequest(), (request, scope) -> orderService.list(request, context.userId(),
                        context.organizationId(), scope, false));
        case FOLLOW_RECORD -> relatedPage(context, relatedForm, PermissionConstants.FOLLOW_RECORD_READ,
                new RecordHomePageRequest(), (request, scope) -> followUpRecordService.totalList(request,
                        context.userId(), context.organizationId(), scope));
        case FOLLOW_PLAN -> relatedPage(context, relatedForm, PermissionConstants.FOLLOW_PLAN_READ,
                new PlanHomePageRequest(), (request, scope) -> followUpPlanService.totalList(request,
                        context.userId(), context.organizationId(), scope));
        };
    }

    private <T extends BaseCondition, R extends CustomerTabRow> PagerWithOption<List<R>> relatedPage(
            DetailTabQueryContext context, FormKey relatedForm, String permission, T target,
            BiFunction<T, DeptDataPermissionDTO, PagerWithOption<List<R>>> query) {
        replaceSearchBusinessKey(context);
        if (relatedForm != FormKey.FOLLOW_RECORD && relatedForm != FormKey.FOLLOW_PLAN) {
            checkPermission(context, permission);
        }
        DetailTabScope scope = dataScopes(context, permission);
        T request = prepare(target, relatedForm, context, scope.effective());
        PagerWithOption<List<R>> page = query.apply(request, scope.effective());
        if (page != null && scope.role() != null) {
            relatedResourcePermissionService.markCustomerTabRows(page.getList(), scope.role(), context.userId(),
                    context.organizationId());
        }
        return page;
    }

    @Override
    public Object statistic(DetailTabQueryContext context) {
        FormKey relatedForm = FormKey.ofKey(context.tab().getRelatedForm().getIdAsString());
        return switch (relatedForm) {
        case OPPORTUNITY -> opportunityStatistic(context);
        case CONTRACT -> contractStatistic(context);
        case ORDER -> orderStatistic(context);
        case CONTRACT_PAYMENT_RECORD -> paymentRecordStatistic(context);
        default -> throw new GenericException(Translator.get("module.tab.statistic.not_support"));
        };
    }

    /**
     * 商机金额统计，口径与商机主列表 /opportunity/statistic 一致。
     */
    private Object opportunityStatistic(DetailTabQueryContext context) {
        replaceSearchBusinessKey(context);
        checkPermission(context, PermissionConstants.OPPORTUNITY_MANAGEMENT_READ);
        DeptDataPermissionDTO scope = dataPermission(context, PermissionConstants.OPPORTUNITY_MANAGEMENT_READ);
        OpportunitySearchStatisticRequest request = prepare(new OpportunitySearchStatisticRequest(), FormKey.OPPORTUNITY,
                context, scope);
        return opportunityService.searchStatistic(request, context.userId(), context.organizationId(),
                scope);
    }

    /**
     * 合同金额统计，口径与合同主列表 /contract/statistic 一致。
     */
    private Object contractStatistic(DetailTabQueryContext context) {
        replaceSearchBusinessKey(context);
        checkPermission(context, PermissionConstants.CONTRACT_READ);
        DeptDataPermissionDTO scope = dataPermission(context, PermissionConstants.CONTRACT_READ);
        BaseCondition request = prepare(new BaseCondition(), FormKey.CONTRACT, context, scope);
        return contractService.searchStatistic(request, context.userId(), context.organizationId(),
                scope);
    }

    /**
     * 订单金额统计，口径与订单主列表 /order/statistic 一致。
     */
    private Object orderStatistic(DetailTabQueryContext context) {
        replaceSearchBusinessKey(context);
        checkPermission(context, PermissionConstants.ORDER_READ);
        DeptDataPermissionDTO scope = dataPermission(context, PermissionConstants.ORDER_READ);
        BaseCondition request = prepare(new BaseCondition(), FormKey.ORDER, context, scope);
        return orderService.searchStatistic(request, context.userId(), context.organizationId(),
                scope);
    }

    /**
     * 回款记录金额统计，口径与回款记录主列表 /contract/payment-record/statistic 一致。
     */
    private Object paymentRecordStatistic(DetailTabQueryContext context) {
        replaceSearchBusinessKey(context);
        checkPermission(context, PermissionConstants.CONTRACT_PAYMENT_RECORD_READ);
        DeptDataPermissionDTO scope = dataPermission(context, PermissionConstants.CONTRACT_PAYMENT_RECORD_READ);
        ContractPaymentRecordStatisticRequest request = prepare(new ContractPaymentRecordStatisticRequest(),
                FormKey.CONTRACT_PAYMENT_RECORD, context, scope);
        return contractPaymentRecordService.searchStatistic(request, context.userId(), context.organizationId(),
                scope);
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
                if (Strings.CS.equals(value.getKey(), moduleField.getInternalKey())
                        && StringUtils.isNotBlank(value.getBusinessKey())) {
                    // 查询使用 businessKey
                    context.tab().getRelatedField().setId(value.getBusinessKey());
                    break;
                }
            }
        }
    }

    private PagerWithOption<?> products(DetailTabQueryContext context) {
        replaceSearchBusinessKey(context);
        checkPermission(context, PermissionConstants.PRODUCT_MANAGEMENT_READ);
        ProductPageRequest request = prepare(new ProductPageRequest(), FormKey.PRODUCT, context, null);
        return productService.list(request, context.organizationId());
    }

    private PagerWithOption<?> prices(DetailTabQueryContext context) {
        replaceSearchBusinessKey(context);
        checkPermission(context, PermissionConstants.PRICE_READ);
        ProductPricePageRequest request = prepare(new ProductPricePageRequest(), FormKey.PRICE, context, null);
        return productPriceService.list(request, context.organizationId());
    }

    private PagerWithOption<?> quotations(DetailTabQueryContext context) {
        replaceSearchBusinessKey(context);
        checkPermission(context, PermissionConstants.OPPORTUNITY_QUOTATION_READ);
        DeptDataPermissionDTO scope = dataPermission(context, PermissionConstants.OPPORTUNITY_QUOTATION_READ);
        OpportunityQuotationPageRequest request = prepare(new OpportunityQuotationPageRequest(), FormKey.QUOTATION,
                context, scope);
        return opportunityQuotationService.list(request, context.organizationId(), context.userId(),
                scope, false);
    }

    private <T extends BaseCondition> T prepare(T target, FormKey relatedForm, DetailTabQueryContext context,
            DeptDataPermissionDTO scope) {
        T request = BeanUtils.copyBean(target, context.request());
        request.setViewId(InternalUserView.ALL.name());
        request.setFilters(new ArrayList<>(context.request().getFilters()));

        FilterCondition relation = new FilterCondition();
        relation.setName(context.tab().getRelatedField().getIdAsString());
        relation.setOperator(FilterCondition.CombineConditionOperator.EQUALS.name());
        relation.setValue(context.resourceId());
        List<FilterCondition> filters = request.getFilters();
        filters.add(relation);
        if (FormKey.CUSTOMER.getKey().equals(context.formKey()) && scope != null
                && !scope.getExcludedOwnerIds().isEmpty()) {
            FilterCondition isolation = new FilterCondition();
            isolation.setName("owner");
            isolation.setOperator(FilterCondition.CombineConditionOperator.NOT_IN.name());
            isolation.setValue(List.copyOf(scope.getExcludedOwnerIds()));
            filters.add(isolation);
        }
        request.setFilters(filters);

        ConditionFilterUtils.parseCondition(request, relatedForm.getKey());
        return request;
    }

    private void checkPermission(DetailTabQueryContext context, String permission) {
        if (!relatedResourcePermissionService.isOwner(context.formKey(), context.resourceId(), context.userId(),
                context.organizationId())) {
            resourcePermissionService.checkPermission(permission);
        }
    }

    private DeptDataPermissionDTO dataPermission(DetailTabQueryContext context, String permission) {
        return dataScopes(context, permission).effective();
    }

    private DetailTabScope dataScopes(DetailTabQueryContext context, String permission) {
        boolean customerTab = FormKey.CUSTOMER.getKey().equals(context.formKey());
        if (customerTab) {
            DeptDataPermissionDTO customerScope = relatedResourcePermissionService.getCustomerTabDataPermission(
                    context.resourceId(), context.userId(), context.organizationId());
            if (customerScope != null) {
                if (FormKey.QUOTATION.getKey().equals(context.tab().getRelatedForm().getIdAsString())) {
                    DeptDataPermissionDTO all = new DeptDataPermissionDTO();
                    all.setAll(true);
                    return new DetailTabScope(all, null);
                }
                DeptDataPermissionDTO roleScope = dataScopeService.getDeptDataPermission(context.userId(),
                        context.organizationId(), InternalUserView.ALL.name(), permission);
                DeptDataPermissionDTO effective = relatedResourcePermissionService.mergeCustomerTabDataPermission(
                        roleScope, customerScope, context.organizationId());
                return new DetailTabScope(effective, roleScope);
            }
        }
        if (!PermissionConstants.FOLLOW_RECORD_READ.equals(permission)
                && !PermissionConstants.FOLLOW_PLAN_READ.equals(permission)
                && relatedResourcePermissionService.isOwner(context.formKey(), context.resourceId(), context.userId(),
                        context.organizationId())) {
            DeptDataPermissionDTO all = new DeptDataPermissionDTO();
            all.setAll(true);
            return new DetailTabScope(all, null);
        }
        DeptDataPermissionDTO roleScope = dataScopeService.getDeptDataPermission(context.userId(),
                context.organizationId(), InternalUserView.ALL.name(), permission);
        return new DetailTabScope(roleScope, customerTab ? roleScope : null);
    }

    private record DetailTabScope(DeptDataPermissionDTO effective, DeptDataPermissionDTO role) {
    }
}
