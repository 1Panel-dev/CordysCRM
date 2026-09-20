package cn.cordys.crm.system.constants;

/**
 * 统计字段的聚合方式。
 *
 * <p>COUNT 只统计关联数据的条数, 不需要指定被统计字段;
 * SUM / AVG 需要指定目标表单中的一个数值类字段。</p>
 */
public enum StatisticType {

    /**
     * 总和
     */
    SUM,
    /**
     * 计数
     */
    COUNT,
    /**
     * 平均值
     */
    AVG
}
