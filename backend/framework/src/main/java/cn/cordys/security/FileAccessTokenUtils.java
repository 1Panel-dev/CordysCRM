package cn.cordys.security;

import cn.cordys.common.util.CodingUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

/**
 * 文件访问 Token 工具类
 * <p>
 * Cookie 加密当前 Shiro SessionId，并仅允许对应的已认证 Session 访问文件。
 * </p>
 */
public class FileAccessTokenUtils {

    private static final String COOKIE_NAME = "F_A_TOKEN";
    private static final String TOKEN_PURPOSE = "F_A";
    private static final int IV_LENGTH = 12;

    /**
     * 生成附件访问令牌（加密 SessionId）
     */
    public static String generateToken(String sessionId) {
        if (StringUtils.isBlank(sessionId)) {
            return null;
        }

        String payload = String.join("|", TOKEN_PURPOSE, sessionId);
        byte[] iv = CodingUtils.generateIv();
        byte[] ciphertext = Base64.decodeBase64(CodingUtils.aesEncrypt(payload, SessionUser.secret, iv));
        byte[] token = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, token, 0, iv.length);
        System.arraycopy(ciphertext, 0, token, iv.length, ciphertext.length);
        return Base64.encodeBase64URLSafeString(token);
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
    private static String getSessionId(HttpServletRequest request) {
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
            byte[] tokenBytes = Base64.decodeBase64(token);
            if (tokenBytes.length <= IV_LENGTH) {
                return null;
            }

            byte[] iv = Arrays.copyOfRange(tokenBytes, 0, IV_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(tokenBytes, IV_LENGTH, tokenBytes.length);
            String payload = CodingUtils.aesDecrypt(Base64.encodeBase64String(ciphertext), SessionUser.secret, iv);
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
