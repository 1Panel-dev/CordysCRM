package cn.cordys.crm.system.constants;

/**
 * 统计字段的更新范围, 决定保存表单配置时是否需要重算存量数据的统计值。
 */
public enum StatisticUpdateScope {

    /**
     * 现有数据不计算: 保存配置时不刷新任何存量数据
     */
    NONE,
    /**
     * 全部计算: 刷新当前表单下所有数据的该统计字段值
     */
    ALL,
    /**
     * 符合数据范围计算: 仅刷新当前表单下符合 updateScopeCondition 的数据
     */
    CONDITION
}
