package cn.cordys.crm.approval.constants;

/**
 * @author jianxing
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
