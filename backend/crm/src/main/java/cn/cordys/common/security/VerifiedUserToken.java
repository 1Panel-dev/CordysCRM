package cn.cordys.common.security;

import org.apache.shiro.authc.AuthenticationToken;

/**
 * 已由服务端验证 API Key 或 SSO 授权码的用户身份。
 * 只能在外部凭据验证成功后创建，不能从请求参数或会话认证标记生成。
 */
public record VerifiedUserToken(String userId) implements AuthenticationToken {
    @Override
    public Object getPrincipal() {
        return userId;
    }

    @Override
    public Object getCredentials() {
        return userId;
    }
}
