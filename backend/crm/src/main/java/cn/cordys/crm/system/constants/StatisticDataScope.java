package cn.cordys.crm.system.constants;

/**
 * 统计字段的统计数据范围, 决定聚合时过滤目标表单中哪些关联数据。
 */
public enum StatisticDataScope {

    /**
     * 全部关联数据
     */
    ALL,
    /**
     * 仅符合 combineSearch 过滤条件的关联数据
     */
    CONDITION
}
