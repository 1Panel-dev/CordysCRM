package cn.cordys.crm.system.constants;

/**
 * 平均值统计时, 被统计字段为空值的处理方式。
 *
 * <p>仅 AVG 生效。</p>
 */
public enum StatisticEmptyValueMode {

    /**
     * 默认为零: 空值按 0 参与计算, 会拉低平均值
     */
    DEFAULT_ZERO,
    /**
     * 不参与计算: 空值直接从分母中剔除
     */
    SKIP
}
