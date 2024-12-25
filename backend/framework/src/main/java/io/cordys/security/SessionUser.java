package io.cordys.security;

import io.cordys.common.util.CodingUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

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
    public static final String secret = "9a9rdqPlTqhpZzkq";

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
     * @param user     用户数据对象
     * @param sessionId 会话 ID
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
}
