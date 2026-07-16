package cn.cordys.crm.integration.sso.constants;

import java.util.Arrays;
import java.util.Optional;

/**
 * OAuth 登录流程类型。
 *
 * <p>枚举值对应系统内部支持的 OAuth 登录入口，{@link #value} 是写入 {@code state} 参数前缀的稳定标识。
 * 使用显式字符串而不是枚举名称，可以避免枚举重命名意外改变前后端约定或已发出的授权请求。</p>
 */
public enum OAuthStateFlow {

    /** 企业微信扫码登录。 */
    QR_WECOM("qr-wecom"),

    /** 钉钉扫码登录。 */
    QR_DING_TALK("qr-ding-talk"),

    /** 企业微信 OAuth 登录。 */
    WECOM("wecom"),

    /** 钉钉 OAuth 登录。 */
    DING_TALK("ding-talk"),

    /** 飞书 Web 端 OAuth 登录。 */
    LARK("lark"),

    /** 飞书移动端 OAuth 登录。 */
    LARK_MOBILE("lark-mobile"),

    /** GitHub OAuth 登录。 */
    GITHUB("github");

    /** 前后端传输以及 {@code state} 前缀使用的流程标识。 */
    private final String value;

    OAuthStateFlow(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * 根据外部传入的流程标识查找受支持的 OAuth 流程。
     *
     * @param value 前端传入的流程标识
     * @return 匹配的流程；不受支持时返回空
     */
    public static Optional<OAuthStateFlow> fromValue(String value) {
        return Arrays.stream(values())
                .filter(flow -> flow.value.equals(value))
                .findFirst();
    }
}
