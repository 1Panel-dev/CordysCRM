package cn.cordys.common.statistic;

import cn.cordys.common.dto.condition.CombineSearch;
import lombok.Data;

/**
 * 统计字段聚合的请求参数。
 *
 * <p>由 {@code StatisticFieldService} 按「一条宿主数据」构造, 交给宿主表单所对应资源 Mapper 的
 * {@code selectStatisticAggregate} 执行 —— 同一条语句里的条件片段与该表单列表页是同一套,
 * 保证「统计时算的行」与「列表页看到的行」口径一致。</p>
 *
 * <p>绑定名固定为 {@code request}: {@code CommonMapper} 里的统计片段直接引用
 * {@code request.statisticBusinessKey} 这类属性, 改名要同步改片段。</p>
 */
@Data
public class StatisticAggregateRequest {

    /**
     * 组织ID, 用于组织隔离。
     */
    private String orgId;

    /**
     * 宿主表单的数据ID, 即被统计数据的关联目标。
     */
    private String dataId;

    /**
     * 关联字段ID: 目标表单上指向宿主表单的数据源单选字段。
     */
    private String relatedFieldId;

    /**
     * 关联字段在目标表单主表上的列名; 为空表示关系只存在字段值表里。
     */
    private String relatedBusinessKey;

    /**
     * 被统计字段ID; COUNT 时为空。
     */
    private String statisticFieldId;

    /**
     * 被统计字段在目标表单主表上的列名; 为空表示取值要走字段值表。
     */
    private String statisticBusinessKey;

    /**
     * 统计类型: SUM / COUNT / AVG。
     */
    private String statisticType;

    /**
     * 平均值统计时空值是否跳过: true 跳过, false 当 0 计入分母。
     */
    private boolean avgSkipEmpty;

    /**
     * 统计范围条件, 已按目标表单解析并换好右值。
     */
    private CombineSearch scopeCondition;
}
