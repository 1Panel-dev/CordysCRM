package io.cordys.crm.home.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


/**
 *
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Data
public class HomeStatisticSearchResponse {

    @Schema(description = "数值")
    private Long value;

    @Schema(description = "较上期对比率")
    private Double priorPeriodCompareRate;
}
