package cn.cordys.crm.customer.dto.request;

import cn.cordys.common.dto.ChartAnalysisRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author jianxing
 */
@Data
public class PoolClueChartAnalysisRequest extends ChartAnalysisRequest {
    @Schema(description = "线索池ID")
    @NotBlank
    private String poolId;
}
