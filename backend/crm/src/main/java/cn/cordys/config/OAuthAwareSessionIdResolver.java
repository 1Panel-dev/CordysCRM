package cn.cordys.config;

import cn.cordys.security.FileAccessTokenUtils;
import cn.cordys.security.SessionConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.session.web.http.CookieHttpSessionIdResolver;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.session.web.http.HeaderHttpSessionIdResolver;
import org.springframework.session.web.http.HttpSessionIdResolver;

import java.util.List;

/**
 * 为 OAuth 登录链路补充 Session Cookie 支持的会话 ID 解析器。
 * <p>
 * 系统常规接口仍使用 {@code X-AUTH-TOKEN} 请求头识别会话。OAuth 授权跳转由第三方平台发起，回调请求无法携带
 * 自定义请求头，因此签发 state 与校验回调可能落入不同 Session。本解析器在 state 签发接口额外写入专用 Cookie，
 * 并且只在 {@code /sso/callback/**} 范围读取该 Cookie，使回调能够恢复原 Session。
 * </p>
 * <p>
 * 文件预览仅从文件访问 Cookie 恢复会话，以便继续校验附件所属业务数据的权限。
 * 两类 Cookie 均不参与普通业务接口的身份认证。
 * </p>
 */
public class OAuthAwareSessionIdResolver implements HttpSessionIdResolver {

    /** OAuth 授权前会话使用的专用 Cookie 名称。 */
    static final String OAUTH_SESSION_COOKIE = "CORDYS_OAUTH_SESSION";

    /** SSO 回调接口根路径，仅该路径范围允许从专用 Cookie 恢复会话。 */
    private static final String SSO_CALLBACK_PATH = "/sso/callback";

    /** OAuth state 签发路径，仅该路径范围需要写入专用 Cookie。 */
    private static final String OAUTH_STATE_PATH = "/sso/callback/oauth/state/";

    /** 常规接口沿用的请求头会话解析器。 */
    private final HeaderHttpSessionIdResolver headerResolver = new HeaderHttpSessionIdResolver(
            SessionConstants.HEADER_TOKEN);

    /** 仅服务于 OAuth 授权跳转的 Cookie 会话解析器。 */
    private final CookieHttpSessionIdResolver cookieResolver = createCookieResolver();

    /**
     * 解析当前请求的 Session ID。
     * <p>
     * SSO 回调优先使用授权前写入的 Cookie，防止浏览器中残留的旧登录请求头覆盖本次 OAuth 会话；
     * 文件预览使用文件 Cookie 对应的会话，其有效性继续由文件访问过滤器校验；其他请求只读取认证请求头。
     * </p>
     */
    @Override
    public List<String> resolveSessionIds(HttpServletRequest request) {
        if (isFilePreviewRequest(request)) {
            // 认证和对象授权使用同一个 Cookie 会话，避免与请求头中的其他用户身份混用。
            String sessionId = FileAccessTokenUtils.getSessionId(request);
            return sessionId == null ? List.of() : List.of(sessionId);
        }
        if (isSsoCallbackRequest(request)) {
            List<String> oauthSessionIds = cookieResolver.resolveSessionIds(request);
            if (!oauthSessionIds.isEmpty()) {
                return oauthSessionIds;
            }
        }
        return headerResolver.resolveSessionIds(request);
    }

    /**
     * 向响应写入 Session ID。
     * <p>
     * 所有接口保持原有响应头行为，仅 state 签发接口额外写入 OAuth 专用 Cookie。
     * </p>
     */
    @Override
    public void setSessionId(HttpServletRequest request, HttpServletResponse response, String sessionId) {
        headerResolver.setSessionId(request, response, sessionId);
        if (getRequestPath(request).startsWith(OAUTH_STATE_PATH)) {
            cookieResolver.setSessionId(request, response, sessionId);
        }
    }

    /** 清理常规会话标识，并在 SSO 回调链路同步清理 OAuth 专用 Cookie。 */
    @Override
    public void expireSession(HttpServletRequest request, HttpServletResponse response) {
        headerResolver.expireSession(request, response);
        if (isSsoCallbackRequest(request)) {
            cookieResolver.expireSession(request, response);
        }
    }

    /** 判断请求是否严格位于 SSO 回调路径范围，避免相似路径误用 Cookie。 */
    private boolean isSsoCallbackRequest(HttpServletRequest request) {
        String requestPath = getRequestPath(request);
        return requestPath.equals(SSO_CALLBACK_PATH) || requestPath.startsWith(SSO_CALLBACK_PATH + "/");
    }

    private boolean isFilePreviewRequest(HttpServletRequest request) {
        return ("GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod()))
                && getRequestPath(request).matches("/(attachment|pic)/preview/[^/]+");
    }

    /** 获取去除应用 Context Path 后的请求路径。 */
    private String getRequestPath(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    /**
     * 创建 OAuth 专用 Cookie 解析器。
     * <p>
     * {@code HttpOnly} 禁止前端脚本读取会话标识；{@code SameSite=Lax} 允许用户从 OAuth 服务商顶层跳转回来时
     * 携带 Cookie，同时限制多数跨站子请求自动携带 Cookie。
     * </p>
     */
    private CookieHttpSessionIdResolver createCookieResolver() {
        DefaultCookieSerializer cookieSerializer = new DefaultCookieSerializer();
        cookieSerializer.setCookieName(OAUTH_SESSION_COOKIE);
        cookieSerializer.setCookiePath("/");
        cookieSerializer.setUseHttpOnlyCookie(true);
        cookieSerializer.setSameSite("Lax");

        CookieHttpSessionIdResolver resolver = new CookieHttpSessionIdResolver();
        resolver.setCookieSerializer(cookieSerializer);
        return resolver;
    }
}
