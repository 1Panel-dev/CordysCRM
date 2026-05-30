package cn.cordys.common.constants;

/**
 * 客户,线索,商机等业务数据的搜索类型
 *
 * @author jianxing
 */
public enum BusinessSearchType {
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
    DEPARTMENT
}