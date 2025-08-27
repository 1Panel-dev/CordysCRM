package cn.cordys.common.request;

import cn.cordys.common.util.rsa.RsaKey;
import cn.cordys.common.util.rsa.RsaUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>登录请求的 DTO 类。</p>
 * <p>包含用户名、密码以及认证信息。用户名和密码经过 RSA 解密后使用。</p>
 */
@Getter
@Setter
public class LoginRequest {

    /**
     * 用户名，不能为空，最大长度为 256。
     */
    @NotBlank(message = "{user_name_is_null}")
    @Size(max = 256, message = "{user_name_length_too_long}")
    private String username;

    /**
     * 密码，不能为空，最大长度为 256。
     */
    @NotBlank(message = "{password_is_null}")
    @Size(max = 256, message = "{password_length_too_long}")
    private String password;

    /**
     * 认证信息，可选字段。
     */
    private String authenticate;

    /**
     * 登录地，可选字段。
     */
    private String loginAddress;

    /**
     * 平台
     */
    @NotBlank(message = "{platform_is_null}")
    private String platform;

    /**
     * 获取解密后的用户名。
     * <p>如果解密失败，将返回原始的用户名。</p>
     *
     * @return 解密后的用户名
     */
    public String getUsername() {
        try {
            RsaKey rsaKey = RsaUtils.getRsaKey();
            return RsaUtils.privateDecrypt(username, rsaKey.getPrivateKey());
        } catch (Exception e) {
            // 解密失败，返回原始用户名
            return username;
        }
    }

    /**
     * 获取解密后的密码。
     * <p>如果解密失败，将返回原始的密码。</p>
     *
     * @return 解密后的密码
     */
    public String getPassword() {
        try {
            RsaKey rsaKey = RsaUtils.getRsaKey();
            return RsaUtils.privateDecrypt(password, rsaKey.getPrivateKey());
        } catch (Exception e) {
            // 解密失败，返回原始密码
            return password;
        }
    }
}
