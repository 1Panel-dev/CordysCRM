package cn.cordys.crm.customer.controller;

import cn.cordys.common.constants.FormKeyConstants;
import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.dto.UserDeptDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.permission.CsPermissionAspect;
import cn.cordys.common.permission.PermissionCache;
import cn.cordys.common.permission.ResourcePermissionService;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.service.UserQueryService;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.approval.constants.ApprovalStatus;
import cn.cordys.crm.approval.constants.ApprovalTaskType;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.domain.ApprovalTask;
import cn.cordys.crm.approval.dto.StatusPermissionDTO;
import cn.cordys.crm.approval.dto.response.StatusPermissionSettingResponse;
import cn.cordys.crm.approval.service.ApprovalFlowService;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.customer.domain.CustomerCollaboration;
import cn.cordys.crm.customer.domain.CustomerContact;
import cn.cordys.crm.customer.dto.response.CustomerContactGetResponse;
import cn.cordys.crm.customer.dto.response.CustomerContactListResponse;
import cn.cordys.crm.customer.mapper.ExtCustomerContactMapper;
import cn.cordys.crm.customer.service.CustomerCollaborationService;
import cn.cordys.crm.customer.service.CustomerContactFieldService;
import cn.cordys.crm.customer.service.CustomerContactService;
import cn.cordys.crm.customer.service.CustomerResourceAccessContextProvider;
import cn.cordys.crm.opportunity.domain.Opportunity;
import cn.cordys.crm.opportunity.service.OpportunityResourceAccessContextProvider;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.crm.system.service.ModuleFormService;
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
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 真实权限切面与服务共同校验客户联系人，协作读授权不能扩大原有联系人集合或绕过组织与审批限制。
 */
class CustomerContactPermissionTest {
    private static final String READ = PermissionConstants.CUSTOMER_MANAGEMENT_READ;
    private static final String CONTACT_READ = PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ;
    private static final String USER = "reader";
    private static final String ORG = "org-example";
    private static final String CUSTOMER = "customer-example";

    @SuppressWarnings("unchecked")
    private final BaseMapper<Customer> customers = mock(BaseMapper.class);
    @SuppressWarnings("unchecked")
    private final BaseMapper<CustomerContact> contactRecords = mock(BaseMapper.class);
    @SuppressWarnings("unchecked")
    private final BaseMapper<CustomerCollaboration> collaborationMapper = mock(BaseMapper.class);
    private final CustomerResourceAccessContextProvider provider = new CustomerResourceAccessContextProvider();
    private final ExtCustomerContactMapper contacts = mock(ExtCustomerContactMapper.class);
    private final CustomerCollaborationService collaborations = mock(CustomerCollaborationService.class);
    private final BaseService base = mock(BaseService.class);
    private final UserQueryService users = mock(UserQueryService.class);
    private final DataScopeService dataScope = spy(new DataScopeService());
    private final ApprovalFlowService approvals = mock(ApprovalFlowService.class);
    private final ResourcePermissionService resourcePermissions = new ResourcePermissionService();
    private final PermissionCache permissions = mock(PermissionCache.class);
    private final CustomerContactService service = spy(new CustomerContactService());
    private final CustomerContactFieldService contactFields = mock(CustomerContactFieldService.class);
    private final ModuleFormCacheService formCache = mock(ModuleFormCacheService.class);
    private final ModuleFormService forms = mock(ModuleFormService.class);
    private final DeptDataPermissionDTO scope = new DeptDataPermissionDTO();
    private final SessionUser user = new SessionUser();
    private final Customer customer = new Customer();
    private final UserDeptDTO ownerDepartment = new UserDeptDTO("owner", "dept-a", "示例部门");
    private CustomerContactController controller;
    private ApplicationContext previousContext;
    private Subject previousSubject;

    @BeforeEach
    void preparePermissionChain() {
        previousContext = (ApplicationContext) ReflectionTestUtils.getField(CommonBeanFactory.class, "context");
        previousSubject = ThreadContext.getSubject();
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getBean(PermissionCache.class)).thenReturn(permissions);
        when(permissions.getPermissionIds(anyString(), anyString()))
                .thenReturn(Set.of(READ, PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ));
        new CommonBeanFactory().setApplicationContext(context);
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
        customer.setOwner("owner");
        when(customers.selectByPrimaryKey(CUSTOMER)).thenReturn(customer);
        when(customers.selectByIds(List.of(CUSTOMER))).thenReturn(List.of(customer));
        when(contacts.listByCustomerId(CUSTOMER)).thenReturn(List.of(
                contact("mine", USER), contact("owner", "owner"),
                contact("collaboration", "collaborator"), contact("read-only", "reader-only")));
        scope.setSelf(true);
        doReturn(scope).when(dataScope).getDeptDataPermission(anyString(), anyString(), anyString());
        ReflectionTestUtils.setField(dataScope, "userQueryService", users);
        Map<String, UserDeptDTO> departments = Map.of(
                "owner", ownerDepartment,
                USER, new UserDeptDTO(USER, "dept-a", "示例部门"),
                "collaborator", new UserDeptDTO("collaborator", "dept-b", "其他部门"));
        when(base.getUserDeptMapByUserId("owner", ORG)).thenReturn(departments.get("owner"));
        when(base.getUserDeptMapByUserIds(anyList(), eq(ORG))).thenReturn(departments);
        when(base.getUserDeptMapByUserIds(anySet(), eq(ORG))).thenReturn(departments);
        when(users.getUserDeptMapByUserIds(anyList(), eq(ORG))).thenReturn(departments);
        configureCollaboration("NONE");

        ReflectionTestUtils.setField(provider, "customerMapper", customers);
        ReflectionTestUtils.setField(provider, "collaborationMapper", collaborationMapper);
        ReflectionTestUtils.setField(resourcePermissions, "dataScopeService", dataScope);
        ReflectionTestUtils.setField(resourcePermissions, "approvalFlowService", approvals);
        ReflectionTestUtils.setField(resourcePermissions, "contextProviders", List.of(provider));
        ReflectionTestUtils.invokeMethod(resourcePermissions, "initializeProviders");
        ReflectionTestUtils.setField(service, "customerMapper", customers);
        ReflectionTestUtils.setField(service, "customerContactMapper", contactRecords);
        ReflectionTestUtils.setField(service, "dataScopeService", dataScope);
        ReflectionTestUtils.setField(service, "resourcePermissionService", resourcePermissions);
        ReflectionTestUtils.setField(service, "customerContactFieldService", contactFields);
        ReflectionTestUtils.setField(service, "moduleFormCacheService", formCache);
        ReflectionTestUtils.setField(service, "moduleFormService", forms);
        ReflectionTestUtils.setField(service, "extCustomerContactMapper", contacts);
        ReflectionTestUtils.setField(service, "customerCollaborationService", collaborations);
        ReflectionTestUtils.setField(service, "baseService", base);
        // 仅跳过字段展示组装，联系人过滤与权限判断全部执行真实代码。
        doAnswer(invocation -> invocation.getArgument(0)).when(service).buildListData(anyList(), eq(ORG));
        doReturn(Map.of()).when(service).getListOptionMap(eq(ORG), anyList());
        CustomerContactController target = new CustomerContactController();
        ReflectionTestUtils.setField(target, "customerContactService", service);
        ReflectionTestUtils.setField(target, "dataScopeService", dataScope);
        CsPermissionAspect aspect = new CsPermissionAspect();
        ReflectionTestUtils.setField(aspect, "resourcePermissionService", resourcePermissions);
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(aspect);
        controller = factory.getProxy();
    }

    @AfterEach
    void clearPermissionContext() {
        OrganizationContext.clear();
        ThreadContext.unbindSubject();
        if (previousSubject != null) {
            ThreadContext.bind(previousSubject);
        }
        new CommonBeanFactory().setApplicationContext(previousContext);
    }

    @ParameterizedTest
    @ValueSource(strings = { "SELF", "DEPARTMENT", "ALL" })
    void 仅联系人查看权限可读取本人部门或全部范围的详情(String access) {
        when(permissions.getPermissionIds(USER, ORG)).thenReturn(Set.of(CONTACT_READ));
        CustomerContact record = detail("owner", "owner");
        switch (access) {
        case "SELF" -> record.setOwner(USER);
        case "DEPARTMENT" -> {
            scope.setSelf(false);
            scope.setDeptIds(Set.of("dept-a"));
        }
        default -> scope.setAll(true);
        }
        prepareDetailDisplay();
        assertEquals(record.getId(), controller.get(record.getId()).getId());
        verifyNoInteractions(collaborationMapper);
    }

    @ParameterizedTest
    @ValueSource(strings = { "SELF", "DEPARTMENT" })
    void 仅联系人查看权限不能读取范围外负责人的详情(String access) {
        when(permissions.getPermissionIds(USER, ORG)).thenReturn(Set.of(CONTACT_READ));
        CustomerContact record = detail("collaboration", "collaborator");
        if ("DEPARTMENT".equals(access)) {
            scope.setSelf(false);
            scope.setDeptIds(Set.of("dept-a"));
        }
        assertDetailForbidden(record.getId());
    }

    @ParameterizedTest
    @ValueSource(strings = { "SELF", "ALL", "ADMIN" })
    void 本人全部范围及管理员均不能读取其他组织联系人详情(String access) {
        CustomerContact record = detail("mine", USER);
        record.setOrganizationId("foreign-org");
        scope.setAll("ALL".equals(access));
        if ("ADMIN".equals(access)) {
            user.setId(InternalUser.ADMIN.getValue());
            record.setOwner(user.getId());
        }
        assertDetailForbidden(record.getId());
        verifyNoInteractions(customers, contacts);
    }

    @Test
    void 无可用组织时拒绝联系人详情() {
        CustomerContact record = detail("mine", USER);
        OrganizationContext.clear();
        user.setOrganizationIds(Set.of());
        assertDetailForbidden(record.getId());
        verifyNoInteractions(customers, contacts);
    }

    @ParameterizedTest
    @CsvSource({ "OWNER,owner,owner", "COLLABORATION,mine,reader", "READ_ONLY,collaboration,collaborator" })
    void 仅客户查看权限保留本人客户和协作客户的联系人详情(String access, String id, String owner) {
        when(permissions.getPermissionIds(USER, ORG)).thenReturn(Set.of(READ));
        if ("OWNER".equals(access)) {
            customer.setOwner(USER);
        } else {
            configureCollaboration(access);
        }
        CustomerContact record = detail(id, owner);
        prepareDetailDisplay();
        assertEquals(record.getId(), controller.get(record.getId()).getId());
        verify(contacts).listByCustomerId(CUSTOMER);
    }

    @Test
    void 仅客户查看权限不能凭本人联系人越过所属客户权限() {
        when(permissions.getPermissionIds(USER, ORG)).thenReturn(Set.of(READ));
        CustomerContact record = detail("mine", USER);
        assertDetailForbidden(record.getId());
        verifyNoInteractions(contacts);
    }

    @Test
    void 客户负责人不能读取联系人列表已过滤的其他协作人详情() {
        when(permissions.getPermissionIds(USER, ORG)).thenReturn(Set.of(READ));
        customer.setOwner(USER);
        CustomerContact record = detail("collaboration", "collaborator");
        assertDetailForbidden(record.getId());
        verify(contacts).listByCustomerId(CUSTOMER);
    }

    @Test
    void 双查看权限在联系人范围不足时仍可通过客户联系人范围读取() {
        customer.setOwner(USER);
        CustomerContact record = detail("owner", "owner");
        prepareDetailDisplay();
        assertEquals(record.getId(), controller.get(record.getId()).getId());
        verify(dataScope).hasDataPermission(USER, ORG, record.getOwner(), CONTACT_READ);
        verify(contacts).listByCustomerId(CUSTOMER);
    }

    @Test
    void 无查看角色权限不能读取本人联系人详情() {
        when(permissions.getPermissionIds(USER, ORG)).thenReturn(Set.of());
        customer.setOwner(USER);
        CustomerContact record = detail("mine", USER);
        assertDetailForbidden(record.getId());
        verifyNoInteractions(contacts);
    }

    @Test
    void 不存在的联系人详情保持空结果() {
        assertNull(controller.get("missing"));
        verifyNoInteractions(customers, contacts, contactFields, formCache, forms);
    }

    @ParameterizedTest
    @CsvSource({ "COLLABORATION,mine", "READ_ONLY,mine|owner|collaboration|read-only" })
    void 协作客户通过权限切面且保留原联系人过滤(String type, String expected) {
        configureCollaboration(type);
        assertContacts(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = { "OWNER", "ALL", "DEPARTMENT", "DEPARTMENT_COLLABORATOR", "ADMIN" })
    void 原有负责人全部与部门范围返回结果不变(String access) {
        String expected = "mine|owner|read-only";
        switch (access) {
        case "OWNER" -> customer.setOwner(USER);
        case "ALL" -> {
            scope.setAll(true);
            expected = "mine|owner|collaboration|read-only";
        }
        case "ADMIN" -> {
            user.setId(InternalUser.ADMIN.getValue());
            expected = "mine|owner|collaboration|read-only";
        }
        default -> {
            scope.setSelf(false);
            scope.setDeptIds(Set.of("dept-a"));
            if ("DEPARTMENT_COLLABORATOR".equals(access)) {
                configureCollaboration("COLLABORATION");
                ownerDepartment.setDeptId("dept-b");
                expected = "mine";
            }
        }
        }
        assertContacts(expected);
    }

    @Test
    void 有协作与联系人查看权限仍须客户查看角色权限() {
        configureCollaboration("COLLABORATION");
        when(permissions.getPermissionIds(USER, ORG))
                .thenReturn(Set.of(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ));
        assertForbidden(() -> controller.list(CUSTOMER));
        verifyNoInteractions(collaborationMapper);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void 无协作且客户不在数据范围时拒绝(boolean self) {
        scope.setSelf(self);
        assertForbidden(() -> controller.list(CUSTOMER));
    }

    @ParameterizedTest
    @CsvSource({ "ALL,FOREIGN", "ALL,MISSING", "ADMIN,FOREIGN", "ADMIN,MISSING",
            "ALL,NULL_ORG", "ADMIN,NULL_ORG", "ALL,BLANK_ORG", "ADMIN,BLANK_ORG" })
    void 全部范围与管理员也不能读取跨组织不存在或无组织客户(String access, String invalid) {
        scope.setAll("ALL".equals(access));
        if ("ADMIN".equals(access)) {
            user.setId(InternalUser.ADMIN.getValue());
        }
        configureCollaboration("COLLABORATION");
        switch (invalid) {
        case "FOREIGN" -> customer.setOrganizationId("foreign-org");
        case "MISSING" -> when(customers.selectByPrimaryKey(CUSTOMER)).thenReturn(null);
        case "NULL_ORG" -> {
            assertForbidden(() -> service.listByCustomerId(CUSTOMER, user.getId(), null, scope));
            return;
        }
        case "BLANK_ORG" -> {
            assertForbidden(() -> service.listByCustomerId(CUSTOMER, user.getId(), " ", scope));
            return;
        }
        }
        assertForbidden(() -> controller.list(CUSTOMER));
    }

    @ParameterizedTest
    @ValueSource(strings = { "NONE", "COLLABORATION", "READ_ONLY" })
    void 普通及协作用户均保留审批状态禁止查看的校验(String type) {
        configureCollaboration(type);
        if ("NONE".equals(type)) {
            customer.setOwner(USER);
        }
        denyApprovalStatusRead();
        assertForbidden(() -> controller.list(CUSTOMER));
        verify(approvals).getStatusPermissionsByFormType(FormKeyConstants.CUSTOMER, ORG);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void 客户移入公海后旧协作关系不能代替公海权限(String owner) {
        configureCollaboration("COLLABORATION");
        customer.setOwner(owner);
        assertForbidden(() -> controller.list(CUSTOMER));
        verifyNoInteractions(collaborationMapper);
    }

    @ParameterizedTest
    @CsvSource({ "false,CUSTOMER_MANAGEMENT:UPDATE", "false,CUSTOMER_MANAGEMENT:DELETE",
            "false,CUSTOMER_MANAGEMENT:EXPORT", "false,CUSTOMER_MANAGEMENT:TRANSFER",
            "true,CUSTOMER_MANAGEMENT:READ", "true,CUSTOMER_MANAGEMENT:UPDATE",
            "true,CUSTOMER_MANAGEMENT:DELETE", "true,CUSTOMER_MANAGEMENT:EXPORT",
            "true,CUSTOMER_MANAGEMENT:TRANSFER" })
    void 协作读取不能扩大客户写操作或批量权限(boolean batch, String permission) {
        configureCollaboration("COLLABORATION");
        when(permissions.getPermissionIds(USER, ORG)).thenReturn(Set.of(permission));
        assertForbidden(() -> {
            if (batch) {
                resourcePermissions.checkBatchResourcePermission(permission, List.of(CUSTOMER),
                        FormKeyConstants.CUSTOMER, USER, ORG);
            } else {
                resourcePermissions.checkResourcePermission(permission, CUSTOMER, FormKeyConstants.CUSTOMER, USER, ORG);
            }
        });
        verifyNoInteractions(collaborationMapper);
    }

    @ParameterizedTest
    @ValueSource(strings = { READ, PermissionConstants.OPPORTUNITY_MANAGEMENT_READ })
    @SuppressWarnings("unchecked")
    void 客户协作不能扩大其他表单的数据范围(String permission) {
        configureCollaboration("COLLABORATION");
        when(permissions.getPermissionIds(USER, ORG)).thenReturn(Set.of(permission));
        BaseMapper<Opportunity> opportunities = mock(BaseMapper.class);
        Opportunity opportunity = new Opportunity();
        opportunity.setId("opportunity-example");
        opportunity.setOrganizationId(ORG);
        opportunity.setOwner("other-owner");
        when(opportunities.selectByPrimaryKey(opportunity.getId())).thenReturn(opportunity);
        OpportunityResourceAccessContextProvider opportunityProvider = new OpportunityResourceAccessContextProvider();
        ReflectionTestUtils.setField(opportunityProvider, "opportunityMapper", opportunities);
        ReflectionTestUtils.setField(resourcePermissions, "contextProviders", List.of(provider, opportunityProvider));
        ReflectionTestUtils.invokeMethod(resourcePermissions, "initializeProviders");
        assertForbidden(() -> resourcePermissions.checkResourcePermission(permission, opportunity.getId(),
                FormKeyConstants.OPPORTUNITY, USER, ORG));
        verifyNoInteractions(collaborationMapper);
    }

    @Test
    @SuppressWarnings("unchecked")
    void 有效审批待办保留跳过状态限制的读取授权() {
        configureCollaboration("COLLABORATION");
        denyApprovalStatusRead();
        BaseMapper<ApprovalTask> tasks = mock(BaseMapper.class);
        BaseMapper<ApprovalInstance> instances = mock(BaseMapper.class);
        ApprovalTask task = new ApprovalTask();
        task.setApproverId(USER);
        task.setInstanceId("instance-example");
        task.setNodeId("node-example");
        task.setStatus(ApprovalStatus.APPROVING.name());
        task.setType(ApprovalTaskType.NL.name());
        ApprovalInstance instance = new ApprovalInstance();
        instance.setResourceId(CUSTOMER);
        instance.setApprovalStatus(ApprovalStatus.APPROVING.name());
        instance.setCurrentNodeId(task.getNodeId());
        when(tasks.selectByPrimaryKey("task-example")).thenReturn(task);
        when(instances.selectByPrimaryKey("instance-example")).thenReturn(instance);
        ReflectionTestUtils.setField(resourcePermissions, "approvalTaskMapper", tasks);
        ReflectionTestUtils.setField(resourcePermissions, "approvalInstanceMapper", instances);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Approval-Task-Id", "task-example");
        RequestAttributes previous = RequestContextHolder.getRequestAttributes();
        try {
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
            assertContacts("mine");
            verify(tasks).selectByPrimaryKey("task-example");
            verifyNoInteractions(approvals);
        } finally {
            RequestContextHolder.setRequestAttributes(previous);
        }
    }

    private void denyApprovalStatusRead() {
        customer.setApprovalStatus("APPROVING");
        StatusPermissionDTO status = new StatusPermissionDTO();
        status.setApprovalStatus("APPROVING");
        status.setPermission(READ);
        status.setEnabled(false);
        StatusPermissionSettingResponse setting = new StatusPermissionSettingResponse();
        setting.setStatusPermissions(List.of(status));
        when(approvals.getStatusPermissionsByFormType(FormKeyConstants.CUSTOMER, ORG)).thenReturn(setting);
    }

    private void configureCollaboration(String type) {
        CustomerCollaboration other = collaboration("collaborator", "COLLABORATION");
        CustomerCollaboration readOnly = collaboration("reader-only", "READ_ONLY");
        doAnswer(invocation -> {
            CustomerCollaboration query = invocation.getArgument(0);
            assertEquals(CUSTOMER, query.getCustomerId(), "协作授权必须绑定当前客户");
            assertEquals(USER, query.getUserId(), "协作授权必须绑定当前用户");
            return "NONE".equals(type) ? 0L : 1L;
        }).when(collaborationMapper).countByExample(any(CustomerCollaboration.class));
        when(collaborations.selectByCustomerId(CUSTOMER)).thenReturn("NONE".equals(type)
                ? List.of(other, readOnly)
                : List.of(other, readOnly, collaboration(USER, type)));
    }

    private CustomerContact detail(String id, String owner) {
        CustomerContact record = new CustomerContact();
        record.setId(id);
        record.setCustomerId(CUSTOMER);
        record.setOwner(owner);
        record.setOrganizationId(ORG);
        when(contactRecords.selectByPrimaryKey(id)).thenReturn(record);
        return record;
    }

    private void prepareDetailDisplay() {
        when(base.setCreateUpdateOwnerUserName(any(CustomerContactGetResponse.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(contactFields.setBusinessRefFieldValue(anyList(), anyList(), anyMap()))
                .thenAnswer(invocation -> invocation.getArgument(2));
        when(formCache.getBusinessFormConfig(anyString(), anyString())).thenReturn(new ModuleFormConfigDTO());
        when(forms.getOptionMap(any(ModuleFormConfigDTO.class), anyList())).thenAnswer(invocation -> new HashMap<>());
    }

    private void assertDetailForbidden(String id) {
        prepareDetailDisplay();
        GenericException error = assertThrows(GenericException.class, () -> controller.get(id));
        assertEquals(CrmHttpResultCode.FORBIDDEN, error.getErrorCode());
        verifyNoInteractions(contactFields, formCache, forms);
    }

    private void assertContacts(String expected) {
        assertEquals(List.of(expected.split("\\|")), controller.list(CUSTOMER).getList().stream()
                .map(CustomerContactListResponse::getId).toList());
    }

    private void assertForbidden(Executable action) {
        GenericException error = assertThrows(GenericException.class, action);
        assertEquals(CrmHttpResultCode.FORBIDDEN, error.getErrorCode());
        verifyNoInteractions(contacts);
    }

    private CustomerCollaboration collaboration(String userId, String type) {
        CustomerCollaboration result = new CustomerCollaboration();
        result.setUserId(userId);
        result.setCustomerId(CUSTOMER);
        result.setCollaborationType(type);
        return result;
    }

    private CustomerContactListResponse contact(String id, String owner) {
        CustomerContactListResponse result = new CustomerContactListResponse();
        result.setId(id);
        result.setOwner(owner);
        result.setCustomerId(CUSTOMER);
        return result;
    }
}
