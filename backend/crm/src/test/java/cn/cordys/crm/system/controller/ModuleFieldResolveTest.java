package cn.cordys.crm.system.controller;

import cn.cordys.common.constants.InternalUserView;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.dto.DeptUserTreeNode;
import cn.cordys.common.dto.condition.BaseCondition;
import cn.cordys.common.dto.condition.CombineSearch;
import cn.cordys.common.dto.condition.FilterCondition;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.permission.PermissionCache;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.customer.dto.request.CustomerPageRequest;
import cn.cordys.crm.customer.dto.response.CustomerListResponse;
import cn.cordys.crm.customer.service.CustomerService;
import cn.cordys.crm.form.dto.request.CustomFormDataPageRequest;
import cn.cordys.crm.form.dto.response.CustomFormDataListResponse;
import cn.cordys.crm.form.service.CustomFormDataService;
import cn.cordys.crm.system.dto.request.FieldResolveRequest;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.crm.system.service.ModuleService;
import cn.cordys.security.SessionConstants;
import cn.cordys.security.SessionUser;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ModuleFieldResolveTest {
    private final ModuleFieldController controller = new ModuleFieldController();
    private ApplicationContext previousContext;
    private Subject previousSubject;

    @BeforeEach
    void prepareSessionAndSchema() {
        previousContext = (ApplicationContext) ReflectionTestUtils.getField(CommonBeanFactory.class, "context");
        previousSubject = ThreadContext.getSubject();
        ApplicationContext context = mock(ApplicationContext.class);
        ModuleFormCacheService forms = mock(ModuleFormCacheService.class);
        ModuleFormConfigDTO schema = new ModuleFormConfigDTO();
        schema.setFields(List.of());
        when(forms.getBusinessFormConfig(anyString(), anyString())).thenReturn(schema);
        when(context.getBean(ModuleFormCacheService.class)).thenReturn(forms);
        PermissionCache permissions = mock(PermissionCache.class);
        when(permissions.getPermissionIds("user-current", "org-current"))
                .thenReturn(Set.of(PermissionConstants.CUSTOM_FORM_READ));
        when(context.getBean(PermissionCache.class)).thenReturn(permissions);
        new CommonBeanFactory().setApplicationContext(context);
        Subject subject = mock(Subject.class);
        Session session = mock(Session.class);
        SessionUser user = new SessionUser();
        user.setId("user-current");
        user.setOrganizationIds(Set.of("org-current"));
        when(subject.getSession()).thenReturn(session);
        when(session.getAttribute(SessionConstants.ATTR_USER)).thenReturn(user);
        ThreadContext.bind(subject);
    }

    @AfterEach
    void clearContext() {
        OrganizationContext.clear();
        ThreadContext.unbindSubject();
        if (previousSubject != null) ThreadContext.bind(previousSubject);
        new CommonBeanFactory().setApplicationContext(previousContext);
    }

    @Test
    void 客户解析复用当前组织用户和数据权限并保留名称歧义() {
        OrganizationContext.setOrganizationId("org-current");
        CustomerService customers = mock(CustomerService.class);
        DataScopeService scope = mock(DataScopeService.class);
        DeptDataPermissionDTO permission = new DeptDataPermissionDTO();
        ReflectionTestUtils.setField(controller, "customerService", customers);
        ReflectionTestUtils.setField(controller, "dataScopeService", scope);
        when(scope.getDeptDataPermission("user-current", "org-current", InternalUserView.ALL.name(),
                PermissionConstants.CUSTOMER_MANAGEMENT_READ)).thenReturn(permission);
        CustomerListResponse first = new CustomerListResponse();
        first.setId("customer-a");
        first.setName("同名客户");
        CustomerListResponse second = new CustomerListResponse();
        second.setId("customer-b");
        second.setName("同名客户");
        PagerWithOption<List<CustomerListResponse>> page = new PagerWithOption<>();
        page.setList(List.of(first, second));
        page.setTotal(2);
        when(customers.sourceList(any(), eq("user-current"), eq("org-current"), same(permission)))
                .thenReturn(page);
        var result = controller.resolveBusinessId(request("CUSTOMER", "同名客户"));
        assertEquals(List.of("customer-a", "customer-b"), result.stream().map(v -> v.getIdAsString()).toList());
        ArgumentCaptor<CustomerPageRequest> requests = ArgumentCaptor.forClass(CustomerPageRequest.class);
        verify(customers, times(2)).sourceList(requests.capture(), eq("user-current"), eq("org-current"), same(permission));
        assertEquals(List.of("id", "name"), requests.getAllValues().stream()
                .map(v -> v.getFilters().getFirst().getName()).toList());
    }

    @Test
    void 动态来源保留真实表单ID和当前组织且不可见时返回空候选() {
        OrganizationContext.setOrganizationId("org-current");
        CustomFormDataService service = mock(CustomFormDataService.class);
        ReflectionTestUtils.setField(controller, "customFormDataService", service);
        PagerWithOption<List<CustomFormDataListResponse>> page = new PagerWithOption<>();
        page.setList(List.of());
        when(service.page(any(), eq("user-current"), eq("org-current"), eq(true)))
                .thenReturn(page);
        assertTrue(controller.resolveBusinessId(request("form-a", "other-org-record")).isEmpty());
        ArgumentCaptor<CustomFormDataPageRequest> requests = ArgumentCaptor.forClass(CustomFormDataPageRequest.class);
        verify(service, times(2)).page(requests.capture(), eq("user-current"), eq("org-current"), eq(true));
        assertTrue(requests.getAllValues().stream().allMatch(v -> "form-a".equals(v.getCustomFormId())));
    }

    @Test
    void 成员只从当前组织成员树解析而不把部门当成员() {
        OrganizationContext.setOrganizationId("org-current");
        ModuleService modules = mock(ModuleService.class);
        ReflectionTestUtils.setField(controller, "moduleService", modules);
        DeptUserTreeNode department = new DeptUserTreeNode();
        department.setId("department");
        department.setName("同名");
        department.setNodeType("DEPARTMENT");
        DeptUserTreeNode user = new DeptUserTreeNode();
        user.setId("member");
        user.setName("同名");
        user.setNodeType("USER");
        department.setChildren(List.of(user));
        when(modules.getDeptUserTree("org-current", false)).thenReturn(List.of(department));
        assertEquals("member", controller.resolveBusinessId(request("MEMBER", "同名")).getFirst().getId());
        assertTrue(controller.resolveBusinessId(request("MEMBER", "foreign-member")).isEmpty());
    }

    @Test
    void 不完整分页和非法来源不允许伪装成完整解析() {
        ModuleFieldController endpoint = spy(controller);
        doReturn(new Pager<List<CustomerListResponse>>(List.of(), 101, 100, 1))
                .when(endpoint).sourceCustomerPage(any());
        assertThrows(GenericException.class, () -> endpoint.resolveBusinessId(request("CUSTOMER", "客户")));
        assertThrows(GenericException.class, () -> endpoint.resolveBusinessId(request("../customer", "客户")));
    }

    @Test
    void 当前用户占位符只替换成员字段而不改同名枚举值() {
        OrganizationContext.setOrganizationId("org-current");
        FilterCondition option = new FilterCondition();
        option.setName("status");
        option.setType("SELECT");
        option.setOperator("IN");
        option.setValue(new ArrayList<>(List.of("CURRENT_USER")));
        FilterCondition member = new FilterCondition();
        member.setName("owner");
        member.setType("MEMBER");
        member.setOperator("IN");
        member.setValue(new ArrayList<>(List.of("CURRENT_USER")));
        CombineSearch search = new CombineSearch();
        search.setConditions(new ArrayList<>(List.of(option, member)));
        BaseCondition request = new BaseCondition();
        request.setCombineSearch(search);

        ConditionFilterUtils.parseCondition(request, "customer");

        assertEquals(List.of("CURRENT_USER"), request.getCombineSearch().getConditions().get(0).getValue());
        assertEquals(List.of("user-current"), request.getCombineSearch().getConditions().get(1).getValue());
    }

    private FieldResolveRequest request(String source, String keyword) {
        FieldResolveRequest request = new FieldResolveRequest();
        request.setSourceType(source);
        request.setKeywords(List.of(keyword));
        return request;
    }
}
