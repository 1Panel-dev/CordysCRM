package cn.cordys.common.dto.chart;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @Author: jianxing
 * @CreateTime: 2025-10-13  14:54
 */
@Data
public class ChartCategoryAxisConfig {
    @NotBlank
    @Schema(description = "字段ID")
    private String fieldId;
}
