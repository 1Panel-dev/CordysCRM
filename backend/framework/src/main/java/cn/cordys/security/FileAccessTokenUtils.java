package cn.cordys.security;

import cn.cordys.common.util.CodingUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;

/**
 * 文件访问 Token 工具类
 * <p>
 * Cookie 加密当前 Shiro SessionId，并仅允许对应的已认证 Session 访问文件。
 * </p>
 */
public class FileAccessTokenUtils {

    private static final String COOKIE_NAME = "F_A_TOKEN";
    private static final String TOKEN_PURPOSE = "F_A";

    /**
     * 生成附件访问令牌（加密 SessionId）
     */
    public static String generateToken(String sessionId) {
        if (StringUtils.isBlank(sessionId)) {
            return null;
        }

        String payload = String.join("|", TOKEN_PURPOSE, sessionId);
        return CodingUtils.encryptToken(payload, SessionUser.secret);
    }

    /**
     * 验证 Token 并检查其对应的 Session 是否已认证
     */
    public static boolean validateToken(HttpServletRequest request) {
        if (request == null) {
            return false;
        }

        String sessionId = getSessionId(request);
        if (sessionId == null) {
            return false;
        }

        return SessionUtils.hasAuthenticatedSession(sessionId);
    }

    /**
     * 从 Cookie 获取并解密 SessionId
     */
    public static String getSessionId(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return getSessionId(cookie.getValue());
            }
        }
        return null;
    }

    static String getSessionId(String token) {
        try {
            String payload = CodingUtils.decryptToken(token, SessionUser.secret);
            String[] parts = payload.split("\\|", 2);
            if (parts.length != 2 || !TOKEN_PURPOSE.equals(parts[0]) || StringUtils.isBlank(parts[1])) {
                return null;
            }
            return parts[1];
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 设置文件访问 Cookie（加密 SessionId）
     */
    public static void setAccessCookie(HttpServletResponse response, String sessionId, boolean isSecure) {
        String token = generateToken(sessionId);
        if (token == null) {
            return;
        }
        Cookie cookie = new Cookie(COOKIE_NAME, token);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(isSecure);
        cookie.setMaxAge(-1);
        response.addCookie(cookie);
    }

    /**
     * 移除文件访问 Cookie
     */
    public static void deleteAccessCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(COOKIE_NAME, "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
