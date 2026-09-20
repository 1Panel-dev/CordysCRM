package cn.cordys.crm.system.constants;

/**
 * 统计结果为空值(没有任何关联数据或关联数据均无被统计字段的值)时的展示方式。
 */
public enum StatisticEmptyResultMode {

    /**
     * 默认为空, 页面显示 "-"
     */
    EMPTY,
    /**
     * 默认为 0
     */
    ZERO
}
