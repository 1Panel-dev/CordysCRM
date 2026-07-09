package cn.cordys.security;

import cn.cordys.common.util.CodingUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;

/**
 * 文件访问 Token 工具类
 */
public class FileAccessTokenUtils {

    private static final String COOKIE_NAME = "F_A_TOKEN";

    /**
     * 生成附件访问令牌
     */
    public static String generateToken(String sessionId) {
        if (StringUtils.isBlank(sessionId)) {
            return null;
        }
        return CodingUtils.aesEncrypt(sessionId, SessionUser.secret, CodingUtils.generateIv());
    }

    /**
     * 验证 Token 并检查用户登录状态
     */
    public static boolean validateToken(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        
        String sessionId = getSessionId(request);
        if (sessionId == null) {
            return false;
        }
        
        return SessionUtils.sessionExists(sessionId);
    }

    /**
     * 从 Cookie 获取并解密 sessionId
     */
    private static String getSessionId(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                try {
                    return CodingUtils.aesDecrypt(cookie.getValue(), SessionUser.secret, CodingUtils.generateIv());
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * 设置文件访问 Cookie
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
