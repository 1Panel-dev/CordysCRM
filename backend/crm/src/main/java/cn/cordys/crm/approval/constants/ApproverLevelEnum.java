package cn.cordys.crm.approval.constants;

/**
 * @Author: jianxing
 * @CreateTime: 2026-05-09  17:04
 */
public enum ApproverLevelEnum {
    FIRST_LEVEL("1"),
    SECOND_LEVEL("2"),
    THIRD_LEVEL("3"),
    FOURTH_LEVEL("4"),
    FIFTH_LEVEL("5"),
    SIXTH_LEVEL("6"),
    SEVENTH_LEVEL("7"),
    EIGHTH_LEVEL("8"),
    NINTH_LEVEL("9"),
    TENTH_LEVEL("10");

    private final String value;

    ApproverLevelEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
