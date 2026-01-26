package cn.cordys.common.constants;

/**
 * 部门来源类型
 */
public enum ThirdConfigTypeConstants {

    /**
     * 本地
     */
    INTERNAL,
    /**
     * 企业微信
     */
    WECOM,
    /**
     * 钉钉
     */
    DINGTALK,
    /**
     * 飞书
     */
    LARK,
    /**
     * DE
     */
    DE,
    /**
     * SQLBOT
     */
    SQLBOT,
    /**
     * maxKB
     */
    MAXKB,
    /**
     * tender
     */
    TENDER,
    /**
     * 企查查
     */
    QCC;

    public static ThirdConfigTypeConstants fromString(String type) {
        try {
            return ThirdConfigTypeConstants.valueOf(type.toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }

}