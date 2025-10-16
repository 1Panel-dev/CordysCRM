package cn.cordys.common.dto;

import cn.cordys.common.dto.chart.ChartConfig;
import cn.cordys.common.dto.condition.CombineSearch;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @Author: jianxing
 * @CreateTime: 2025-10-13  14:34
 */
@Data
public class ChartAnalysisRequest {

    @Schema(description = "视图ID")
    private String viewId;

    @Schema(description = "过滤条件")
    @Valid
    private CombineSearch filterCondition;

    @Schema(description = "过滤条件")
    private CombineSearch viewCombineSearch;

    @Schema(description = "搜索条件，支持组合搜索")
    @NotNull
    @Valid
    private ChartConfig chartConfig;
}
