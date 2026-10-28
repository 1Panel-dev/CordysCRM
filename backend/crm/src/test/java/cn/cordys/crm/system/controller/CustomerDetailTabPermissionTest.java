package cn.cordys.crm.system.controller;

import cn.cordys.common.constants.FormKeyConstants;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.BasePageRequest;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.dto.condition.FilterCondition;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.permission.CsPermissionAspect;
import cn.cordys.common.permission.PermissionCache;
import cn.cordys.common.permission.ResourcePermissionService;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.customer.controller.CustomerCollaborationController;
import cn.cordys.crm.customer.controller.CustomerRelationController;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.customer.domain.CustomerCollaboration;
import cn.cordys.crm.customer.service.CustomerCollaborationService;
import cn.cordys.crm.customer.service.CustomerRelationService;
import cn.cordys.crm.customer.service.CustomerResourceAccessContextProvider;
import cn.cordys.crm.form.dto.request.CustomFormDataPageRequest;
import cn.cordys.crm.form.service.CustomFormDataService;
import cn.cordys.crm.opportunity.dto.request.OpportunityPageRequest;
import cn.cordys.crm.opportunity.dto.response.OpportunityListResponse;
import cn.cordys.crm.opportunity.service.OpportunityService;
import cn.cordys.crm.system.domain.ModuleField;
import cn.cordys.crm.system.dto.field.DatasourceField;
import cn.cordys.crm.system.dto.field.InputField;
import cn.cordys.crm.system.dto.form.FormDetailTab;
import cn.cordys.crm.system.dto.form.FormProp;
import cn.cordys.crm.system.dto.request.DetailTabPageRequest;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.DetailTabDataService;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.crm.system.service.detailtab.CustomRelatedDetailTabDataHandler;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

/** 客户协作读取穿过真实父权限切面后，关联模块仍独立校验自己的权限和固定关联条件。 */
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
    private final CustomFormDataService customData = mock(CustomFormDataService.class);
    private final ModuleFormCacheService forms = mock(ModuleFormCacheService.class);
    private final PermissionCache permissions = mock(PermissionCache.class);
    private final DataScopeService dataScope = spy(new DataScopeService());
    private final ResourcePermissionService resourcePermissions = new ResourcePermissionService();
    private final DeptDataPermissionDTO relatedScope = new DeptDataPermissionDTO();
    private final Set<String> granted = new HashSet<>(Set.of(READ, RELATED_READ, PermissionConstants.CUSTOM_FORM_READ));
    private final Customer customer = new Customer();
    private final CustomerCollaboration collaboration = new CustomerCollaboration();
    private CustomerRelationController relationController;
    private CustomerCollaborationController collaborationController;
    private DetailTabDataController tabController;
    private ApplicationContext previousContext;
    private Subject previousSubject;
    private String previousOrg;

    @BeforeEach
    void 配置真实权限切面与已保存标签() {
        previousContext = (ApplicationContext) ReflectionTestUtils.getField(CommonBeanFactory.class, "context");
        previousSubject = ThreadContext.getSubject();
        previousOrg = OrganizationContext.getOrganizationId();
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
        CustomerResourceAccessContextProvider provider = new CustomerResourceAccessContextProvider();
        ReflectionTestUtils.setField(provider, "customerMapper", customers);
        ReflectionTestUtils.setField(provider, "collaborationMapper", collaborationMapper);
        ReflectionTestUtils.setField(resourcePermissions, "dataScopeService", dataScope);
        ReflectionTestUtils.setField(resourcePermissions, "contextProviders", List.of(provider));
        ReflectionTestUtils.invokeMethod(resourcePermissions, "initializeProviders");

        ModuleField linkField = new ModuleField();
        linkField.setId(LINK);
        when(fields.selectByPrimaryKey(LINK)).thenReturn(linkField);
        StandardRelatedDetailTabDataHandler standard = new StandardRelatedDetailTabDataHandler();
        ReflectionTestUtils.setField(standard, "resourcePermissionService", resourcePermissions);
        ReflectionTestUtils.setField(standard, "dataScopeService", dataScope);
        ReflectionTestUtils.setField(standard, "opportunityService", opportunities);
        ReflectionTestUtils.setField(standard, "moduleFieldBaseMapper", fields);
        CustomRelatedDetailTabDataHandler custom = new CustomRelatedDetailTabDataHandler();
        ReflectionTestUtils.setField(custom, "resourcePermissionService", resourcePermissions);
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
        default -> verify(opportunities).list(any(), eq(USER), eq(ORG), same(relatedScope), eq(false));
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
        assertForbidden(() -> tabController.customerPage(CUSTOMER, request("opportunity")));
        verifyNoInteractions(opportunities);
    }

    @Test
    void 标准标签保留关联模块范围与固定父客户过滤() {
        PagerWithOption<List<OpportunityListResponse>> result = new PagerWithOption<>();
        when(opportunities.list(any(), eq(USER), eq(ORG), same(relatedScope), eq(false))).thenReturn(result);
        assertSame(result, tabController.customerPage(CUSTOMER, request("opportunity")));
        ArgumentCaptor<OpportunityPageRequest> query = ArgumentCaptor.forClass(OpportunityPageRequest.class);
        verify(opportunities).list(query.capture(), eq(USER), eq(ORG), same(relatedScope), eq(false));
        assertEquals("ALL", query.getValue().getViewId());
        assertFixedRelation(query.getValue());
        verify(forms).getConfig(FormKeyConstants.CUSTOMER, ORG);
    }

    @Test
    void 客户协作不能替代自定义表单查看权限() {
        savedTab(CUSTOM_FORM);
        granted.remove(PermissionConstants.CUSTOM_FORM_READ);
        assertForbidden(() -> tabController.customerPage(CUSTOMER, request(CUSTOM_FORM)));
        verifyNoInteractions(customData);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void 自定义标签仍委派表单角色校验且不吞权限错误(boolean formRoleDenied) {
        savedTab(CUSTOM_FORM);
        // 此处仅验证真实 Handler 的委派契约，具体表单角色算法由其自身测试覆盖。
        if (formRoleDenied) {
            when(customData.page(any(), eq(USER), eq(ORG), eq(false)))
                    .thenThrow(new GenericException(CrmHttpResultCode.FORBIDDEN));
            assertForbidden(() -> tabController.customerPage(CUSTOMER, request(CUSTOM_FORM)));
        } else {
            assertDoesNotThrow(() -> tabController.customerPage(CUSTOMER, request(CUSTOM_FORM)));
        }
        ArgumentCaptor<CustomFormDataPageRequest> query = ArgumentCaptor.forClass(CustomFormDataPageRequest.class);
        verify(customData).page(query.capture(), eq(USER), eq(ORG), eq(false));
        assertEquals(CUSTOM_FORM, query.getValue().getCustomFormId());
        assertNull(query.getValue().getViewId());
        assertFixedRelation(query.getValue());
    }

    private Object invoke(String entry) {
        return switch (entry) {
        case "RELATION" -> relationController.list(CUSTOMER);
        case "COLLABORATION" -> collaborationController.list(CUSTOMER);
        default -> tabController.customerPage(CUSTOMER, request("opportunity"));
        };
    }

    private void savedTab(String relatedForm) {
        FormDetailTab tab = new FormDetailTab();
        tab.setEnable(true);
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

    private DetailTabPageRequest request(String relatedForm) {
        DetailTabPageRequest request = new DetailTabPageRequest();
        request.setRelatedFormId(relatedForm);
        request.setRelatedFieldId(LINK);
        request.setCurrent(2);
        request.setPageSize(20);
        request.setViewId("ALL");
        request.setKeyword("搜索词");
        FilterCondition filter = new FilterCondition();
        filter.setName("client-filter");
        filter.setOperator("EQUALS");
        filter.setValue("client-value");
        request.setFilters(new ArrayList<>(List.of(filter)));
        return request;
    }

    private void assertFixedRelation(BasePageRequest query) {
        assertEquals(2, query.getCurrent());
        assertEquals(20, query.getPageSize());
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
