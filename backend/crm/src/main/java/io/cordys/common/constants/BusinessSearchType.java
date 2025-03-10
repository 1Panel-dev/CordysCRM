package io.cordys.common.constants;

import org.apache.commons.lang3.StringUtils;

/**
 *
 * 客户,线索,商机等业务数据的搜索类型
 * @author jianxing
 */
public enum  BusinessSearchType {
    /**
     * 全部数据
     */
    ALL,
    /**
     * 负责人是我的数据
     */
    SELF,
    /**
     * 有数据权限的部门的数据
     */
    DEPARTMENT,
    /**
     * 被设置为当前用户可见的数据
     */
    VISIBLE,

    /**
     * 成交商机数据
     */
    DEAL;

    public static boolean isAll(String searchType) {
        return StringUtils.equals(ALL.name(), searchType);
    }

    public static boolean isSelf(String searchType) {
        return StringUtils.equals(SELF.name(), searchType);
    }

    public static boolean isDepartment(String searchType) {
        return StringUtils.equals(DEPARTMENT.name(), searchType);
    }

    public static boolean isVisible(String searchType) {
        return StringUtils.equals(VISIBLE.name(), searchType);
    }

    public static boolean isDeal(String searchType) {
        return StringUtils.equals(DEAL.name(), searchType);
    }
}