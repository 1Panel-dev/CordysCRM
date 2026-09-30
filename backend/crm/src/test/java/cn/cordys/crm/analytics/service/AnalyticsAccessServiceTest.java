package cn.cordys.crm.analytics.service;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.permission.PermissionUtils;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.crm.analytics.dto.request.AnalyticsQueryRequest.View;
import cn.cordys.crm.clue.service.PoolClueService;
import cn.cordys.crm.customer.service.PoolCustomerService;
import cn.cordys.crm.form.mapper.ExtCustomFormDataMapper;
import cn.cordys.crm.form.service.CustomFormAnalyticsAccessService;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.PreparedStatement;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 真实列表 XML + 授权入口：捕捉绑定别名、视图、空权限和池/自定义表单授权回归。 */
class AnalyticsAccessServiceTest {
    private final AnalyticsAccessService service = new AnalyticsAccessService();
    private final DataScopeService scopes = mock(DataScopeService.class);
    private final CustomFormAnalyticsAccessService forms = mock(CustomFormAnalyticsAccessService.class);
    private final PoolCustomerService customerPools = mock(PoolCustomerService.class);
    private final PoolClueService cluePools = mock(PoolClueService.class);

    @BeforeEach
    void setup() throws Exception {
        SqlSessionFactory factory = mock(SqlSessionFactory.class);
        when(factory.getConfiguration()).thenReturn(configuration());
        ReflectionTestUtils.setField(service, "sqlSessionFactory", factory);
        ReflectionTestUtils.setField(service, "dataScopeService", scopes);
        ReflectionTestUtils.setField(service, "customFormAccess", forms);
        ReflectionTestUtils.setField(service, "poolCustomerService", customerPools);
        ReflectionTestUtils.setField(service, "poolClueService", cluePools);
        when(scopes.getDeptDataPermission(anyString(), anyString(), anyString(), anyString())).thenAnswer(invocation -> {
            var scope = new DeptDataPermissionDTO();
            String view = invocation.getArgument(2);
            scope.setViewId(view);
            scope.setAll(view.equals("ALL"));
            scope.setSelf(view.equals("SELF"));
            scope.setVisible(view.equals("VISIBLE"));
            if (view.equals("DEPARTMENT")) scope.getDeptIds().add("dept-a");
            return scope;
        });
    }

    static Configuration configuration() throws Exception {
        Configuration configuration = new Configuration();
        parse(configuration, "cn/cordys/common/mapper/CommonMapper.xml");
        for (var source : AnalyticsAccessService.SOURCES.values()) {
            parse(configuration, source.statementId().substring(0, source.statementId().lastIndexOf('.')).replace('.', '/') + ".xml");
        }
        parse(configuration, ExtCustomFormDataMapper.class.getName().replace('.', '/') + ".xml");
        return configuration;
    }

    private static void parse(Configuration configuration, String resource) throws Exception {
        try (var input = AnalyticsAccessServiceTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input, resource);
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
    }

    static Stream<String> modules() { return AnalyticsAccessService.SOURCES.keySet().stream().sorted(); }

    @ParameterizedTest
    @MethodSource("modules")
    void everyModuleRendersAndBindsItsOriginalList(String formKey) throws Exception {
        try (var permission = permitted()) {
            for (View view : View.values()) {
                if (view == View.VISIBLE && !formKey.equals("customer")) continue;
                if ((formKey.equals("product") || formKey.equals("price")) && view != View.ALL) continue;
                var access = service.resolve(formKey, view, null, "viewer", "org-a");
                assertFalse(access.empty());
                assertFalse(access.boundSql().getSql().contains("${"));
                assertFalse(access.boundSql().getSql().toLowerCase().contains(" limit "));
                PreparedStatement statement = mock(PreparedStatement.class);
                new DefaultParameterHandler(access.statement(), access.boundSql().getParameterObject(), access.boundSql()).setParameters(statement);
                verify(statement, atLeastOnce()).setString(anyInt(), eq("org-a"));
                // 从实体发现字段存储能力应覆盖全部模块，不允许首次请求时才发现缺类。
                assertDoesNotThrow(access.source()::hasSubtableFields);
            }
        }
    }

    @Test
    void deniesModuleAccessBeforeRenderingAnySql() {
        try (var permission = mockStatic(PermissionUtils.class)) {
            assertThrows(GenericException.class, () -> service.resolve("customer", View.ALL, null, "viewer", "org-a"));
            verifyNoInteractions(scopes);
        }
    }

    @Test
    void emptyPermissionCannotBecomeUnrestrictedCustomerQuery() {
        when(scopes.getDeptDataPermission(anyString(), anyString(), anyString(), anyString())).thenReturn(new DeptDataPermissionDTO());
        try (var permission = permitted()) {
            assertTrue(service.resolve("customer", View.ALL, null, "viewer", "org-a").empty());
            assertTrue(service.resolve("record", View.ALL, null, "viewer", "org-a").empty());
        }
    }

    @Test
    void poolPermissionAndMembershipAreBothRequired() {
        try (var permission = mockStatic(PermissionUtils.class)) {
            permission.when(() -> PermissionUtils.hasPermission(PermissionConstants.CUSTOMER_MANAGEMENT_READ)).thenReturn(true);
            assertThrows(GenericException.class, () -> service.resolve("customer", View.ALL, "pool-a", "viewer", "org-a"));
            verifyNoInteractions(customerPools);
            permission.when(() -> PermissionUtils.hasPermission(PermissionConstants.CUSTOMER_MANAGEMENT_POOL_READ)).thenReturn(true);
            service.resolve("customer", View.ALL, "pool-a", "viewer", "org-a");
            verify(customerPools).checkPoolMember("pool-a", "viewer", "org-a");
            doThrow(new GenericException("denied")).when(customerPools).checkPoolMember("foreign-pool", "viewer", "org-a");
            assertThrows(GenericException.class, () -> service.resolve("customer", View.ALL, "foreign-pool", "viewer", "org-a"));
        }
    }

    @Test
    void customFormRoleCannotBeWidenedByAllView() {
        when(forms.manageOwn("custom-a", "viewer", "org-a")).thenReturn(true);
        try (var permission = permitted()) {
            var own = service.resolve("custom-a", View.ALL, null, "viewer", "org-a");
            assertEquals("SELF", own.scope().effectiveScope());
            assertTrue(own.boundSql().getSql().contains("d.owner = ?"));
            assertTrue(own.boundSql().getSql().contains("custom_form_id = ?"));
            verify(forms).manageOwn("custom-a", "viewer", "org-a");
            doThrow(new GenericException("denied")).when(forms).manageOwn("foreign-form", "viewer", "org-a");
            assertThrows(GenericException.class, () -> service.resolve("foreign-form", View.ALL, null, "viewer", "org-a"));
        }
    }

    @Test
    void rejectsUnsupportedScopesInsteadOfIgnoringThem() {
        try (var permission = permitted()) {
            assertThrows(GenericException.class, () -> service.resolve("contract", View.VISIBLE, null, "viewer", "org-a"));
            assertThrows(GenericException.class, () -> service.resolve("customer", View.SELF, "pool-a", "viewer", "org-a"));
            assertThrows(GenericException.class, () -> service.resolve("custom-a", View.DEPARTMENT, null, "viewer", "org-a"));
            assertThrows(GenericException.class, () -> service.resolve("product", View.SELF, null, "viewer", "org-a"));
        }
    }

    static MockedStatic<PermissionUtils> permitted() {
        var permission = mockStatic(PermissionUtils.class);
        permission.when(() -> PermissionUtils.hasPermission(anyString())).thenReturn(true);
        return permission;
    }
}
