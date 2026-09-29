package cn.cordys.common.security;

import cn.cordys.common.response.handler.RestControllerExceptionHandler;
import cn.cordys.common.security.realm.LocalRealm;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.Translator;
import cn.cordys.common.util.rsa.RsaUtils;
import cn.cordys.config.ShiroConfig;
import cn.cordys.crm.system.controller.LoginController;
import cn.cordys.crm.system.service.UserLoginService;
import cn.cordys.security.SessionConstants;
import cn.cordys.security.SessionUser;
import cn.cordys.security.UserDTO;
import jakarta.servlet.Filter;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.session.mgt.ServletContainerSessionManager;
import org.apache.shiro.web.subject.WebSubject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 使用真实 Shiro 过滤链和 Realm，只替换用户存储与外部 API Key 验证。 */
class AuthenticationSecurityTest {
    private UserLoginService users;
    private DefaultWebSecurityManager manager;
    private MockMvc mvc;
    private MockedStatic<CommonBeanFactory> beans;
    private MockedStatic<Translator> translator;
    private String previousSecret;

    @BeforeEach
    void setUp() throws Exception {
        previousSecret = SessionUser.secret;
        SessionUser.secret = "synthetic-secret";
        beans = mockStatic(CommonBeanFactory.class);
        translator = mockStatic(Translator.class, invocation -> invocation.getArgument(0));
        users = mock(UserLoginService.class);
        UserDTO admin = new UserDTO();
        admin.setId("admin");
        admin.setName("Synthetic administrator");
        when(users.authenticateUser("admin")).thenReturn(admin);
        doCallRealMethod().when(users).login(any());

        LocalRealm realm = new LocalRealm();
        ReflectionTestUtils.setField(realm, "userLoginService", users);
        manager = new DefaultWebSecurityManager(realm);
        manager.setSessionManager(new ServletContainerSessionManager());
        mvc = createMvc();
    }

    private MockMvc createMvc() throws Exception {
        ShiroConfig config = new ShiroConfig();
        Filter shiro = (Filter) config.shiroFilterFactoryBean(manager).getObject();
        LoginController controller = new LoginController();
        ReflectionTestUtils.setField(controller, "userLoginService", users);
        return MockMvcBuilders.standaloneSetup(controller, new TestEndpoints())
                .setControllerAdvice(new RestControllerExceptionHandler()).addFilters(shiro).build();
    }

    @AfterEach
    void tearDown() {
        SessionUser.secret = previousSecret;
        ThreadContext.remove();
        manager.destroy();
        translator.close();
        beans.close();
    }

    @Test
    void anonymousRootSubmissionDoesNotCreateAnAuthenticatedSession() throws Exception {
        MockHttpSession session = new MockHttpSession();
        MvcResult result = mvc.perform(post("/").session(session)
                .param("username", "admin").param("password", "synthetic-wrong-password")).andReturn();

        // 即使客户端复用失败请求的会话，也不能获取管理员身份或受保护资源。
        mvc.perform(get("/is-login").session(session)).andExpect(content().string(""));
        mvc.perform(get("/department/tree").session(session)).andExpect(status().isUnauthorized());
        assertNull(session.getAttribute(SessionConstants.ATTR_USER));
        assertEquals(401, result.getResponse().getStatus());
        assertNull(result.getResponse().getRedirectedUrl());
        verify(users, never()).authenticateUser(anyString());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"LOCAL", "QR_CODE", "WECOM_OAUTH2"})
    void passwordTokensAlwaysRequireThePasswordRegardlessOfSessionMarker(String marker) {
        Subject subject = bindSubject();
        if (marker != null) {
            subject.getSession().setAttribute("authenticate", marker);
        }

        assertThrows(IncorrectCredentialsException.class,
                () -> subject.login(new UsernamePasswordToken("admin", "synthetic-wrong-password")));
        assertFalse(subject.isAuthenticated());
        assertNull(subject.getSession().getAttribute(SessionConstants.ATTR_USER));
        verify(users).checkUserPassword("admin", "synthetic-wrong-password");
    }

    @Test
    void correctPasswordStillAuthenticatesWithoutASessionMarker() {
        when(users.checkUserPassword("admin", "synthetic-correct-password")).thenReturn(true);
        Subject subject = bindSubject();

        subject.login(new UsernamePasswordToken("admin", "synthetic-correct-password"));

        assertTrue(subject.isAuthenticated());
        assertEquals("admin", subject.getPrincipal());
    }

    @Test
    void loginEndpointRejectsWrongPasswordEvenWithAnExternalAuthenticationField() throws Exception {
        String key = RsaUtils.getRsaKey().getPublicKey();
        String username = RsaUtils.publicEncrypt("admin", key);
        String password = RsaUtils.publicEncrypt("synthetic-wrong-password", key);
        MockHttpSession session = new MockHttpSession();

        mvc.perform(post("/login").session(session).contentType("application/json").content("""
                {"username":"%s","password":"%s","platform":"web","authenticate":"QR_CODE"}
                """.formatted(username, password))).andExpect(status().isUnauthorized());
        assertNull(session.getAttribute(SessionConstants.ATTR_USER));
        verify(users).checkUserPassword("admin", "synthetic-wrong-password");
    }

    @Test
    void rootPageStillLoadsWithoutAuthentication() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(content().string("synthetic front page"));
        verifyNoInteractions(users);
    }

    @Test
    void xpackDocumentationDoesNotAuthenticateOrExposeOtherResources() throws Exception {
        Filter preApiKey = (request, response, chain) -> chain.doFilter(request, response);
        beans.when(CommonBeanFactory::getFilter).thenReturn(preApiKey);
        mvc = createMvc();
        MockHttpSession session = new MockHttpSession();

        for (String path : new String[]{"/swagger-ui/index.html", "/v3/api-docs", "/v3/api-docs/account"}) {
            mvc.perform(get(path).session(session).header("X-MCP", String.valueOf(System.currentTimeMillis())))
                    .andExpect(status().isOk()).andExpect(content().string("synthetic API documentation"));
        }
        mvc.perform(get("/is-login").session(session)).andExpect(content().string(""));
        mvc.perform(get("/department/tree").session(session)).andExpect(status().isUnauthorized());
        mvc.perform(get("/v3/api-docs-forged").session(session)).andExpect(status().isUnauthorized());
        assertNull(session.getAttribute(SessionConstants.ATTR_USER));
        verifyNoInteractions(users);
    }

    @Test
    void verifiedApiKeyWorksEvenAfterAFailedLocalLoginAndDoesNotPersistTheSession() throws Exception {
        beans.when(() -> CommonBeanFactory.getUser(any())).thenReturn("admin");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticate", "LOCAL");

        mvc.perform(get("/language").session(session).header("Authorization", "synthetic-key:synthetic-signature"))
                .andExpect(status().isOk()).andExpect(content().string("admin"));

        verify(users).authenticateUser("admin");
        verify(users, never()).checkUserPassword(anyString(), anyString());
        assertTrue(session.isInvalid());
    }

    @Test
    void apiKeyHeaderAloneCannotAuthenticate() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(get("/language").session(session).header("Authorization", "synthetic-key:invalid-signature"))
                .andExpect(status().isUnauthorized());

        assertNull(session.getAttribute(SessionConstants.ATTR_USER));
        verifyNoInteractions(users);
        beans.verify(() -> CommonBeanFactory.getUser(any()));
    }

    @Test
    void verifiedSsoIdentityCanAuthenticateRegardlessOfSessionMarker() {
        Subject subject = bindSubject();
        subject.getSession().setAttribute("authenticate", "LOCAL");

        subject.login(new VerifiedUserToken("admin"));

        assertTrue(subject.isAuthenticated());
        assertEquals("admin", subject.getPrincipal());
        SessionUser user = (SessionUser) subject.getSession().getAttribute(SessionConstants.ATTR_USER);
        assertEquals("admin", user.getId());
        verify(users, never()).checkUserPassword(anyString(), anyString());
    }

    @Test
    void unknownUsersAndUnsupportedTokensCannotAuthenticate() {
        Subject subject = bindSubject();
        assertThrows(AuthenticationException.class,
                () -> subject.login(new UsernamePasswordToken("synthetic-missing-user", "synthetic-password")));
        assertFalse(subject.isAuthenticated());
        assertNull(subject.getSession().getAttribute(SessionConstants.ATTR_USER));

        assertThrows(AuthenticationException.class,
                () -> subject.login(new VerifiedUserToken("synthetic-missing-user")));
        assertFalse(subject.isAuthenticated());
        assertNull(subject.getSession().getAttribute(SessionConstants.ATTR_USER));

        AuthenticationToken unsupported = mock(AuthenticationToken.class);
        assertThrows(AuthenticationException.class, () -> subject.login(unsupported));
        assertFalse(subject.isAuthenticated());
        verify(users, never()).checkUserPassword(anyString(), anyString());
    }

    private Subject bindSubject() {
        Subject subject = new WebSubject.Builder(manager, new MockHttpServletRequest(), new MockHttpServletResponse())
                .buildWebSubject();
        ThreadContext.bind(subject);
        return subject;
    }

    @RestController
    static class TestEndpoints {
        @GetMapping({"/swagger-ui/index.html", "/v3/api-docs", "/v3/api-docs/account"})
        String apiDocumentation() {
            return "synthetic API documentation";
        }

        @GetMapping("/")
        String index() {
            return "synthetic front page";
        }

        @GetMapping({"/language", "/department/tree"})
        String protectedResource() {
            return org.apache.shiro.SecurityUtils.getSubject().getPrincipal().toString();
        }
    }
}
