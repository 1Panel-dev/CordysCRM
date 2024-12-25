package io.cordys.common.constants;

/**
 * 用户来源类型枚举类，用于标识用户的来源。
 * <p>
 * 此枚举类定义了不同的用户来源类型，包括本地、LDAP、CAS、OIDC、OAuth2 和二维码。
 * </p>
 */
public enum UserSource {

    /**
     * 本地用户来源，表示用户通过本地系统注册和登录。
     */
    LOCAL,

    /**
     * LDAP 用户来源，表示用户通过 LDAP（轻量目录访问协议）系统认证。
     */
    LDAP,

    /**
     * CAS 用户来源，表示用户通过 CAS（中央认证服务）认证。
     */
    CAS,

    /**
     * OIDC 用户来源，表示用户通过 OIDC（开放ID连接）认证。
     */
    OIDC,

    /**
     * OAUTH2 用户来源，表示用户通过 OAUTH2 授权框架认证。
     */
    OAUTH2,

    /**
     * 二维码用户来源，表示用户通过扫描二维码登录。
     */
    QR_CODE;
}
