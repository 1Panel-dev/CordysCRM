package cn.cordys.common.security;

import cn.cordys.common.util.CodingUtils;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.security.FileAccessTokenUtils;
import cn.cordys.security.SessionConstants;
import cn.cordys.security.SessionUser;
import cn.cordys.security.SessionUtils;
import cn.cordys.security.UserDTO;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.session.mgt.ServletContainerSessionManager;
import org.apache.shiro.web.subject.WebSubject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

class CsrfFilterTest {

    private final CsrfFilter filter = new CsrfFilter();
    private DefaultWebSecurityManager manager;
    private MockedStatic<CommonBeanFactory> beans;
    private String previousSecret;
    private SessionUser user;

    @BeforeEach
    void setUp() {
        previousSecret = SessionUser.secret;
        SessionUser.secret = "synthetic-secret";
        beans = mockStatic(CommonBeanFactory.class);
        beans.when(() -> CommonBeanFactory.getBean(Environment.class)).thenReturn(new MockEnvironment());
        manager = new DefaultWebSecurityManager();
        manager.setSessionManager(new ServletContainerSessionManager());
        WebSubject.Builder builder = new WebSubject.Builder(manager, new MockHttpServletRequest(),
                new MockHttpServletResponse());
        builder.principals(new SimplePrincipalCollection("user-1", "test")).authenticated(true);
        Subject subject = builder.buildWebSubject();
        ThreadContext.bind(subject);
        UserDTO account = new UserDTO();
        account.setId("user-1");
        user = SessionUser.fromUser(account, subject.getSession().getId().toString());
        SessionUtils.putUser(user);
    }

    @AfterEach
    void tearDown() {
        SessionUser.secret = previousSecret;
        ThreadContext.remove();
        manager.destroy();
        beans.close();
    }

    @Test
    void acceptsCurrentSessionTokensIncludingBeforeARefresh() {
        String originalToken = user.getCsrfToken();
        SessionUser refreshed = SessionUser.fromUser(user, user.getSessionId());
        SessionUtils.putUser(refreshed);

        assertNotEquals(originalToken, refreshed.getCsrfToken());
        assertTrue(validate(originalToken, user.getSessionId()));
        assertTrue(validate(refreshed.getCsrfToken(), user.getSessionId()));
    }

    @Test
    void acceptsOnlyTheExactLegacyTokenStoredInTheCurrentSession() {
        String originalToken = legacyToken(user.getSessionId(), "abcdef", "0");
        user.setCsrfToken(originalToken);
        SessionUtils.putUser(user);

        assertTrue(validate(originalToken, user.getSessionId()));
        String forgedToken = legacyToken(user.getSessionId(), "aaaaaa", "1");
        assertThrows(RuntimeException.class, () -> validate(forgedToken, user.getSessionId()));
        assertThrows(RuntimeException.class, () -> validate(legacyToken("other-session", "abcdef", "0"),
                "other-session"));
    }

    @Test
    void rejectsLegacyTokenWhenTheStoredSessionIdDoesNotMatchTheAuthenticatedSession() {
        String token = legacyToken("other-session", "abcdef", "0");
        user.setCsrfToken(token);
        user.setSessionId("other-session");
        SessionUtils.putUser(user);

        assertThrows(RuntimeException.class, () -> validate(token, "other-session"));
    }

    @Test
    void refreshingALegacySessionReplacesItsStoredLegacyToken() {
        String legacyToken = legacyToken(user.getSessionId(), "abcdef", "0");
        user.setCsrfToken(legacyToken);
        SessionUtils.putUser(user);
        assertTrue(validate(legacyToken, user.getSessionId()));

        SessionUser refreshed = SessionUser.fromUser(user, user.getSessionId());
        SessionUtils.putUser(refreshed);

        assertTrue(validate(refreshed.getCsrfToken(), user.getSessionId()));
        assertThrows(RuntimeException.class, () -> validate(legacyToken, user.getSessionId()));
    }

    @Test
    void rejectsAnotherUserOrSessionEvenWhenTheHeaderMatchesThatSession() {
        String otherUser = CodingUtils.encryptToken("CSRF|other-user|" + user.getSessionId(), SessionUser.secret);
        String otherSession = CodingUtils.encryptToken("CSRF|user-1|other-session", SessionUser.secret);

        assertThrows(RuntimeException.class, () -> validate(otherUser, user.getSessionId()));
        assertThrows(RuntimeException.class, () -> validate(otherSession, "other-session"));
    }

    @Test
    void rejectsFileTokensAndTokensWithAnotherPurpose() {
        String fileToken = FileAccessTokenUtils.generateToken(user.getSessionId());
        String wrongPurpose = CodingUtils.encryptToken("F_A|user-1|" + user.getSessionId(), SessionUser.secret);

        assertThrows(RuntimeException.class, () -> validate(fileToken, user.getSessionId()));
        assertThrows(RuntimeException.class, () -> validate(wrongPurpose, user.getSessionId()));
    }

    @Test
    void rejectsTokensForgedWithThePublishedKey() {
        String legacyToken = CodingUtils.aesEncrypt("user-1|aaaaaa|" + user.getSessionId() + "|0",
                "9a9rdqPlTqhpZzkq", CodingUtils.generateLegacyIv());
        String newFormatForgery = CodingUtils.encryptToken("CSRF|user-1|" + user.getSessionId(),
                "9a9rdqPlTqhpZzkq");

        assertThrows(RuntimeException.class, () -> validate(legacyToken, user.getSessionId()));
        assertThrows(RuntimeException.class, () -> validate(newFormatForgery, user.getSessionId()));
    }

    @Test
    void rejectsTamperedCiphertext() {
        byte[] bytes = Base64.getUrlDecoder().decode(user.getCsrfToken().substring(CodingUtils.TOKEN_PREFIX.length()));
        bytes[bytes.length - 1] ^= 1;
        String tampered = CodingUtils.TOKEN_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        assertThrows(RuntimeException.class, () -> validate(tampered, user.getSessionId()));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " ", "v2.", "v2.invalid!", "v2.YQ", "not-a-token" })
    void rejectsMissingOrMalformedTokens(String token) {
        assertThrows(RuntimeException.class, () -> validate(token, user.getSessionId()));
    }

    private boolean validate(String token, String headerSessionId) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/department/update");
        if (token != null) {
            request.addHeader(SessionConstants.CSRF_TOKEN, token);
        }
        request.addHeader(SessionConstants.HEADER_TOKEN, headerSessionId);
        return filter.onPreHandle(request, new MockHttpServletResponse(), null);
    }

    private String legacyToken(String sessionId, String random, String timestamp) {
        return CodingUtils.aesEncrypt("user-1|" + random + "|" + sessionId + "|" + timestamp,
                "9a9rdqPlTqhpZzkq", CodingUtils.generateLegacyIv());
    }
}
