package cn.cordys.crm.system.controller;

import cn.cordys.common.constants.FormKeyConstants;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.dto.condition.BaseCondition;
import cn.cordys.common.dto.condition.FilterCondition;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.permission.CsPermissionAspect;
import cn.cordys.common.permission.PermissionCache;
import cn.cordys.common.permission.RelatedResourcePermissionService;
import cn.cordys.common.permission.ResourcePermissionService;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.Translator;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.customer.controller.CustomerCollaborationController;
import cn.cordys.crm.customer.controller.CustomerRelationController;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.customer.domain.CustomerCollaboration;
import cn.cordys.crm.customer.service.CustomerCollaborationService;
import cn.cordys.crm.customer.service.CustomerRelationService;
import cn.cordys.crm.customer.service.CustomerResourceAccessContextProvider;
import cn.cordys.crm.contract.dto.request.ContractPaymentRecordStatisticRequest;
import cn.cordys.crm.contract.dto.response.ContractPaymentRecordStatisticResponse;
import cn.cordys.crm.contract.dto.response.ContractStatisticResponse;
import cn.cordys.crm.contract.service.ContractPaymentRecordService;
import cn.cordys.crm.contract.service.ContractService;
import cn.cordys.crm.form.dto.request.CustomFormDataPageRequest;
import cn.cordys.crm.form.service.CustomFormDataService;
import cn.cordys.crm.follow.service.FollowUpPlanService;
import cn.cordys.crm.follow.service.FollowUpRecordService;
import cn.cordys.crm.opportunity.dto.request.OpportunityPageRequest;
import cn.cordys.crm.opportunity.dto.request.OpportunityQuotationPageRequest;
import cn.cordys.crm.opportunity.dto.request.OpportunitySearchStatisticRequest;
import cn.cordys.crm.opportunity.dto.response.OpportunityListResponse;
import cn.cordys.crm.opportunity.dto.response.OpportunitySearchStatisticResponse;
import cn.cordys.crm.opportunity.service.OpportunityService;
import cn.cordys.crm.opportunity.service.OpportunityQuotationService;
import cn.cordys.crm.order.dto.response.OrderStatisticResponse;
import cn.cordys.crm.order.service.OrderService;
import cn.cordys.crm.system.domain.ModuleField;
import cn.cordys.crm.system.dto.field.DatasourceField;
import cn.cordys.crm.system.dto.field.InputField;
import cn.cordys.crm.system.dto.form.FormDetailTab;
import cn.cordys.crm.system.dto.form.FormProp;
import cn.cordys.crm.system.dto.request.DetailTabPageRequest;
import cn.cordys.crm.system.dto.request.DetailTabQueryRequest;
import cn.cordys.crm.system.dto.request.DetailTabStatisticRequest;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.DetailTabDataService;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.crm.system.service.detailtab.CustomRelatedDetailTabDataHandler;
import cn.cordys.crm.system.service.detailtab.DetailTabQueryContext;
import cn.cordys.crm.system.service.detailtab.StandardRelatedDetailTabDataHandler;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.security.SessionConstants;
import cn.cordys.security.SessionUser;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 客户协作标签保留功能权限和固定关联条件，数据范围由客户关系决定。 */
class CustomerDetailTabPermissionTest {
    private static final String READ = PermissionConstants.CUSTOMER_MANAGEMENT_READ;
    private static final String RELATED_READ = PermissionConstants.OPPORTUNITY_MANAGEMENT_READ;
    private static final String USER = "tab-reader";
    private static final String ORG = "tab-org";
    private static final String CUSTOMER = "tab-customer";
    private static final String LINK = "customer-link";
    private static final String CUSTOM_FORM = "custom-form-example";

    @SuppressWarnings("unchecked")
    private final BaseMapper<Customer> customers = mock(BaseMapper.class);
    @SuppressWarnings("unchecked")
    private final BaseMapper<CustomerCollaboration> collaborationMapper = mock(BaseMapper.class);
    @SuppressWarnings("unchecked")
    private final BaseMapper<ModuleField> fields = mock(BaseMapper.class);
    private final CustomerRelationService relations = mock(CustomerRelationService.class);
    private final CustomerCollaborationService collaborations = mock(CustomerCollaborationService.class);
    private final OpportunityService opportunities = mock(OpportunityService.class);
    private final OpportunityQuotationService quotations = mock(OpportunityQuotationService.class);
    private final ContractService contracts = mock(ContractService.class);
    private final OrderService orderService = mock(OrderService.class);
    private final ContractPaymentRecordService paymentRecords = mock(ContractPaymentRecordService.class);
    private final CustomFormDataService customData = mock(CustomFormDataService.class);
    private final FollowUpRecordService followRecords = mock(FollowUpRecordService.class);
    private final FollowUpPlanService followPlans = mock(FollowUpPlanService.class);
    private final ModuleFormCacheService forms = mock(ModuleFormCacheService.class);
    private final PermissionCache permissions = mock(PermissionCache.class);
    private final DataScopeService dataScope = spy(new DataScopeService());
    private final ResourcePermissionService resourcePermissions = new ResourcePermissionService();
    private final RelatedResourcePermissionService relatedPermissions = mock(RelatedResourcePermissionService.class);
    private final DeptDataPermissionDTO relatedScope = new DeptDataPermissionDTO();
    private final DeptDataPermissionDTO customerTabScope = new DeptDataPermissionDTO();
    private final Set<String> granted = new HashSet<>(Set.of(READ, RELATED_READ, PermissionConstants.CUSTOM_FORM_READ));
    private final Customer customer = new Customer();
    private final CustomerCollaboration collaboration = new CustomerCollaboration();
    private CustomerRelationController relationController;
    private CustomerCollaborationController collaborationController;
    private DetailTabDataController tabController;
    private StandardRelatedDetailTabDataHandler standard;
    private ApplicationContext previousContext;
    private Subject previousSubject;
    private String previousOrg;
    private MessageSource previousMessageSource;
    private LocaleContext previousLocaleContext;

    @BeforeEach
    void 配置真实权限切面与已保存标签() {
        previousContext = (ApplicationContext) ReflectionTestUtils.getField(CommonBeanFactory.class, "context");
        previousSubject = ThreadContext.getSubject();
        previousOrg = OrganizationContext.getOrganizationId();
        previousMessageSource = (MessageSource) ReflectionTestUtils.getField(Translator.class, "messageSource");
        previousLocaleContext = LocaleContextHolder.getLocaleContext();
        ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        messages.setBasename("i18n/cordys-crm");
        messages.setDefaultEncoding("UTF-8");
        new Translator().setMessageSource(messages);
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getBean(PermissionCache.class)).thenReturn(permissions);
        when(context.getBean(ModuleFormCacheService.class)).thenReturn(forms);
        when(permissions.getPermissionIds(USER, ORG)).thenAnswer(invocation -> granted);
        new CommonBeanFactory().setApplicationContext(context);
        SessionUser user = new SessionUser();
        user.setId(USER);
        user.setOrganizationIds(Set.of(ORG));
        Subject subject = mock(Subject.class);
        Session session = mock(Session.class);
        when(subject.getSession()).thenReturn(session);
        when(session.getAttribute(SessionConstants.ATTR_USER)).thenReturn(user);
        ThreadContext.bind(subject);
        OrganizationContext.setOrganizationId(ORG);

        customer.setId(CUSTOMER);
        customer.setOrganizationId(ORG);
        customer.setOwner("another-owner");
        customer.setApprovalStatus("NONE");
        when(customers.selectByPrimaryKey(CUSTOMER)).thenReturn(customer);
        collaboration.setCustomerId(CUSTOMER);
        collaboration.setUserId(USER);
        collaboration.setCollaborationType("READ_ONLY");
        when(collaborationMapper.countByExample(any(CustomerCollaboration.class))).thenAnswer(invocation -> {
            CustomerCollaboration query = invocation.getArgument(0);
            assertEquals(CUSTOMER, query.getCustomerId());
            assertEquals(USER, query.getUserId());
            return collaboration.getCollaborationType() == null ? 0L : 1L;
        });
        DeptDataPermissionDTO parentScope = new DeptDataPermissionDTO();
        parentScope.setSelf(true);
        doReturn(parentScope).when(dataScope).getDeptDataPermission(USER, ORG, READ);
        relatedScope.setDeptIds(Set.of("related-department"));
        doReturn(relatedScope).when(dataScope).getDeptDataPermission(USER, ORG, "ALL", RELATED_READ);
        when(relatedPermissions.getCustomerTabDataPermission(CUSTOMER, USER, ORG)).thenAnswer(invocation -> {
            if (collaboration.getCollaborationType() == null && !USER.equals(customer.getOwner())) {
                return null;
            }
            boolean self = "COLLABORATION".equals(collaboration.getCollaborationType())
                    && !USER.equals(customer.getOwner());
            customerTabScope.setSelf(self);
            customerTabScope.setAll(!self);
            return customerTabScope;
        });
        CustomerResourceAccessContextProvider provider = new CustomerResourceAccessContextProvider();
        ReflectionTestUtils.setField(provider, "customerMapper", customers);
        ReflectionTestUtils.setField(provider, "collaborationMapper", collaborationMapper);
        ReflectionTestUtils.setField(resourcePermissions, "dataScopeService", dataScope);
        ReflectionTestUtils.setField(resourcePermissions, "relatedResourcePermissionService", relatedPermissions);
        ReflectionTestUtils.setField(resourcePermissions, "contextProviders", List.of(provider));
        ReflectionTestUtils.invokeMethod(resourcePermissions, "initializeProviders");

        ModuleField linkField = new ModuleField();
        linkField.setId(LINK);
        when(fields.selectByPrimaryKey(LINK)).thenReturn(linkField);
        standard = new StandardRelatedDetailTabDataHandler();
        ReflectionTestUtils.setField(standard, "resourcePermissionService", resourcePermissions);
        ReflectionTestUtils.setField(standard, "relatedResourcePermissionService", relatedPermissions);
        ReflectionTestUtils.setField(standard, "dataScopeService", dataScope);
        ReflectionTestUtils.setField(standard, "opportunityService", opportunities);
        ReflectionTestUtils.setField(standard, "opportunityQuotationService", quotations);
        ReflectionTestUtils.setField(standard, "contractService", contracts);
        ReflectionTestUtils.setField(standard, "orderService", orderService);
        ReflectionTestUtils.setField(standard, "contractPaymentRecordService", paymentRecords);
        ReflectionTestUtils.setField(standard, "followUpRecordService", followRecords);
        ReflectionTestUtils.setField(standard, "followUpPlanService", followPlans);
        ReflectionTestUtils.setField(standard, "moduleFieldBaseMapper", fields);
        CustomRelatedDetailTabDataHandler custom = new CustomRelatedDetailTabDataHandler();
        ReflectionTestUtils.setField(custom, "resourcePermissionService", resourcePermissions);
        ReflectionTestUtils.setField(custom, "relatedResourcePermissionService", relatedPermissions);
        ReflectionTestUtils.setField(custom, "customFormDataService", customData);
        ReflectionTestUtils.setField(custom, "moduleFieldBaseMapper", fields);
        DetailTabDataService tabs = new DetailTabDataService(forms, List.of(standard, custom));
        DetailTabDataController tabTarget = new DetailTabDataController();
        ReflectionTestUtils.setField(tabTarget, "detailTabDataService", tabs);
        tabController = proxy(tabTarget);
        CustomerRelationController relationTarget = new CustomerRelationController();
        ReflectionTestUtils.setField(relationTarget, "customerRelationService", relations);
        relationController = proxy(relationTarget);
        CustomerCollaborationController collaborationTarget = new CustomerCollaborationController();
        ReflectionTestUtils.setField(collaborationTarget, "customerCollaborationService", collaborations);
        collaborationController = proxy(collaborationTarget);
        savedTab("opportunity");
    }

    @AfterEach
    void 恢复会话与表单上下文() {
        OrganizationContext.clear();
        OrganizationContext.setOrganizationId(previousOrg);
        ThreadContext.unbindSubject();
        if (previousSubject != null) {
            ThreadContext.bind(previousSubject);
        }
        new CommonBeanFactory().setApplicationContext(previousContext);
        new Translator().setMessageSource(previousMessageSource);
        LocaleContextHolder.setLocaleContext(previousLocaleContext);
    }

    @ParameterizedTest
    @CsvSource({ "RELATION,READ_ONLY", "RELATION,COLLABORATION", "COLLABORATION,READ_ONLY",
            "COLLABORATION,COLLABORATION", "TAB,READ_ONLY", "TAB,COLLABORATION" })
    void 范围外协作客户可读取关系协作人和已配置标签(String entry, String type) {
        collaboration.setCollaborationType(type);
        assertDoesNotThrow(() -> invoke(entry));
        switch (entry) {
        case "RELATION" -> verify(relations).list(CUSTOMER);
        case "COLLABORATION" -> verify(collaborations).list(CUSTOMER, ORG);
        default -> verify(opportunities).list(any(), eq(USER), eq(ORG), same(customerTabScope), eq(false));
        }
    }

    @ParameterizedTest
    @CsvSource({ "RELATION,NO_RELATION", "COLLABORATION,NO_ROLE", "TAB,FOREIGN_ORG" })
    void 父客户授权失败时不能进入标签业务(String entry, String denied) {
        switch (denied) {
        case "NO_RELATION" -> collaboration.setCollaborationType(null);
        case "NO_ROLE" -> granted.remove(READ);
        default -> customer.setOrganizationId("foreign-org");
        }
        assertForbidden(() -> invoke(entry));
        verifyNoInteractions(relations, collaborations, opportunities, customData, forms);
    }

    @Test
    void 客户协作不能替代关联商机查看权限() {
        granted.remove(RELATED_READ);
        assertForbidden(() -> tabController.customerPage(CUSTOMER, pageRequest("opportunity")));
        verifyNoInteractions(opportunities);
    }

    @Test
    void 标准标签采用客户协作范围并保留固定父客户过滤() {
        PagerWithOption<List<OpportunityListResponse>> result = new PagerWithOption<>();
        when(opportunities.list(any(), eq(USER), eq(ORG), same(customerTabScope), eq(false))).thenReturn(result);
        assertSame(result, tabController.customerPage(CUSTOMER, pageRequest("opportunity")));
        ArgumentCaptor<OpportunityPageRequest> query = ArgumentCaptor.forClass(OpportunityPageRequest.class);
        verify(opportunities).list(query.capture(), eq(USER), eq(ORG), same(customerTabScope), eq(false));
        assertTrue(customerTabScope.getAll());
        assertEquals("ALL", query.getValue().getViewId());
        assertEquals(2, query.getValue().getCurrent());
        assertEquals(20, query.getValue().getPageSize());
        assertFixedRelation(query.getValue());
        verify(forms).getConfig(FormKeyConstants.CUSTOMER, ORG);
    }

    @ParameterizedTest
    @ValueSource(strings = { "OWNER", "READ_ONLY", "JOINT_FOLLOW", "COLLABORATION" })
    void 标准关联标签优先采用客户关系范围而不是角色部门范围(String access) {
        if ("OWNER".equals(access)) {
            customer.setOwner(USER);
        } else {
            collaboration.setCollaborationType(access);
        }
        when(relatedPermissions.getIsolatedOwnerIds(CUSTOMER, USER, ORG)).thenReturn(Set.of("private-owner"));
        tabController.customerPage(CUSTOMER, pageRequest("opportunity"));
        ArgumentCaptor<OpportunityPageRequest> query = ArgumentCaptor.forClass(OpportunityPageRequest.class);
        verify(opportunities).list(query.capture(), eq(USER), eq(ORG), same(customerTabScope), eq(false));
        assertEquals("COLLABORATION".equals(access), customerTabScope.getSelf());
        assertEquals(!"COLLABORATION".equals(access), customerTabScope.getAll());
        assertTrue(customerTabScope.getDeptIds().isEmpty());
        FilterCondition isolation = query.getValue().getFilters().getLast();
        assertEquals("owner", isolation.getName());
        assertEquals("NOT_IN", isolation.getOperator());
        assertEquals(List.of("private-owner"), isolation.getValue());
    }

    @ParameterizedTest
    @ValueSource(strings = { "READ_ONLY", "JOINT_FOLLOW", "COLLABORATION" })
    void 无负责人报价标签不按创建人隔离(String access) {
        collaboration.setCollaborationType(access);
        granted.add(PermissionConstants.OPPORTUNITY_QUOTATION_READ);
        savedTab("quotation");
        when(relatedPermissions.getIsolatedOwnerIds(CUSTOMER, USER, ORG)).thenReturn(Set.of("private-owner"));
        tabController.customerPage(CUSTOMER, pageRequest("quotation"));
        ArgumentCaptor<OpportunityQuotationPageRequest> query =
                ArgumentCaptor.forClass(OpportunityQuotationPageRequest.class);
        ArgumentCaptor<DeptDataPermissionDTO> scope = ArgumentCaptor.forClass(DeptDataPermissionDTO.class);
        verify(quotations).list(query.capture(), eq(ORG), eq(USER), scope.capture(), eq(false));
        assertTrue(scope.getValue().getAll());
        assertEquals(false, scope.getValue().getSelf());
        assertFixedRelation(query.getValue());
    }

    @ParameterizedTest
    @ValueSource(strings = { "opportunity", CUSTOM_FORM })
    void 非客户负责人或协作人保留关联表单角色范围(String relatedForm) {
        collaboration.setCollaborationType(null);
        DeptDataPermissionDTO parentScope = new DeptDataPermissionDTO();
        parentScope.setAll(true);
        doReturn(parentScope).when(dataScope).getDeptDataPermission(USER, ORG, READ);
        savedTab(relatedForm);
        tabController.customerPage(CUSTOMER, pageRequest(relatedForm));
        if (CUSTOM_FORM.equals(relatedForm)) {
            verify(customData).page(any(), eq(USER), eq(ORG), eq(false));
        } else {
            verify(opportunities).list(any(), eq(USER), eq(ORG), same(relatedScope), eq(false));
        }
    }

    @ParameterizedTest
    @CsvSource({ "clue,record", "clue,plan", "opportunity,record", "opportunity,plan" })
    void 非客户宿主负责人保留动态跟进的原角色范围(String formKey, String relatedForm) {
        savedTab(relatedForm);
        when(relatedPermissions.isOwner(formKey, CUSTOMER, USER, ORG)).thenReturn(true);
        String permission = "record".equals(relatedForm)
                ? PermissionConstants.FOLLOW_RECORD_READ : PermissionConstants.FOLLOW_PLAN_READ;
        relatedScope.setSelf(true);
        doReturn(relatedScope).when(dataScope).getDeptDataPermission(USER, ORG, "ALL", permission);
        FormDetailTab tab = forms.getConfig(FormKeyConstants.CUSTOMER, ORG).getFormProp().getDetailTabs().getFirst();
        standard.page(new DetailTabQueryContext(formKey, CUSTOMER, tab, pageRequest(relatedForm), USER, ORG));
        if ("record".equals(relatedForm)) {
            verify(followRecords).totalList(any(), eq(USER), eq(ORG), same(relatedScope));
        } else {
            verify(followPlans).totalList(any(), eq(USER), eq(ORG), same(relatedScope));
        }
    }

    @Test
    void 商机标签统计沿用主列表口径与固定父客户过滤() {
        OpportunitySearchStatisticResponse statistic = new OpportunitySearchStatisticResponse();
        when(opportunities.searchStatistic(any(), eq(USER), eq(ORG), same(customerTabScope))).thenReturn(statistic);
        assertSame(statistic, tabController.customerStatistic(CUSTOMER, statisticRequest("opportunity")));
        ArgumentCaptor<OpportunitySearchStatisticRequest> query =
                ArgumentCaptor.forClass(OpportunitySearchStatisticRequest.class);
        verify(opportunities).searchStatistic(query.capture(), eq(USER), eq(ORG), same(customerTabScope));
        assertEquals("ALL", query.getValue().getViewId());
        assertFixedRelation(query.getValue());
    }

    @Test
    void 合同标签统计采用客户协作范围并保留固定父客户过滤() {
        savedTab("contract");
        granted.add(PermissionConstants.CONTRACT_READ);
        DeptDataPermissionDTO contractScope = new DeptDataPermissionDTO();
        contractScope.setSelf(true);
        doReturn(contractScope).when(dataScope).getDeptDataPermission(USER, ORG, "ALL",
                PermissionConstants.CONTRACT_READ);
        ContractStatisticResponse statistic = new ContractStatisticResponse();
        doReturn(statistic).when(contracts).searchStatistic(any(), eq(USER), eq(ORG), same(customerTabScope));
        assertSame(statistic, tabController.customerStatistic(CUSTOMER, statisticRequest("contract")));
        ArgumentCaptor<BaseCondition> query = ArgumentCaptor.forClass(BaseCondition.class);
        verify(contracts).searchStatistic(query.capture(), eq(USER), eq(ORG), same(customerTabScope));
        assertEquals("ALL", query.getValue().getViewId());
        assertFixedRelation(query.getValue());
    }

    @Test
    void 客户协作不能替代关联商机金额统计权限() {
        granted.remove(RELATED_READ);
        assertForbidden(() -> tabController.customerStatistic(CUSTOMER, statisticRequest("opportunity")));
        verifyNoInteractions(opportunities);
    }

    @Test
    void 订单标签统计采用客户协作范围并保留固定父客户过滤() {
        savedTab("order");
        granted.add(PermissionConstants.ORDER_READ);
        DeptDataPermissionDTO orderScope = new DeptDataPermissionDTO();
        orderScope.setSelf(true);
        doReturn(orderScope).when(dataScope).getDeptDataPermission(USER, ORG, "ALL", PermissionConstants.ORDER_READ);
        OrderStatisticResponse statistic = new OrderStatisticResponse();
        doReturn(statistic).when(orderService).searchStatistic(any(), eq(USER), eq(ORG), same(customerTabScope));
        assertSame(statistic, tabController.customerStatistic(CUSTOMER, statisticRequest("order")));
        ArgumentCaptor<BaseCondition> query = ArgumentCaptor.forClass(BaseCondition.class);
        verify(orderService).searchStatistic(query.capture(), eq(USER), eq(ORG), same(customerTabScope));
        assertEquals("ALL", query.getValue().getViewId());
        assertFixedRelation(query.getValue());
    }

    @Test
    void 内置订单标签统计同样走标准关联处理() {
        savedTab("order", "CONTRACT_ORDER");
        granted.add(PermissionConstants.ORDER_READ);
        DeptDataPermissionDTO orderScope = new DeptDataPermissionDTO();
        orderScope.setSelf(true);
        doReturn(orderScope).when(dataScope).getDeptDataPermission(USER, ORG, "ALL", PermissionConstants.ORDER_READ);
        OrderStatisticResponse statistic = new OrderStatisticResponse();
        doReturn(statistic).when(orderService).searchStatistic(any(), eq(USER), eq(ORG), same(customerTabScope));
        assertSame(statistic, tabController.customerStatistic(CUSTOMER, statisticRequest("order")));
        ArgumentCaptor<BaseCondition> query = ArgumentCaptor.forClass(BaseCondition.class);
        verify(orderService).searchStatistic(query.capture(), eq(USER), eq(ORG), same(customerTabScope));
        assertEquals("ALL", query.getValue().getViewId());
        assertFixedRelation(query.getValue());
    }

    @Test
    void 回款记录标签统计采用客户协作范围并保留固定父客户过滤() {
        savedTab("contractPaymentRecord");
        granted.add(PermissionConstants.CONTRACT_PAYMENT_RECORD_READ);
        DeptDataPermissionDTO paymentScope = new DeptDataPermissionDTO();
        paymentScope.setSelf(true);
        doReturn(paymentScope).when(dataScope).getDeptDataPermission(USER, ORG, "ALL",
                PermissionConstants.CONTRACT_PAYMENT_RECORD_READ);
        ContractPaymentRecordStatisticResponse statistic = new ContractPaymentRecordStatisticResponse();
        doReturn(statistic).when(paymentRecords).searchStatistic(any(), eq(USER), eq(ORG), same(customerTabScope));
        assertSame(statistic, tabController.customerStatistic(CUSTOMER, statisticRequest("contractPaymentRecord")));
        ArgumentCaptor<ContractPaymentRecordStatisticRequest> query =
                ArgumentCaptor.forClass(ContractPaymentRecordStatisticRequest.class);
        verify(paymentRecords).searchStatistic(query.capture(), eq(USER), eq(ORG), same(customerTabScope));
        assertEquals("ALL", query.getValue().getViewId());
        assertNull(query.getValue().getContractId());
        assertFixedRelation(query.getValue());
    }

    @Test
    void 未配置金额统计的关联表单拒绝统计() {
        savedTab(CUSTOM_FORM);
        assertThrows(GenericException.class,
                () -> tabController.customerStatistic(CUSTOMER, statisticRequest(CUSTOM_FORM)));
        verifyNoInteractions(customData);
    }

    @Test
    void 客户协作不能替代自定义表单查看权限() {
        savedTab(CUSTOM_FORM);
        granted.remove(PermissionConstants.CUSTOM_FORM_READ);
        assertForbidden(() -> tabController.customerPage(CUSTOMER, pageRequest(CUSTOM_FORM)));
        verifyNoInteractions(customData);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void 自定义标签采用客户协作范围且不吞表单有效性错误(boolean formRoleDenied) {
        savedTab(CUSTOM_FORM);
        // 关联范围在 Handler 固定，表单有效性仍由服务校验。
        if (formRoleDenied) {
            when(customData.page(any(), eq(USER), eq(ORG), eq(false), eq(true)))
                    .thenThrow(new GenericException(CrmHttpResultCode.FORBIDDEN));
            assertForbidden(() -> tabController.customerPage(CUSTOMER, pageRequest(CUSTOM_FORM)));
        } else {
            assertDoesNotThrow(() -> tabController.customerPage(CUSTOMER, pageRequest(CUSTOM_FORM)));
        }
        ArgumentCaptor<CustomFormDataPageRequest> query = ArgumentCaptor.forClass(CustomFormDataPageRequest.class);
        verify(customData).page(query.capture(), eq(USER), eq(ORG), eq(false), eq(true));
        assertEquals(CUSTOM_FORM, query.getValue().getCustomFormId());
        assertNull(query.getValue().getViewId());
        assertFixedRelation(query.getValue());
    }

    @ParameterizedTest
    @ValueSource(strings = { "OWNER", "READ_ONLY", "JOINT_FOLLOW", "COLLABORATION" })
    void 自定义关联标签按负责人固定协作可见范围(String access) {
        if ("OWNER".equals(access)) {
            customer.setOwner(USER);
        } else {
            collaboration.setCollaborationType(access);
        }
        savedTab(CUSTOM_FORM);
        when(relatedPermissions.getIsolatedOwnerIds(CUSTOMER, USER, ORG)).thenReturn(Set.of("private-owner"));
        tabController.customerPage(CUSTOMER, pageRequest(CUSTOM_FORM));
        ArgumentCaptor<CustomFormDataPageRequest> query = ArgumentCaptor.forClass(CustomFormDataPageRequest.class);
        verify(customData).page(query.capture(), eq(USER), eq(ORG), eq(false), eq(true));
        FilterCondition isolation = query.getValue().getFilters().getLast();
        assertEquals("owner", isolation.getName());
        if ("COLLABORATION".equals(access)) {
            assertEquals("EQUALS", isolation.getOperator());
            assertEquals(USER, isolation.getValue());
        } else {
            assertEquals("NOT_IN", isolation.getOperator());
            assertEquals(List.of("private-owner"), isolation.getValue());
        }
    }

    private Object invoke(String entry) {
        return switch (entry) {
        case "RELATION" -> relationController.list(CUSTOMER);
        case "COLLABORATION" -> collaborationController.list(CUSTOMER);
        default -> tabController.customerPage(CUSTOMER, pageRequest("opportunity"));
        };
    }

    private void savedTab(String relatedForm) {
        savedTab(relatedForm, null);
    }

    private void savedTab(String relatedForm, String internalKey) {
        FormDetailTab tab = new FormDetailTab();
        tab.setEnable(true);
        tab.setInternalKey(internalKey);
        tab.setRelatedForm(new OptionDTO(relatedForm, "关联表单"));
        tab.setRelatedField(new OptionDTO(LINK, "关联客户"));
        FormProp prop = new FormProp();
        prop.setDetailTabs(List.of(tab));
        ModuleFormConfigDTO config = new ModuleFormConfigDTO();
        config.setFormProp(prop);
        DatasourceField link = new DatasourceField();
        link.setId(LINK);
        link.setType("DATA_SOURCE");
        InputField client = new InputField();
        client.setId("client-filter");
        client.setType("INPUT");
        config.setFields(List.of(link, client));
        when(forms.getConfig(FormKeyConstants.CUSTOMER, ORG)).thenReturn(config);
        when(forms.getBusinessFormConfig(relatedForm, ORG)).thenReturn(config);
    }

    private DetailTabPageRequest pageRequest(String relatedForm) {
        DetailTabPageRequest request = buildRequest(new DetailTabPageRequest(), relatedForm);
        request.setCurrent(2);
        request.setPageSize(20);
        return request;
    }

    private DetailTabStatisticRequest statisticRequest(String relatedForm) {
        return buildRequest(new DetailTabStatisticRequest(), relatedForm);
    }

    private <T extends BaseCondition & DetailTabQueryRequest> T buildRequest(T request, String relatedForm) {
        request.setRelatedFormId(relatedForm);
        request.setRelatedFieldId(LINK);
        request.setViewId("ALL");
        request.setKeyword("搜索词");
        FilterCondition filter = new FilterCondition();
        filter.setName("client-filter");
        filter.setOperator("EQUALS");
        filter.setValue("client-value");
        request.setFilters(new ArrayList<>(List.of(filter)));
        return request;
    }

    private void assertFixedRelation(BaseCondition query) {
        assertEquals("搜索词", query.getKeyword());
        assertEquals(List.of("client-filter", LINK),
                query.getFilters().stream().map(FilterCondition::getName).toList());
        assertEquals(List.of("client-value", CUSTOMER),
                query.getFilters().stream().map(FilterCondition::getValue).toList());
        assertTrue(query.getFilters().stream().allMatch(filter -> "EQUALS".equals(filter.getOperator())));
    }

    private void assertForbidden(Executable action) {
        GenericException error = assertThrows(GenericException.class, action);
        assertEquals(CrmHttpResultCode.FORBIDDEN, error.getErrorCode());
    }

    private <T> T proxy(T target) {
        CsPermissionAspect aspect = new CsPermissionAspect();
        ReflectionTestUtils.setField(aspect, "resourcePermissionService", resourcePermissions);
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(aspect);
        return factory.getProxy();
    }
}
