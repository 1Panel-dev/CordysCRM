package cn.cordys.crm.home.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author jianxing
 */
@Data
public class HomeStatisticSearchResponse {

    @Schema(description = "数值")
    private Long value;

    @Schema(description = "较上期对比率")
    private Double priorPeriodCompareRate;
}
