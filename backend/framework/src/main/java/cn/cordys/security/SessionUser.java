package cn.cordys.security;

import cn.cordys.common.util.CodingUtils;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;

import java.io.Serial;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * <p>表示会话中的用户信息，继承自 {@link UserDTO}，并包含用于防止 CSRF 攻击的 token 和会话 ID。</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class SessionUser extends UserDTO implements Serializable {

    /**
     * 加密密钥，用于生成 CSRF Token。建议从配置或环境变量中读取。
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
     *
     * @return {@link SessionUser} 对象
     */
    public static SessionUser fromUser(UserDTO user, String sessionId) {
        // 创建 SessionUser 实例
        SessionUser sessionUser = new SessionUser();
        // 拷贝 UserDTO 的属性到 SessionUser
        BeanUtils.copyProperties(user, sessionUser);

        // 构建用于生成 CSRF Token 的信息
        List<String> infos = Arrays.asList(
                user.getId(),
                RandomStringUtils.secure().nextAlphabetic(6),  // 随机字符串增加多样性
                sessionId,
                String.valueOf(System.currentTimeMillis()) // 当前时间戳
        );

        try {
            // 使用 AES 加密生成 CSRF Token
            sessionUser.csrfToken = CodingUtils.aesEncrypt(StringUtils.join(infos, "|"), secret, CodingUtils.generateIv());
        } catch (Exception e) {
            // 异常处理：加密失败时可以记录日志或者返回默认值
            sessionUser.csrfToken = StringUtils.EMPTY;
        }

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
