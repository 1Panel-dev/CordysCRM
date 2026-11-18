package cn.cordys.security;

import cn.cordys.common.util.CodingUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.BeanUtils;

import java.io.Serial;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Objects;

/**
 * <p>
 * 表示会话中的用户信息，继承自 {@link UserDTO}，并包含用于防止 CSRF 攻击的 token 和会话 ID。
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
public class SessionUser extends UserDTO implements Serializable {

    /**
     * 会话令牌密钥，启动时从 Redis 加载，与历史业务数据加密无关。
     */
    public static String secret;

    @Serial
    private static final long serialVersionUID = -7149638440406959033L;

    /**
     * CSRF Token，用于防止跨站请求伪造攻击。
     */
    private String csrfToken;

    /**
     * 会话 ID，表示当前会话的唯一标识。
     */
    private String sessionId;

    /**
     * 从 UserDTO 创建 SessionUser 对象，并生成 CSRF Token 和会话 ID。
     *
     * @param user      用户数据对象
     * @param sessionId 会话 ID
     * @return {@link SessionUser} 对象
     */
    public static SessionUser fromUser(UserDTO user, String sessionId) {
        // 创建 SessionUser 实例
        SessionUser sessionUser = new SessionUser();
        // 拷贝 UserDTO 的属性到 SessionUser
        BeanUtils.copyProperties(user, sessionUser);

        // 随机 IV 保证每次签发不同；用途和会话绑定防止跨令牌复用，有效期随会话。
        sessionUser.csrfToken = CodingUtils.encryptToken(String.join("|", "CSRF", user.getId(), sessionId), secret);

        // 设置 sessionId
        sessionUser.sessionId = sessionId;
        return sessionUser;
    }

    public static String getRandomAlphabetic(String userId) {
        try {
            byte[] hashedKey = MessageDigest.getInstance("SHA-256")
                    .digest(Objects.requireNonNull(userId).getBytes(StandardCharsets.UTF_8));
            return Base64.encodeBase64String(Arrays.copyOf(hashedKey, 16)); // 取 16 字节作 AES-128
        } catch (Exception e) {
            throw new RuntimeException("Error generating random alphabetic string", e);
        }
    }

}
