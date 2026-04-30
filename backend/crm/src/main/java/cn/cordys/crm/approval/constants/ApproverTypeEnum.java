package cn.cordys.crm.approval.constants;

/**
 * 审批人类型枚举
 */
public enum ApproverTypeEnum {

    /** 指定成员 */
    MEMBER,
    /** 直属上级 */
    SUPERIOR,
    /** 多级上级 */
    MULTIPLE_SUPERIOR,
    /** 部门负责人 */
    DEPT_HEAD,
    /** 多级部门负责人 */
    MULTIPLE_DEPT_HEAD,
    /** 指定角色 */
    ROLE
}
