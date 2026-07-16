package cn.cordys.config;

import cn.cordys.security.SessionConstants;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * OAuth 会话解析边界的单元测试，确保专用 Cookie 仅在 state 签发和 SSO 回调链路生效。
 */
class OAuthAwareSessionIdResolverTest {

    private final OAuthAwareSessionIdResolver resolver = new OAuthAwareSessionIdResolver();

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
                "pre-auth-session"
        );
        String cookiePair = response.getHeader(HttpHeaders.SET_COOKIE).split(";", 2)[0];
        Cookie cookie = new Cookie(
                OAuthAwareSessionIdResolver.OAUTH_SESSION_COOKIE,
                cookiePair.split("=", 2)[1]
        );
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
                "pre-auth-session"
        );
        String cookiePair = response.getHeader(HttpHeaders.SET_COOKIE).split(";", 2)[0];
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/sso/callback/oauth/wecom");
        request.addHeader(SessionConstants.HEADER_TOKEN, "old-auth-session");
        request.setCookies(new Cookie(
                OAuthAwareSessionIdResolver.OAUTH_SESSION_COOKIE,
                cookiePair.split("=", 2)[1]
        ));

        assertEquals(List.of("pre-auth-session"), resolver.resolveSessionIds(request));
    }
}
