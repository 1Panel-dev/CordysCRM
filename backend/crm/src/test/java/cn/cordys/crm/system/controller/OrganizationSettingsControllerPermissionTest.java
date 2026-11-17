package cn.cordys.crm.system.controller;

import cn.cordys.config.ShiroConfig;
import cn.cordys.crm.integration.common.dto.ThirdConfigBaseDTO;
import cn.cordys.crm.system.service.IntegrationConfigService;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.config.Ini;
import org.apache.shiro.realm.text.IniRealm;
import org.apache.shiro.util.ThreadContext;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.session.mgt.ServletContainerSessionManager;
import org.apache.shiro.web.subject.WebSubject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class OrganizationSettingsControllerPermissionTest {
    private DefaultWebSecurityManager manager;
    private OrganizationSettingsController controller;
    private IntegrationConfigService service;

    @BeforeEach
    void setUp() {
        Ini ini = new Ini();
        ini.load("""
                [users]
                reader = synthetic-password, reader
                editor = synthetic-password, editor
                [roles]
                reader = SYSTEM_SETTING:READ
                editor = SYSTEM_SETTING:READ, SYSTEM_SETTING:UPDATE
                """);
        manager = new DefaultWebSecurityManager(new IniRealm(ini));
        manager.setSessionManager(new ServletContainerSessionManager());

        service = mock(IntegrationConfigService.class);
        OrganizationSettingsController target = new OrganizationSettingsController();
        ReflectionTestUtils.setField(target, "integrationConfigService", service);
        ProxyFactory factory = new ProxyFactory(target);
        factory.addAdvisor(new ShiroConfig().getAuthorizationAttributeSourceAdvisor(manager));
        controller = (OrganizationSettingsController) factory.getProxy();
    }

    @AfterEach
    void tearDown() {
        ThreadContext.remove();
        manager.destroy();
    }

    @Test
    void readOnlyUserCannotSaveConfiguration() {
        login("reader");

        assertThrows(UnauthorizedException.class, () -> controller.editThirdConfig(new ThirdConfigBaseDTO<>()));

        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = { "reader", "editor" })
    void userWithReadPermissionCanTestConnection(String userId) {
        login(userId);
        ThirdConfigBaseDTO<Object> request = new ThirdConfigBaseDTO<>();

        controller.validate(request);

        verify(service).testConnection(request);
    }

    private void login(String userId) {
        WebSubject subject = new WebSubject.Builder(manager, new MockHttpServletRequest(), new MockHttpServletResponse())
                .buildWebSubject();
        ThreadContext.bind(subject);
        subject.login(new UsernamePasswordToken(userId, "synthetic-password"));
    }
}
