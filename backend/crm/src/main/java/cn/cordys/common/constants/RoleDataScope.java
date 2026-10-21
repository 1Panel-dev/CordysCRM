package cn.cordys.common.constants;

/**
 * 角色的数据权限范围
 *
 * @author jianxing
 */
public enum RoleDataScope {
    /**
     * 全部数据权限
     */
    ALL,
    /**
     * 指定部门数据权限
     */
    DEPT_CUSTOM,
    /**
     * 部门及以下数据权限
     */
    DEPT_AND_CHILD,
    /**
     * 仅本人数据权限
     */
    SELF
}
