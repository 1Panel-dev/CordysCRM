package cn.cordys.common.statistic;

import cn.cordys.common.dto.condition.CombineSearch;
import lombok.Data;

/**
 * 统计字段「更新范围」游标分页的请求参数。
 *
 * <p>筛的是宿主表单自身的存量数据, 条件结构就是该表单的高级搜索, 由宿主表单自己的资源 Mapper
 * 执行 —— 与列表页同一套条件片段。</p>
 *
 * <p>绑定名固定为 {@code request}, 与 {@link StatisticAggregateRequest} 一致。</p>
 */
@Data
public class StatisticCursorRequest {

    /**
     * 组织ID, 用于组织隔离。
     */
    private String orgId;

    /**
     * 上一页最后一条的主键; 为空表示第一页。
     */
    private String lastId;

    /**
     * 每页条数。
     */
    private int limit;

    /**
     * 更新范围条件; 无条件时也要给一个非空的空条件, 让片段里的判空短路, 而不是变成 null 比较。
     */
    private CombineSearch scopeCondition;
}
