package cn.cordys.crm.approval.constants;

/**
 * 审批人方向枚举（上级/部门负责人）
 */
public enum ApproverDirectionEnum {

    /** 从下往上：从提交人所在层级往上逐级查找 */
    BOTTOM_UP,
    /** 从上往下：从最高层级往下逐级查找到提交人所在层级 */
    TOP_DOWN
}
