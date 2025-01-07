package io.cordys.common.constants;

/**
 * 角色的数据权限范围
 * @Author: jianxing
 * @CreateTime: 2025-01-07  16:42
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
    SELF;
}