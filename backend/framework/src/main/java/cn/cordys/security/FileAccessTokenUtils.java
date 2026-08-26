package cn.cordys.security;

import cn.cordys.common.util.CodingUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;

/**
 * 文件访问 Token 工具类
 * <p>
 * Cookie 加密的是 {@code userId} (UserDTO.id) 而非 Shiro SessionId:
 * userId 永不过期, Shiro Session 即使被 Redis 清理, 重新登录后 Cookie 仍然有效;
 * 校验时通过 Spring Session principal 索引反查是否有活跃 Session, 避免依赖会过期的 Shiro SessionId。
 * </p>
 */
public class FileAccessTokenUtils {

    private static final String COOKIE_NAME = "F_A_TOKEN";

    /**
     * 生成附件访问令牌 (加密 userId)
     */
    public static String generateToken(String userId) {
        if (StringUtils.isBlank(userId)) {
            return null;
        }
        return CodingUtils.aesEncrypt(userId, SessionUser.secret, CodingUtils.generateIv());
    }

    /**
     * 验证 Token 并检查用户登录状态 (通过 userId 反查活跃 Session)
     */
    public static boolean validateToken(HttpServletRequest request) {
        if (request == null) {
            return false;
        }

        String userId = getUserId(request);
        if (userId == null) {
            return false;
        }

        return SessionUtils.hasActiveSession(userId);
    }

    /**
     * 从 Cookie 获取并解密 userId
     */
    private static String getUserId(HttpServletRequest request) {
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
     * 设置文件访问 Cookie (加密 userId)
     */
    public static void setAccessCookie(HttpServletResponse response, String userId, boolean isSecure) {
        String token = generateToken(userId);
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
