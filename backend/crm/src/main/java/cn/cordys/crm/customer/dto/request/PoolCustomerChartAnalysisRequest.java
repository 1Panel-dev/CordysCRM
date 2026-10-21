package cn.cordys.crm.customer.dto.request;

import cn.cordys.common.dto.ChartAnalysisRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author jianxing
 */
@Data
public class PoolCustomerChartAnalysisRequest extends ChartAnalysisRequest {
    @Schema(description = "公海ID")
    @NotBlank
    private String poolId;
}
