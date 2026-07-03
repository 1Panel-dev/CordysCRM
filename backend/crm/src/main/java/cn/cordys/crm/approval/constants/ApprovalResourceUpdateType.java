package cn.cordys.crm.approval.constants;

/**
 * @Author: jianxing
 * @CreateTime: 2026-07-03  11:24
 */
public enum ApprovalResourceUpdateType {

    /**
     * 正常更新(需要走评审切面)
     */
    NORMAL("normal"),

    /**
     * 评审时候的更新（直接更新不走切面）
     */
    APPROVAL("approval");

    private final String value;
    ApprovalResourceUpdateType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
