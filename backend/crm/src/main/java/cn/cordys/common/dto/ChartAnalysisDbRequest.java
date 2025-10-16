package cn.cordys.common.dto;

import cn.cordys.common.dto.chart.ChartCategoryAxisDbParam;
import cn.cordys.common.dto.chart.ChartValueAxisDbParam;
import cn.cordys.common.dto.condition.CombineSearch;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @Author: jianxing
 * @CreateTime: 2025-10-13  14:34
 */
@Data
public class ChartAnalysisDbRequest extends ChartAnalysisRequest {

    @Schema(description = "过滤条件")
    private CombineSearch viewFilterCondition;

    /**
     * x轴查询参数
     */
    private ChartCategoryAxisDbParam categoryAxisParam;

    /**
     * x轴子类别查询参数
     */
    private ChartCategoryAxisDbParam subCategoryAxisParam;

    /**
     * y轴查询参数
     */
    private ChartValueAxisDbParam valueAxisParam;
}
