package cn.cordys.config;

import cn.cordys.security.FileAccessTokenUtils;
import cn.cordys.security.SessionConstants;
import cn.cordys.security.SessionUser;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 会话解析边界测试，确保 OAuth 和文件 Cookie 仅在各自允许的路径生效。
 */
class OAuthAwareSessionIdResolverTest {

    private final OAuthAwareSessionIdResolver resolver = new OAuthAwareSessionIdResolver();
    private String originalSecret;

    @BeforeEach
    void setUpSecret() {
        originalSecret = SessionUser.secret;
        SessionUser.secret = "0123456789abcdef";
    }

    @AfterEach
    void restoreSecret() {
        SessionUser.secret = originalSecret;
    }

    @Test
    void stateEndpointWritesHeaderAndPreAuthCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/sso/callback/oauth/state/wecom");
        MockHttpServletResponse response = new MockHttpServletResponse();

        resolver.setSessionId(request, response, "pre-auth-session");

        assertEquals("pre-auth-session", response.getHeader(SessionConstants.HEADER_TOKEN));
        assertTrue(response.getHeader(HttpHeaders.SET_COOKIE)
                .startsWith(OAuthAwareSessionIdResolver.OAUTH_SESSION_COOKIE + "="));
    }

    @Test
    void callbackResolvesSessionFromPreAuthCookie() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        resolver.setSessionId(
                new MockHttpServletRequest("GET", "/sso/callback/oauth/state/wecom"),
                response,
                "pre-auth-session");
        String cookiePair = response.getHeader(HttpHeaders.SET_COOKIE).split(";", 2)[0];
        Cookie cookie = new Cookie(
                OAuthAwareSessionIdResolver.OAUTH_SESSION_COOKIE,
                cookiePair.split("=", 2)[1]);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/sso/callback/oauth/wecom");
        request.setCookies(cookie);

        assertEquals(List.of("pre-auth-session"), resolver.resolveSessionIds(request));
    }

    @Test
    void regularApiDoesNotResolveSessionFromPreAuthCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/is-login");
        request.setCookies(new Cookie(OAuthAwareSessionIdResolver.OAUTH_SESSION_COOKIE, "pre-auth-session"));

        assertTrue(resolver.resolveSessionIds(request).isEmpty());
    }

    @Test
    void callbackPrefersPreAuthCookieOverExistingAuthHeader() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        resolver.setSessionId(
                new MockHttpServletRequest("GET", "/sso/callback/oauth/state/wecom"),
                response,
                "pre-auth-session");
        String cookiePair = response.getHeader(HttpHeaders.SET_COOKIE).split(";", 2)[0];
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/sso/callback/oauth/wecom");
        request.addHeader(SessionConstants.HEADER_TOKEN, "old-auth-session");
        request.setCookies(new Cookie(
                OAuthAwareSessionIdResolver.OAUTH_SESSION_COOKIE,
                cookiePair.split("=", 2)[1]));

        assertEquals(List.of("pre-auth-session"), resolver.resolveSessionIds(request));
    }

    @ParameterizedTest
    @ValueSource(strings = { "/attachment/preview/file-id", "/pic/preview/file-id" })
    void previewResolvesCookieSessionEvenWithConflictingHeader(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setCookies(fileCookie("file-user-session"));

        assertEquals(List.of("file-user-session"), resolver.resolveSessionIds(request));
        request.addHeader(SessionConstants.HEADER_TOKEN, "another-user-session");
        assertEquals(List.of("file-user-session"), resolver.resolveSessionIds(request));
    }

    @ParameterizedTest
    @ValueSource(strings = { "/attachment/download/file-id", "/customer/get/id", "/attachment/previewx/file-id",
            "/attachment/preview/", "/pic/preview/file-id/extra" })
    void fileCookieDoesNotAuthenticateOtherPaths(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setCookies(fileCookie("file-user-session"));

        assertTrue(resolver.resolveSessionIds(request).isEmpty());
        request.addHeader(SessionConstants.HEADER_TOKEN, "api-session");
        assertEquals(List.of("api-session"), resolver.resolveSessionIds(request));
    }

    @Test
    void invalidPreviewCookieDoesNotFallBackToAnotherUserHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/pic/preview/file-id");
        request.addHeader(SessionConstants.HEADER_TOKEN, "another-user-session");
        request.setCookies(new Cookie("F_A_TOKEN", "invalid-token"));

        assertTrue(resolver.resolveSessionIds(request).isEmpty());
    }

    @Test
    void previewResolvesCookieWithinApplicationContextPath() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/crm/attachment/preview/file-id");
        request.setContextPath("/crm");
        request.setCookies(fileCookie("file-user-session"));

        assertEquals(List.of("file-user-session"), resolver.resolveSessionIds(request));
    }

    private Cookie fileCookie(String sessionId) {
        return new Cookie("F_A_TOKEN", FileAccessTokenUtils.generateToken(sessionId));
    }
}
