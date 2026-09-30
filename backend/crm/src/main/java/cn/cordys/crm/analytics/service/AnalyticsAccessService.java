package cn.cordys.crm.analytics.service;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.BasePageRequest;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.domain.BaseResourceSubField;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.permission.PermissionUtils;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.crm.analytics.dto.request.AnalyticsQueryRequest.View;
import cn.cordys.crm.analytics.dto.response.AnalyticsQueryResponse.Scope;
import cn.cordys.crm.clue.domain.Clue;
import cn.cordys.crm.clue.dto.request.CluePageRequest;
import cn.cordys.crm.clue.mapper.ExtClueMapper;
import cn.cordys.crm.clue.service.PoolClueService;
import cn.cordys.crm.contract.domain.*;
import cn.cordys.crm.contract.dto.request.*;
import cn.cordys.crm.contract.mapper.*;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.customer.domain.CustomerContact;
import cn.cordys.crm.customer.dto.request.CustomerContactPageRequest;
import cn.cordys.crm.customer.dto.request.CustomerPageRequest;
import cn.cordys.crm.customer.mapper.ExtCustomerContactMapper;
import cn.cordys.crm.customer.mapper.ExtCustomerMapper;
import cn.cordys.crm.customer.service.PoolCustomerService;
import cn.cordys.crm.follow.domain.FollowUpPlan;
import cn.cordys.crm.follow.domain.FollowUpRecord;
import cn.cordys.crm.follow.dto.request.PlanHomePageRequest;
import cn.cordys.crm.follow.dto.request.RecordHomePageRequest;
import cn.cordys.crm.follow.mapper.ExtFollowUpPlanMapper;
import cn.cordys.crm.follow.mapper.ExtFollowUpRecordMapper;
import cn.cordys.crm.form.domain.CustomFormData;
import cn.cordys.crm.form.dto.request.CustomFormDataPageRequest;
import cn.cordys.crm.form.mapper.ExtCustomFormDataMapper;
import cn.cordys.crm.form.service.CustomFormAnalyticsAccessService;
import cn.cordys.crm.opportunity.domain.Opportunity;
import cn.cordys.crm.opportunity.domain.OpportunityQuotation;
import cn.cordys.crm.opportunity.dto.request.OpportunityPageRequest;
import cn.cordys.crm.opportunity.dto.request.OpportunityQuotationPageRequest;
import cn.cordys.crm.opportunity.mapper.ExtOpportunityMapper;
import cn.cordys.crm.opportunity.mapper.ExtOpportunityQuotationMapper;
import cn.cordys.crm.order.domain.Order;
import cn.cordys.crm.order.dto.request.OrderPageRequest;
import cn.cordys.crm.order.mapper.ExtOrderMapper;
import cn.cordys.crm.product.domain.Product;
import cn.cordys.crm.product.domain.ProductPrice;
import cn.cordys.crm.product.dto.request.ProductPageRequest;
import cn.cordys.crm.product.dto.request.ProductPricePageRequest;
import cn.cordys.crm.product.mapper.ExtProductMapper;
import cn.cordys.crm.product.mapper.ExtProductPriceMapper;
import cn.cordys.mybatis.EntityTableMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 将各模块的列表授权入口接到统计查询。只生成原列表 SQL，不执行列表、不启动 PageHelper。
 * 聚合查询用这个子查询的记录 ID 限定范围，避免复制各模块的业务可见性谓词。
 */
@Service
public class AnalyticsAccessService {
    @Resource private DataScopeService dataScopeService;
    @Resource private SqlSessionFactory sqlSessionFactory;
    @Resource private CustomFormAnalyticsAccessService customFormAccess;
    @Resource private PoolCustomerService poolCustomerService;
    @Resource private PoolClueService poolClueService;

    static final Map<String, Source> SOURCES = Map.ofEntries(
            entry("clue", Clue.class, ExtClueMapper.class, CluePageRequest::new, PermissionConstants.CLUE_MANAGEMENT_READ),
            entry("customer", Customer.class, ExtCustomerMapper.class, CustomerPageRequest::new, PermissionConstants.CUSTOMER_MANAGEMENT_READ),
            entry("contact", CustomerContact.class, ExtCustomerContactMapper.class, CustomerContactPageRequest::new, PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ),
            entry("opportunity", Opportunity.class, ExtOpportunityMapper.class, OpportunityPageRequest::new, PermissionConstants.OPPORTUNITY_MANAGEMENT_READ),
            entry("quotation", OpportunityQuotation.class, ExtOpportunityQuotationMapper.class, OpportunityQuotationPageRequest::new, PermissionConstants.OPPORTUNITY_QUOTATION_READ),
            entry("contract", Contract.class, ExtContractMapper.class, ContractPageRequest::new, PermissionConstants.CONTRACT_READ),
            entry("invoice", ContractInvoice.class, ExtContractInvoiceMapper.class, ContractInvoicePageRequest::new, PermissionConstants.CONTRACT_INVOICE_READ),
            entry("contractPaymentPlan", ContractPaymentPlan.class, ExtContractPaymentPlanMapper.class, ContractPaymentPlanPageRequest::new, PermissionConstants.CONTRACT_PAYMENT_PLAN_READ),
            entry("contractPaymentRecord", ContractPaymentRecord.class, ExtContractPaymentRecordMapper.class, ContractPaymentRecordPageRequest::new, PermissionConstants.CONTRACT_PAYMENT_RECORD_READ),
            entry("order", Order.class, ExtOrderMapper.class, OrderPageRequest::new, PermissionConstants.ORDER_READ),
            entry("product", Product.class, ExtProductMapper.class, ProductPageRequest::new, PermissionConstants.PRODUCT_MANAGEMENT_READ),
            entry("price", ProductPrice.class, ExtProductPriceMapper.class, ProductPricePageRequest::new, PermissionConstants.PRICE_READ),
            entry("record", FollowUpRecord.class, ExtFollowUpRecordMapper.class, RecordHomePageRequest::new, PermissionConstants.CLUE_MANAGEMENT_READ, PermissionConstants.CUSTOMER_MANAGEMENT_READ, PermissionConstants.OPPORTUNITY_MANAGEMENT_READ),
            entry("plan", FollowUpPlan.class, ExtFollowUpPlanMapper.class, PlanHomePageRequest::new, PermissionConstants.CLUE_MANAGEMENT_READ, PermissionConstants.CUSTOMER_MANAGEMENT_READ));

    private static Map.Entry<String, Source> entry(String key, Class<?> entity, Class<?> mapper,
                                                   Supplier<BasePageRequest> request, String... permissions) {
        String method = key.equals("record") || key.equals("plan") ? ".selectTotalList" : ".list";
        return Map.entry(key, new Source(entity, mapper.getName() + method, request, List.of(permissions)));
    }

    public Access resolve(String formKey, View view, String poolId, String userId, String orgId) {
        if (StringUtils.isAnyBlank(formKey, userId, orgId) || view == null) {
            throw new GenericException(CrmHttpResultCode.UNAUTHORIZED);
        }
        Source source = SOURCES.get(formKey);
        boolean custom = source == null;
        if (custom) {
            source = new Source(CustomFormData.class, ExtCustomFormDataMapper.class.getName() + ".list",
                    CustomFormDataPageRequest::new, List.of(PermissionConstants.CUSTOM_FORM_READ));
        }
        boolean pool = StringUtils.isNotBlank(poolId);
        if (pool && (!List.of("clue", "customer").contains(formKey) || view != View.ALL)
                || view == View.VISIBLE && !formKey.equals("customer")
                || (custom || List.of("product", "price").contains(formKey)) && view == View.DEPARTMENT
                || List.of("product", "price").contains(formKey) && view != View.ALL) {
            throw invalid("UNSUPPORTED_SCOPE");
        }

        List<String> permissions = pool ? List.of(formKey.equals("clue")
                ? PermissionConstants.CLUE_MANAGEMENT_POOL_READ : PermissionConstants.CUSTOMER_MANAGEMENT_POOL_READ)
                : source.permissions();
        if (permissions.stream().noneMatch(PermissionUtils::hasPermission)) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }

        BasePageRequest request = source.request().get();
        request.setViewId(view.name());
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("request", request);
        parameters.put("userId", userId);
        parameters.put("orgId", orgId);
        // 两个旧 Mapper 的绑定名不同，保留其原始契约。
        parameters.put("currentUser", userId);
        parameters.put("currentOrg", orgId);
        parameters.put("source", false);
        boolean empty = false;
        String effectiveScope;
        if (custom) {
            boolean own = customFormAccess.manageOwn(formKey, userId, orgId) || view == View.SELF;
            ((CustomFormDataPageRequest) request).setCustomFormId(formKey);
            parameters.put("manageOwn", own);
            effectiveScope = own ? "SELF" : "FORM_ALL";
        } else if (pool) {
            if (request instanceof CluePageRequest clue) {
                poolClueService.checkPoolMember(poolId, userId, orgId);
                clue.setPoolId(poolId);
            } else {
                poolCustomerService.checkPoolMember(poolId, userId, orgId);
                ((CustomerPageRequest) request).setPoolId(poolId);
            }
            parameters.put("dataPermission", null);
            effectiveScope = "POOL_MEMBER";
        } else if (List.of("product", "price").contains(formKey)) {
            effectiveScope = "ORGANIZATION";
        } else if (List.of("record", "plan").contains(formKey)) {
            DeptDataPermissionDTO clue = scope(userId, orgId, view, PermissionConstants.CLUE_MANAGEMENT_READ);
            DeptDataPermissionDTO customer = scope(userId, orgId, view, PermissionConstants.CUSTOMER_MANAGEMENT_READ);
            parameters.put("clueDataPermission", clue);
            parameters.put("customerDataPermission", customer);
            if (request instanceof PlanHomePageRequest plan) plan.setStatus("ALL");
            empty = clue.getInvisible() && customer.getInvisible();
            effectiveScope = "FOLLOW_PAGE";
        } else {
            DeptDataPermissionDTO permission = scope(userId, orgId, view, permissions.getFirst());
            parameters.put("dataPermission", permission);
            empty = permission.getInvisible();
            effectiveScope = permission.getVisible() ? "COLLABORATION" : permission.getSelf() ? "SELF"
                    : permission.getAll() ? "ORGANIZATION" : empty ? "NONE" : "DEPARTMENTS";
        }

        MappedStatement statement = sqlSessionFactory.getConfiguration().getMappedStatement(source.statementId());
        BoundSql boundSql = statement.getBoundSql(parameters);
        String description = List.of("record", "plan").contains(formKey)
                ? "与跟进统一页面一致：线索和客户跟进，分别应用各模块的数据权限"
                : "与对应业务列表一致，仅统计当前组织和当前用户在所选视图内可见的记录";
        return new Access(source, statement, boundSql, empty, orgId,
                new Scope(view.name(), effectiveScope, pool ? poolId : null, description));
    }

    private DeptDataPermissionDTO scope(String userId, String orgId, View view, String permission) {
        DeptDataPermissionDTO result = dataScopeService.getDeptDataPermission(userId, orgId, view.name(), permission);
        // 部分旧列表未处理空范围；新接口始终关闭空权限，不能把它误解成“无条件”。
        if (result == null) throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        if (!result.getAll() && !result.getSelf() && !result.getVisible() && result.getDeptIds().isEmpty()) {
            result.setInvisible(true);
        }
        return result;
    }

    static GenericException invalid(String reason) {
        return new GenericException(CrmHttpResultCode.VALIDATE_FAILED, Map.of("reason", reason));
    }

    public record Source(Class<?> entity, String statementId, Supplier<BasePageRequest> request, List<String> permissions) {
        public String table() { return EntityTableMapper.generateTableName(entity); }

        public boolean hasSubtableFields() {
            // 固定领域实体均有同包的 XxxField；从现有实体继承关系读取存储能力，避免另维护一张表结构清单。
            try {
                return BaseResourceSubField.class.isAssignableFrom(Class.forName(entity.getName() + "Field"));
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Missing field entity for " + entity.getSimpleName(), e);
            }
        }
    }

    public record Access(Source source, MappedStatement statement, BoundSql boundSql, boolean empty,
                         String orgId, Scope scope) { }
}
