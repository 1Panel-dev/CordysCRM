package cn.cordys.common.dto.chart;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @Author: jianxing
 * @CreateTime: 2025-10-13  14:34
 */
@Data
public class ChartConfig {

    @Schema(description = "图表类型")
    private String chatType;

    @Schema(description = "类别轴配置")
    @NotNull
    @Valid
    private ChartCategoryAxisConfig categoryAxis;

    @Schema(description = "子类别轴配置")
    @Valid
    private ChartCategoryAxisConfig subCategoryAxis;

    @Schema(description = "值轴配置")
    @NotNull
    @Valid
    private ChartValueAxisConfig valueAxis;
}
